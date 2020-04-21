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
package com.chrism.api.stats

import cats.effect.IO
import com.chrism.api.{CampaignContributionJson4sFormatsLike, NoResultsException}
import com.chrism.api.google.CivicInformationClient
import com.chrism.api.log.Logging
import com.chrism.api.maplight.MapLightClient
import com.chrism.api.maplight.model.contributions.{ContributionSearchResponse, RequestParams}
import com.chrism.api.maplight.model.search.name.{CandidateName, CandidateSearchResponse}
import com.chrism.api.standard.{SubdivisionKind, UsSubdivision}
import org.apache.commons.lang3.StringUtils
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{DataFrame, Dataset, SaveMode, SparkSession}

import scala.collection.mutable
import scala.util.matching.Regex

// TODO: ScalaDoc

object StatsLoader extends CampaignContributionJson4sFormatsLike with Logging {

  private val CandidateLabelRegex: Regex = "\\(([A-Z]), ([A-Za-z]+)(-([0-9]+))?\\)".r
  private val CandidateDonationPath: String = "candidate_donation"
  private val PartyDonationPath: String = "party_donation"

  def preload(googleApiKey: String, basePath: String)(implicit spark: SparkSession): IO[Unit] = {
    import spark.implicits._

    val subdivisions = UsSubdivision.Values
      .filter(_.kind == SubdivisionKind.State)
      .filter(_ == UsSubdivision.MA)
      .toSeq
      .map(EncodableUsSubdivision(_))
    val client = spark.sparkContext.broadcast(CivicInformationClient(googleApiKey))
    writeCandidateDonationDs(
      basePath,
      spark
        .createDataset(subdivisions)
        .flatMap(searchOcdIds(_, client))
        .distinct()
        // Google is rate-limiting
        .repartition(1)
        .flatMap(findOfficials(_, client))
        .flatMap(donationsOf)
    ).flatMap(_ => readCandidateDonations(basePath))
      .map(_.map(PartyDonation(_))
        .groupByKey(_.party)
        .reduceGroups(_ + _)
        .map(_._2))
      .flatMap(writePartyDonationDs(basePath, _))
  }

  def readCandidateDonations(basePath: String)(implicit spark: SparkSession): IO[Dataset[CandidateDonation]] = {
    import spark.implicits._

    logger.info(s"Reading the contribution aggregate data for the candidates from $basePath")
    readDataFrame(candidateDonationPathOf(basePath)).map(_.as[CandidateDonation])
  }

  def readCandidateDonationsWithPredicate(
    basePath: String
  )(
    p: CandidateDonation => Boolean
  )(
    implicit
    spark: SparkSession
  ): IO[Dataset[CandidateDonation]] =
    readCandidateDonations(basePath).map(_.filter(p))

  def readPartyDonations(basePath: String)(implicit spark: SparkSession): IO[Dataset[PartyDonation]] = {
    import spark.implicits._

    logger.info(s"Reading the contribution aggregate data for the parties from $basePath")
    readDataFrame(partyDonationPathOf(basePath)).map(_.as[PartyDonation])
  }

  def readPartyDonationsWithPredicate(
    basePath: String
  )(
    p: PartyDonation => Boolean
  )(
    implicit
    spark: SparkSession
  ): IO[Dataset[PartyDonation]] =
    readPartyDonations(basePath).map(_.filter(p))

  def writeCandidateDonationDs(
    basePath: String,
    donationDs: Dataset[CandidateDonation]
  )(
    implicit
    spark: SparkSession
  ): IO[Unit] = {
    logger.info(s"Writing the contribution aggregate data for the candidates to $basePath")
    writeDataset(candidateDonationPathOf(basePath), donationDs)
  }

  def writePartyDonationDs(
    basePath: String,
    donationDs: Dataset[PartyDonation]
  )(
    implicit
    spark: SparkSession
  ): IO[Unit] = {
    logger.info(s"Writing the contribution aggregate data for the parties to $basePath")
    writeDataset(partyDonationPathOf(basePath), donationDs)
  }

