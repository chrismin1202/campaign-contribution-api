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
package com.chrism.api.google.model.divisions

import cats.effect.IO
import org.http4s.EntityDecoder
import org.http4s.json4s.native.jsonExtract
import org.json4s.Formats

/** Represents the response object for [[com.chrism.api.google.CivicInformationEndpoint.Divisions]].
  *
  * @param kind the kind of resource
  *             The value is the fixed string, e.g., "civicinfo#divisionSearchResponse"
  * @param results the divisions
  */
final case class DivisionsResponse(kind: String, results: Seq[Division])

object DivisionsResponse {

  implicit def decoder(implicit formats: Formats): EntityDecoder[IO, DivisionsResponse] =
    jsonExtract[IO, DivisionsResponse]
}
