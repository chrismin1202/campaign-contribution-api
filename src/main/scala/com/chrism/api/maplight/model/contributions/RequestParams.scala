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
package com.chrism.api.maplight.model.contributions

import com.chrism.api.maplight.model.search.name.CandidateName

/** Represents the request parameters for [[com.chrism.api.maplight.MapLightEndpoint.Contributions]].
  *
  * Multiple values are separated with pipes (|), and results from all are included (OR).
  * All parameters are optional, but there must be at least one parameter.
  *
  * @param candidateName the candidate's exact full name, as standardized in MapLight's data
  * @param candidateFecId the candidate's FEC ID
  * @param candidateMapLightId the MapLight's "person_id" for the candidate
  * @param electionCycle the two-year election cycle in which the contribution was made
  *                      Cycles since the 2008 cycle are available.
  * @param donorOrganization the donor's exact organization name, as standardized in MapLight's data
  * @param donorText a text string to search
  *                  All terms must appear in either the donor's name or their employer.
  */
final case class RequestParams(
  candidateName: Option[Seq[String]] = None,
  candidateFecId: Option[Seq[String]] = None,
  candidateMapLightId: Option[Seq[Long]] = None,
  electionCycle: Option[Seq[Int]] = None,
  donorOrganization: Option[Seq[String]] = None,
  donorText: Option[Seq[String]] = None) {

  require(
    electionCycle.isEmpty || electionCycle.forall(_.forall(y => y >= 2008 && y < 10000)),
    "Election cycle must be 4-digit!")

  @transient
  lazy val urlParams: Map[String, String] =
    candidateName.pipeDelimit("candidate_name") ++
      candidateFecId.pipeDelimit("candidate_fecid") ++
      candidateMapLightId.pipeDelimit("candidate_mlid") ++
      electionCycle.pipeDelimit("election_cycle") ++
      donorOrganization.pipeDelimit("donor_organization") ++
      donorText.pipeDelimit("donor_text")

  private[this] implicit final class SeqOptOps[A](values: Option[Seq[A]]) {

    def pipeDelimit(name: String): Map[String, String] =
      values.filter(_.nonEmpty).map(_.mkString("|")).map(name -> _).toMap
  }
}

object RequestParams {

  def apply(candidate: CandidateName): RequestParams =
    RequestParams(candidateMapLightId = Some(Seq(candidate.candidateMapLightId)))
}
