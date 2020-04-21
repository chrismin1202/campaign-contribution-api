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
package com.chrism.api.google.model.representatives

import cats.effect.IO
import com.chrism.api.CanBeEmpty
import com.chrism.api.google.model.CivicInformationApiError
import org.http4s.EntityDecoder
import org.http4s.json4s.native.jsonExtract
import org.json4s.Formats

/** Represents the response object for [[com.chrism.api.google.CivicInformationEndpoint.Representatives]].
  *
  * @param divisions the political geographic divisions
  * @param offices the elected offices referenced by the divisions
  * @param officials the elected officials referenced by the divisions
  * @param error the response error
  *              If {{{ error.isDefined == true }}},
  *              {{{ division }}}, {{{ offices }}}, and {{{ officials }}} are [[None]]
  */
final case class RepresentativesResponse(
  divisions: Option[Map[String, Division]] = None,
  offices: Option[Seq[Office]] = None,
  officials: Option[Seq[Official]] = None,
  error: Option[CivicInformationApiError] = None)
    extends CanBeEmpty {

  override def isEmpty: Boolean = officials.isEmpty || officials.exists(_.isEmpty)
}

object RepresentativesResponse {

  implicit def decoder(implicit formats: Formats): EntityDecoder[IO, RepresentativesResponse] =
    jsonExtract[IO, RepresentativesResponse]
}