  /** Searches OCD ids for the given political division.
    *
    * @param division a political subdivision
    * @param clientBroadcast the [[CivicInformationClient]] instance to use
    * @return the OCD ids of the division
    */
  private def searchOcdIds(
    division: EncodableUsSubdivision,
    clientBroadcast: Broadcast[CivicInformationClient]
  ): Iterable[String] =
    UsSubdivision.UsOcdId +:
      division.ocdId +:
      clientBroadcast.value.getCongressionalDistrictsByState(division.code).map(_.results.map(_.ocdId)).unsafeRunSync()

  /** Finds the officials (or candidates) that belong to the political division associated with the given OCD id.
    *
    * @param ocdId the OCD id of the division
    * @param clientBroadcast the [[CivicInformationClient]] instance to use
    * @return
    */
  private def findOfficials(
    ocdId: String,
    clientBroadcast: Broadcast[CivicInformationClient]
  ): Iterable[Representative] = {
    import Representative.implicits._
    logger.info(s"Finding the officials associated with the OCD id $ocdId")

    clientBroadcast.value
      .getRepresentativesByOcdId(ocdId)
      .map(response =>
        if (response.isEmpty) Seq.empty
        else {
          val reps = mutable.ListBuffer.empty[Representative]
          response.presidentOrNone.foreach(reps += _)
          response.vicePresidentOrNone.foreach(reps += _)
          response.usRepresentativesOrNone.foreach(reps ++= _)
          response.usSenatorsOrNone.foreach(reps ++= _)
          reps
        })
      .unsafeRunSync()
  }

  /** Searches the donations for the given candidate.
    *
    * @param representative the candidate to search
    * @return the donation as an instance of [[CandidateDonation]] or [[None]]
    */
  private def donationsOf(representative: Representative): Option[CandidateDonation] =
    MapLightClient
      .getContributionByCandidateName(representative.name)
      .attempt
      .flatMap {
        case Left(err) =>
          logger.error(err)(
            s"An error caught while looking up contributions by name for the representative $representative")
          searchCandidateContributions(representative)
        case Right(response) if response.isEmpty =>
          logger.info(s"No results found while looking up contributions by name for the representative $representative")
          searchCandidateContributions(representative)
        case Right(response) => IO.pure(Right(response))
      }
      .map {
        case Left(err) =>
          logger.error(err)(
            s"An error caught while looking up contributions by name for the representative $representative")
          None
        case Right(response) =>
          CandidateDonation.ofOrNone(representative, response.data.aggregateTotals)
//        .map(Right(_))
//        .getOrElse(Left(new NoResultsException(representative.name)))
      }
      .unsafeRunSync()
//    // Try searching contributions by candidate name first
//    val response = MapLightClient.getContributionByCandidateName(representative.name)
//
////    MapLightClient.getContributionByCandidateName(representative.name)
//
//    // If the lookup fails, search candidate name to get MapLight id
//    val donations =
//      if (response.isEmpty)
//        matchCandidate(representative, MapLightClient.searchCandidate(representative.name))
//          .map(c => MapLightClient.getContribution(RequestParams(c)).data.aggregateTotals)
//          .getOrElse(Seq.empty)
//      else response.data.aggregateTotals
//
//    CandidateDonation.ofOrNone(representative, donations)

  private def searchCandidateContributions(
    representative: Representative
  ): IO[Either[Throwable, ContributionSearchResponse]] =
    MapLightClient
      .searchCandidate(representative.name)
      .attempt
      .flatMap {
        case Left(err) =>
          logger.error(err)(s"An error caught while searching the representative $representative")
          //Either[Throwable, ContributionSearchResponse]
          IO.pure(Left(err))
        case Right(nameSearch) =>
          matchCandidate(representative, nameSearch) match {
            case Left(err) =>
              logger.error(err)(s"An error caught while matching the representative $representative")
              IO.pure(Left(err))
            case Right(name) =>
              MapLightClient
                .getContribution(RequestParams(name))
                .attempt
                .map {
                  case Left(err) => Left(err)
                  case Right(contributionSearch) =>
                    if (contributionSearch.isEmpty) Left(new NoResultsException(representative.name))
                    else Right(contributionSearch)
                }
          }
//            .map(RequestParams(_))

//            .flatMap(p => MapLightClient.getContribution(p).attempt/*.map{
//              case Left(err) => Left(err)
//              case Right(contributionSearch) => Right(contributionSearch)
//            }*/)
//
//            .map(MapLightClient.getContribution)
//        .map(_.attempt.map{
//          case Left(err) => Left(err)
//          case Right(contributionSearch) => Right(contributionSearch)
//        })
      }

