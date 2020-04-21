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
import org.http4s.EntityDecoder
import org.http4s.json4s.native.jsonExtract
import org.json4s.Formats

/** Represents a political geographic division for [[com.chrism.api.google.CivicInformationEndpoint.Representatives]].
  *
  * @param name the name of the division
  * @param officeIndices a list of indices in the offices array, one for each office elected from this division
  * @param alsoKnownAs any other valid OCD IDs that refer to the same division
  *                    Because OCD IDs are meant to be human-readable and at least somewhat predictable,
  *                    there are occasionally several identifiers for a single division.
  *                    These identifiers are defined to be equivalent to one another,
  *                    and one is always indicated as the primary identifier.
  *                    The primary identifier will be returned in ocd_id above,
  *                    and any other equivalent valid identifiers will be returned in this list.
  *                    For example, if this division's OCD ID is ocd-division/country:us/district:dc,
  *                    this will contain ocd-division/country:us/state:dc
  */
final case class Division(name: String, officeIndices: Option[Seq[Int]] = None, alsoKnownAs: Option[Seq[String]] = None)

object Division {

  implicit def decoder(implicit formats: Formats): EntityDecoder[IO, Division] = jsonExtract[IO, Division]
}
