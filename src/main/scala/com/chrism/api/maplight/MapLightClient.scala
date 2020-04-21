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
package com.chrism.api.maplight

import cats.effect.IO
import com.chrism.api.maplight.model.contributions.{ContributionSearchResponse, RequestParams}
import com.chrism.api.maplight.model.search.name.CandidateSearchResponse
import com.chrism.api.{ApiClientLike, ApiException}
import org.http4s.Uri.RegName
import org.http4s.{Header, Headers, Method, Request, Response, Uri}
import org.json4s.Formats

object MapLightClient extends ApiClientLike {

  import MapLightEndpoint.{CandidateNames, Contributions}

  @transient
  private[this] lazy val baseUri: Uri = Uri(
    scheme = Some(Uri.Scheme.https),
    authority = Some(Uri.Authority(host = RegName("api.maplight.org"))),
    path = "/maplight-api/fec"
  )

  def searchCandidate(searchText: String)(implicit formats: Formats): IO[CandidateSearchResponse] =
    call { client =>
      val request = Request[IO](
        Method.GET,
        uri = CandidateNames.uri(baseUri).addSegment(searchText),
        headers = Headers.of(Header("Accept", "application/json"))
      )
      client.expectOr[CandidateSearchResponse](request)(handleError)
    }

  def getContribution(params: RequestParams)(implicit formats: Formats): IO[ContributionSearchResponse] =
    call { client =>
      val request = Request[IO](
        Method.GET,
        uri = Contributions.uri(baseUri).withQueryParams(params.urlParams),
        headers = Headers.of(Header("Accept", "application/json"))
      )

      client.expectOr[ContributionSearchResponse](request)(handleError)
    }

  def getContributionByCandidateName(name: String)(implicit formats: Formats): IO[ContributionSearchResponse] =
    getContribution(RequestParams(candidateName = Some(Seq(name))))

  def getContributionByCandidateId(
    mapLightId: Long
  )(
    implicit
    formats: Formats
  ): IO[ContributionSearchResponse] =
    getContribution(RequestParams(candidateMapLightId = Some(Seq(mapLightId))))

  private def handleError(response: Response[IO]): IO[Throwable] = IO.pure(new ApiException(response))
}