  /** Tries to match the given representative in the given [[CandidateSearchResponse]] object.
    *
    * Note that the match logic is very naive and requires improvements.
    * Currently, it
    *
    * @param representative the candidate to match
    * @param response the candidate search response from MapLight API
    * @return the matched [[CandidateName]] instance or [[None]]
    */
  private def matchCandidate(
    representative: Representative,
    response: CandidateSearchResponse
  ): Either[Throwable, CandidateName] =
    if (response.isEmpty) Left(new NoResultsException(representative.name))
    else {
      val candidates = response.data.candidateNames.flatMap(matchCandidateLabel(representative, _))
      // If there is more than 1 matched candidate, return None
      candidates.size match {
        case 0 => Left(new NoResultsException(representative.name))
        case 1 => Right(candidates.head)
        case _ => Left(new CandidateMatchException(representative, response))
      }
    }

  /** Checks whether the given [[Representative]] instance matches the given [[CandidateName]] instance.
    *
    * @param representative the [[Representative]] instance to match
    * @param candidate the [[CandidateName]] instance to match with
    * @return the [[CandidateName]] instance if matched else [[None]]
    */
  private def matchCandidateLabel(representative: Representative, candidate: CandidateName): Option[CandidateName] =
    CandidateLabelRegex
      .findFirstMatchIn(candidate.candidateLabel)
      .flatMap(m =>
        if (representative.party.exists(StringUtils.startsWithIgnoreCase(_, m.group(1)))) {
          // Check district number
          // If senator, president, or vice president,
          //   m.group(4) == null && representative.division.district == None
          if (Option(m.group(4)).map(_.toInt) == representative.division.district) {
            m.group(2) match {
              case "President" =>
                if (candidate.candidateName.equalsIgnoreCase(representative.name)) Some(candidate)
                else None
              case state =>
                if (representative.division.subdivision.exists(_.name.equalsIgnoreCase(state))) Some(candidate)
                else None
            }
          } else None
        } else None)

  private def candidateDonationPathOf(basePath: String): String = formatDonationPath(basePath, CandidateDonationPath)

  private def partyDonationPathOf(basePath: String): String = formatDonationPath(basePath, PartyDonationPath)

  private def formatDonationPath(basePath: String, subPath: String): String =
    if (basePath.endsWith("/")) basePath + subPath else s"$basePath/$subPath"

  private def readDataFrame(path: String)(implicit spark: SparkSession): IO[DataFrame] =
    IO {
      spark.read.parquet(path)
    }

  private def writeDataset(path: String, ds: Dataset[_])(implicit spark: SparkSession): IO[Unit] =
    IO {
      ds.repartition(1)
        .write
        .mode(SaveMode.Overwrite)
        .parquet(path)
    }

//  override def runSpark(args: Array[String])(implicit spark: SparkSession): Unit = {
//    val groupedArgs = args.grouped(2)
//    var basePath: String = null
//    var key: String = null
//    while (groupedArgs.hasNext) {
//      val arg = groupedArgs.next()
//      arg(0) match {
//        case "--base-path"      => basePath = arg(1)
//        case "--google-api-key" => key = arg(1)
//        case other              => throw new IllegalArgumentException(s"$other is invalid argument!")
//      }
//    }
//    require(StringUtils.isNotBlank(basePath), "The base path must be specified!")
//    require(StringUtils.isNotBlank(key), "The Google API key must be specified!")
//    preload(key, basePath)
//  }

  private def formatErrorMessage(representative: Representative, response: CandidateSearchResponse): String =
    s"Failed to match $representative with ${response.data.candidateNames}"

  final class CandidateMatchException(representative: Representative, response: CandidateSearchResponse)
      extends Exception(formatErrorMessage(representative, response))
}
