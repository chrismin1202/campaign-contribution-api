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

/** Represents an address at which to contact the official.
  *
  * @param locationName the name of the location
  * @param line1 the street name and number of this address
  * @param line2 the second line the address, if needed
  * @param line3 the third line of the address, if needed
  * @param city the city or town for the address
  * @param state the US two letter state abbreviation of the address
  * @param zip the US Postal Zip Code of the address
  */
final case class Address(
  locationName: Option[String] = None,
  line1: Option[String] = None,
  line2: Option[String] = None,
  line3: Option[String] = None,
  city: Option[String] = None,
  state: Option[String] = None,
  zip: Option[String] = None)

object Address {

  implicit def decoder(implicit formats: Formats): EntityDecoder[IO, Address] = jsonExtract[IO, Address]
}
