/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chrism.api

import cats.effect.{ExitCode, IO, IOApp}
import com.chrism.api.config.SystemPropertiesHandle
import com.chrism.api.log.Logging
import com.chrism.api.model.ContributionsResponse
import com.chrism.api.standard.UsSubdivision
import com.chrism.api.stats.PartyDonation
import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.{Dataset, SparkSession}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.{HttpApp, HttpRoutes}

object CampaignContributionService
    extends IOApp
    with CampaignContributionJson4sFormatsLike
    with SystemPropertiesHandle
    with Logging {

  import cats.implicits._
  import com.chrism.api.google.CivicInformationClient.ApiKeyPropKey
  import com.chrism.api.stats.StatsLoader.{preload, readPartyDonations, readPartyDonationsWithPredicate}
  import org.http4s.dsl.io._
  import org.http4s.implicits._

  private[api] val BasePathPropKey: String = "api.basePath"
  private[api] val PortPropKey: String = "api.port"
  private[api] val StatesPropKey: String = "api.states"

  private val DefaultPort: Int = 8080

  // Just a few random states
  private val DefaultStates: Seq[UsSubdivision] = Seq(
    UsSubdivision.GA,
    UsSubdivision.NM,
    UsSubdivision.NY,
    UsSubdivision.MA,
  )

  private val All: String = implicitly[sourcecode.Name].value
  private val Democrat: String = "Democratic Party"
  private val Republican: String = "Republican Party"
  private val DemocratVariations: Seq[String] = Seq(Democrat, "D", "Democrat", "Democrats", "Democratic")
  private val RepublicanVariations: Seq[String] = Seq(Republican, "R", "Republican", "Republicans")

  @transient
  private lazy val acceptablePartyNames: String =
    s"""To obtain stats for all parties: $All
       |To obtain stats for $Democrat: ${DemocratVariations.mkString("|")}
       |To obtain stats for $Republican: ${RepublicanVariations.mkString("|")}
       |Note that the values are case-insensitive""".stripMargin

  def runService(
    port: Int,
    googleApiKey: String,
    basePath: String,
    states: Option[Seq[UsSubdivision]] = None
  )(
    implicit
    spark: SparkSession
  ): IO[ExitCode] = {
    logger.info(s"Trying binding to the port $port")
    BlazeServerBuilder[IO]
      .bindHttp(port, "localhost")
      .withHttpApp(buildApp(googleApiKey, basePath, states = states))
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }

  private[api] def buildApp(
    googleApiKey: String,
    basePath: String,
    states: Option[Seq[UsSubdivision]] = None
  )(
    implicit
    spark: SparkSession
  ): HttpApp[IO] =
    Logger.httpApp(true, true) {
      logger.info(s"Preloading the aggregates to $basePath")
      preload(googleApiKey, basePath, states = states.orElse(Some(DefaultStates))).unsafeRunSync()
      logger.info(s"Successfully preloaded the data to $basePath")

      HttpRoutes
        .of[IO] {
          case GET -> Root / "contributions" / party =>
            party match {
              case r if StringUtils.equalsAnyIgnoreCase(r, RepublicanVariations: _*) =>
                Ok(collectPartyDonationsWithPredicate(basePath)(_.party.contains(Republican)))
              case d if StringUtils.equalsAnyIgnoreCase(d, DemocratVariations: _*) =>
                Ok(collectPartyDonationsWithPredicate(basePath)(_.party.contains(Democrat)))
              case a if a.equalsIgnoreCase(All) => Ok(collectPartyDonations(basePath))
              case other =>
                logger.warn(s"Invalid party name: $other")
                BadRequest(s"""$other is not a valid party name!
                              |$acceptablePartyNames""".stripMargin)
            }
        }
        .orNotFound
    }

  private def collectPartyDonations(basePath: String)(implicit spark: SparkSession): IO[ContributionsResponse] =
    toContributionsResponse(readPartyDonations(basePath))

  private def collectPartyDonationsWithPredicate(
    basePath: String
  )(
    p: PartyDonation => Boolean
  )(
    implicit
    spark: SparkSession
  ): IO[ContributionsResponse] =
    toContributionsResponse(readPartyDonationsWithPredicate(basePath)(p))

  private def toContributionsResponse(
    ds: IO[Dataset[PartyDonation]]
  )(
    implicit
    spark: SparkSession
  ): IO[ContributionsResponse] =
    ds.map(_.collect()).map(ContributionsResponse(_))

  override def run(args: List[String]): IO[ExitCode] =
    withSystemProperties(ApiKeyPropKey, BasePathPropKey) { props =>
      val apiKey = props(ApiKeyPropKey)
      val basePath = props(BasePathPropKey)
      val port = withSystemPropertiesIfSetOrDefault(PortPropKey)(_(PortPropKey).toInt)(DefaultPort)
      val states = withSystemPropertiesIfSetOrNone(StatesPropKey)(p => parseStates(p(StatesPropKey)))

      implicit val spark: SparkSession = SparkSession.builder().master("local[*]").getOrCreate()
      runService(port, apiKey, basePath, states = states)
    }

  private[api] def parseStates(rawStates: String): Seq[UsSubdivision] = {
    val states = rawStates.replaceAll(""""""", "").split(',')
    if (states.exists(_.equalsIgnoreCase(All))) UsSubdivision.values.toSeq
    else states.map(UsSubdivision.parse)
  }
}
