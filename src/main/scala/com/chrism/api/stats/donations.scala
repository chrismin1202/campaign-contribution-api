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
import com.chrism.api.maplight.model.contributions.AggregateTotal
import org.http4s.EntityDecoder
import org.http4s.json4s.native.jsonExtract
import org.json4s.Formats

/** The data model used for aggregating contribution for each candidate.
  *
  * @param name the name of the candidate
  * @param totalContributions the total number of contributions received by the candidate
  * @param totalDonation the total amount of donation received by the candidate
  * @param party the political party of the candidate
  */
final case class CandidateDonation(
  name: String,
  totalContributions: Long,
  totalDonation: BigDecimal,
  party: Option[String]) {

  def +(that: CandidateDonation): CandidateDonation = {
    require(name == that.name, s"The names do not match: $name vs. ${that.name}")
    require(party == that.party, s"The parties do not match: $party vs. ${that.party}")
    copy(
      totalContributions = totalContributions + that.totalContributions,
      totalDonation = totalDonation + that.totalDonation
    )
  }
}

object CandidateDonation {

  def ofOrNone(representative: Representative, donations: Seq[AggregateTotal]): Option[CandidateDonation] =
    donations
      .filter(_.nonEmpty)
      .reduceOption(_ + _)
      .map(agg => CandidateDonation(representative.name, agg.contributions, agg.totalAmount, representative.party))

  implicit def decoder(implicit formats: Formats): EntityDecoder[IO, CandidateDonation] =
    jsonExtract[IO, CandidateDonation]
}

/** The data model for aggregating contribution for each political party.
  *
  * @param party the political party of the candidate
  *              If [[None]], either the candidates are independent or the candidates' party is not identified
  * @param totalContributions the total number of contributions for the party
  * @param minContributions the minimum number of contributions by a candidate who belongs to the party
  * @param maxContributions the maximum number of contributions by a candidate who belongs to the party
  * @param avgContributions the average number of contributions for the party
  * @param totalDonation the total amount of donation for the party
  * @param minDonation the minimum amount of donation made to a candidate who belongs to the party
  * @param maxDonation the maximum amount of donation made to a candidate who belongs to the party
  * @param avgDonation the average amount of donation made to a candidate
  * @param numCandidates the number of candidates in the party
  */
final case class PartyDonation(
  party: Option[String],
  totalContributions: Long,
  minContributions: Long,
  maxContributions: Long,
  avgContributions: BigDecimal,
  totalDonation: BigDecimal,
  minDonation: BigDecimal,
  maxDonation: BigDecimal,
  avgDonation: BigDecimal,
  numCandidates: Long = 1L) {

  def +(that: PartyDonation): PartyDonation = {
    require(party == that.party, s"The parties do not match: $party vs. ${that.party}")
    val tc = totalContributions + that.totalContributions
    val td = totalDonation + that.totalDonation
    copy(
      totalContributions = tc,
      minContributions = minContributions min that.minContributions,
      maxContributions = maxContributions max that.maxContributions,
      avgContributions = BigDecimal(tc) / numCandidates,
      totalDonation = td,
      minDonation = minDonation min that.minDonation,
      maxDonation = maxDonation max that.maxDonation,
      avgDonation = td / tc,
      numCandidates = numCandidates + that.numCandidates
    )
  }
}

object PartyDonation {

  def apply(party: Option[String], contributions: Long, totalDonation: BigDecimal): PartyDonation =
    PartyDonation(
      party,
      contributions,
      contributions,
      contributions,
      contributions,
      totalDonation,
      totalDonation,
      totalDonation,
      totalDonation / contributions)

  def apply(candidateDonation: CandidateDonation): PartyDonation =
    PartyDonation(candidateDonation.party, candidateDonation.totalContributions, candidateDonation.totalDonation)

  implicit def decoder(implicit formats: Formats): EntityDecoder[IO, PartyDonation] = jsonExtract[IO, PartyDonation]
}
