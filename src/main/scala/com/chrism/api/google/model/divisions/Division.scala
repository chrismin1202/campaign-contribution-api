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

/** Represents a political geographic division for [[com.chrism.api.google.CivicInformationEndpoint.Divisions]].
  *
  * @param ocdId the unique Open Civic Data identifier for this division
  * @param name the name of the division
  * @param aliases other Open Civic Data identifiers that refer to the same division,
  *                e.g., those that refer to other political divisions
  *                whose boundaries are defined to be coterminous with this one.
  *                For example, ocd-division/country:us/state:wy will include
  *                an alias of ocd-division/country:us/state:wy/cd:1,
  *                since Wyoming has only one Congressional district
  */
final case class Division(ocdId: String, name: Option[String] = None, aliases: Option[Seq[String]] = None)

object Division {

  implicit def decoder(implicit formats: Formats): EntityDecoder[IO, Division] = jsonExtract[IO, Division]
}
