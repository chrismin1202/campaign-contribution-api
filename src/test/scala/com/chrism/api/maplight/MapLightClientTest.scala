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
package com.chrism.api.maplight

import com.chrism.api.FunTestSuite
import com.chrism.api.log.Logging
import org.json4s.native.Serialization

final class MapLightClientTest extends FunTestSuite with ContributionSearchJson4sFormatsLike with Logging {

  test("GET searching candidate by name") {
    val searchText = "Joe Biden"
    logger.info(s"Searching candidate by name: $searchText")
    val response = MapLightClient.searchCandidate("Joe Biden").unsafeRunSync()
    println(Serialization.write(response))
  }

  test("GET contributions by name") {
    val name = "Donald Trump"
    logger.info(s"GET contribution by name: $name")
    val response = MapLightClient.getContributionByCandidateName(name).unsafeRunSync()
    println(Serialization.write(response))
  }

  test("GET contributions by id") {
    val mapLightId = 4402L
    logger.info(s"GET contribution by MapLight id: $mapLightId")
    val response = MapLightClient.getContributionByCandidateId(mapLightId).unsafeRunSync()
    println(Serialization.write(response))
  }
}
