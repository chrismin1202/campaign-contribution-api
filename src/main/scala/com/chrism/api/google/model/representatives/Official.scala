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

/** Represents an official holding an office.
  *
  * @param name the name of the official
  * @param party the full name of the party the official belongs to
  * @param photoUrl a URL for a photo of the official
  * @param phones the official's public contact phone numbers
  * @param emails the direct email addresses for the official
  * @param address the addresses at which to contact the official
  * @param urls the official's public website URLs
  * @param channels a list of known (social) media channels for this official
  */
final case class Official(
  name: String,
  party: Option[String] = None,
  photoUrl: Option[String] = None,
  phones: Option[Seq[String]] = None,
  emails: Option[Seq[String]] = None,
  address: Option[Seq[Address]] = None,
  urls: Option[Seq[String]] = None,
  channels: Option[Seq[Channel]] = None)

object Official {

  implicit def decoder(implicit formats: Formats): EntityDecoder[IO, Official] = jsonExtract[IO, Official]
}
