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
package com.chrism.api.ocd

import scala.util.matching.Regex

final case class OcdDivision(
  country: String = OcdDivision.us,
  subdivision: Option[OcdSubdivision] = None,
  district: Option[Int] = None)

object OcdDivision {

  val us: String = implicitly[sourcecode.Name].value
  val OcdDivisionPrefix: String = "ocd-division"
  val UsDivisionId: String = "country:us"
  val UsOcdId: String = s"$OcdDivisionPrefix/$UsDivisionId"

  private val StateOcdRegex: Regex = "(state|territory|district):([a-z]{2})$".r
  private val CongressionalDistrictOcdRegex: Regex = "cd:([0-9]+)$".r

  private val OcdDivisionRegex: Regex =
    new Regex(s"$UsDivisionId(/${StateOcdRegex.regex}(/${CongressionalDistrictOcdRegex.regex})?)?")

  def parseIdOrNone(ocdId: String): Option[OcdDivision] =
    Option(ocdId)
      .flatMap(OcdDivisionRegex.findFirstMatchIn)
      .map(m => OcdDivision(subdivision = parseSubdivisionOrNone(m), district = Option(m.group(5)).map(_.toInt)))

  def parseId(ocdId: String): OcdDivision =
    parseIdOrNone(ocdId).getOrElse(throw new IllegalArgumentException(s"$ocdId is malformed!"))

  def isCongressionalDistrictOcd(ocdId: String): Boolean =
    CongressionalDistrictOcdRegex.findFirstMatchIn(ocdId).isDefined

  private[this] def parseSubdivisionOrNone(m: Regex.Match): Option[OcdSubdivision] =
    for {
      kind <- Option(m.group(2))
      name <- Option(m.group(3))
    } yield OcdSubdivision(kind, name)
}

final case class OcdSubdivision(kind: String, name: String)
