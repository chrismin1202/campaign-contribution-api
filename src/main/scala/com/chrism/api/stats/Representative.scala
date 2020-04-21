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

import com.chrism.api.google.model.representatives.{Office, Official, RepresentativesResponse}
import com.chrism.api.ocd.OcdDivision

final case class Representative(name: String, officeName: String, ocdId: String, party: Option[String]) {

  @transient
  lazy val division: OcdDivision = OcdDivision.parseId(ocdId)
}

object Representative {

  def apply(office: Office, official: Official): Representative =
    Representative(official.name, office.name, office.divisionId, official.party)

  object implicits {

    implicit final class RepresentativesResponseOps(response: RepresentativesResponse) {

      def presidentOrNone: Option[Representative] = firstRepresentativeOfOrNone(_.isPresident)

      def vicePresidentOrNone: Option[Representative] = firstRepresentativeOfOrNone(_.isVicePresident)

      def usRepresentativesOrNone: Option[Seq[Representative]] = representativesOfOrNone(_.isUsRepresentative)

      def usSenatorsOrNone: Option[Seq[Representative]] = representativesOfOrNone(_.isUsSenator)

      private[this] def firstRepresentativeOfOrNone(p: Office => Boolean): Option[Representative] =
        if (response.isEmpty) None
        else
          response.officials
            .flatMap(officials =>
              response.offices
                .flatMap(_.collectFirst {
                  case f if p(f) => Representative(f, officials(f.officialIndices.head))
                }))

      private[this] def representativesOfOrNone(p: Office => Boolean): Option[Seq[Representative]] =
        response.officials
          .flatMap(officials =>
            response.offices.map(_.filter(p).flatMap(o => o.officialIndices.map(officials).map(Representative(o, _)))))
          .filter(_.nonEmpty)
    }
  }
}
