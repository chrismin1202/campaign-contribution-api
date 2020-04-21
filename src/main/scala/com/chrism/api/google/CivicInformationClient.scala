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
package com.chrism.api.google

import cats.effect.IO
import com.chrism.api.ApiClientLike
import com.chrism.api.google.model.divisions.{Division, DivisionsResponse}
import com.chrism.api.google.model.representatives.RepresentativesResponse
import com.chrism.api.standard.UsSubdivision
import org.apache.commons.lang3.StringUtils
import org.http4s.Uri.RegName
import org.http4s.{Header, Headers, Method, Query, Request, Uri}
import org.json4s.Formats

final case class CivicInformationClient(apiKey: String) extends ApiClientLike {

  import CivicInformationClient.isCongressionalDistrict
  import CivicInformationEndpoint.{Divisions, Representatives}

  require(StringUtils.isNotBlank(apiKey), "The API key cannot be blank!")

  @transient
  private[this] lazy val baseUri: Uri = Uri(
    scheme = Some(Uri.Scheme.https),
    authority = Some(Uri.Authority(host = RegName("www.googleapis.com"))),
    path = "/civicinfo/v2",
    query = Query("key" -> Some(apiKey))
  )

  def getCongressionalDistricts(state: UsSubdivision)(implicit formats: Formats): IO[DivisionsResponse] =
    call { client =>
      val request = Request[IO](
        Method.GET,
        uri = Divisions.uri(baseUri).withQueryParam("query", state.divisionId + "/"),
        headers = Headers.of(Header("Accept", "application/json"))
      )
      client
        .expect[DivisionsResponse](request)
        .map { response =>
          val districts = response.results.filter(isCongressionalDistrict)
          response.copy(results =
            if (districts.nonEmpty) districts
            else state.congressionalDistrictOcdIds.map(_.map(Division(_))).getOrElse(Seq.empty))
        }
    }

  def getCongressionalDistrictsByState(stateNameOrCode: String)(implicit formats: Formats): IO[DivisionsResponse] =
    getCongressionalDistricts(UsSubdivision.parse(stateNameOrCode))

  def getRepresentativesByOcdId(ocdId: String)(implicit formats: Formats): IO[RepresentativesResponse] =
    call { client =>
      val request = Request[IO](
        Method.GET,
        uri = Representatives.uri(baseUri).addSegment(ocdId),
        headers = Headers.of(Header("Accept", "application/json"))
      )
      client.expect[RepresentativesResponse](request)
    }
}

object CivicInformationClient {

  import com.chrism.api.ocd.OcdDivision.isCongressionalDistrictOcd

  private[api] val ApiKeyPropKey: String = "api.google.apiKey"

  private def isCongressionalDistrict(division: Division): Boolean =
    isCongressionalDistrictOcd(division.ocdId) || division.aliases.exists(_.exists(isCongressionalDistrictOcd))
}
