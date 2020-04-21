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

/** Represents an elected office.
  *
  * @param divisionId the OCD id of the division with which this office is associated
  * @param name the human-readable name of the office
  * @param levels the levels of government of which this office is part
  *               There may be more than one in cases where
  *               a jurisdiction effectively acts at two different levels of government;
  *               for example, the mayor of the District of Columbia acts at "locality" level,
  *               but also effectively at both administrativeArea2 and administrativeArea1
  * @param officialIndices a list of indices in the officials array of people who presently hold this office
  * @param roles the roles which this office fulfills
  *              Roles are not meant to be exhaustive,
  *              or to exactly specify the entire set of responsibilities of a given office,
  *              but are meant to be rough categories that are useful for general selection from
  *              or sorting of a list of offices
  * @param sources a list of sources for this office
  *                If multiple sources are listed, the data has been aggregated from those sources
  */
final case class Office(
  divisionId: String,
  name: String,
  levels: Seq[String],
  officialIndices: Seq[Int],
  roles: Option[Seq[String]] = None,
  sources: Option[Seq[Source]] = None) {

  import Levels._
  import Offices._
  import Roles._

  def isPresident: Boolean =
    name == president ||
      (levels.contains(country) && roles.exists(r => r.contains(headOfState) && r.contains(headOfGovernment)))

  def isVicePresident: Boolean =
    name == vicePresident || (levels.contains(country) && roles.exists(_.contains(deputyHeadOfGovernment)))

  def isUsRepresentative: Boolean =
    name == usRepresentative || (levels.contains(country) && roles.exists(_.contains(legislatorLowerBody)))

  def isUsSenator: Boolean =
    name == usSenator || (levels.contains(country) && roles.exists(_.contains(legislatorUpperBody)))
}

object Office {

  implicit def decoder(implicit formats: Formats): EntityDecoder[IO, Office] = jsonExtract[IO, Office]
}
