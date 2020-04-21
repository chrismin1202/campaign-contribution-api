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
package com.chrism.api.google

import com.chrism.api.FunTestSuite
import com.chrism.api.config.SystemPropertiesHandle
import com.chrism.api.log.Logging
import com.chrism.api.standard.UsSubdivision
import org.json4s.native.Serialization

final class CivicInformationClientTest
    extends FunTestSuite
    with CivicInformationJson4sFormatsLike
    with SystemPropertiesHandle
    with Logging {

  import CivicInformationClient.ApiKeyPropKey

  test("GET: congressional districts") {
    withSystemPropertiesIfSet(ApiKeyPropKey) { props =>
      logger.info(s"The system properties '$ApiKeyPropKey' is set. Running the test case...'")

      val apiKey = props(ApiKeyPropKey)
      val client = CivicInformationClient(apiKey)
      val state = UsSubdivision.NM
      logger.info(s"GET congressional districts for state: ${state.name}")
      val response = client.getCongressionalDistricts(state).unsafeRunSync()
      println(Serialization.write(response))
    } {
      logger.info(s"The system properties '$ApiKeyPropKey' is not set. Skipping the test case...'")
    }
  }

  test("GET representatives") {
    withSystemPropertiesIfSet(ApiKeyPropKey) { props =>
      logger.info(s"The system properties '$ApiKeyPropKey' is set. Running the test case...'")

      val apiKey = props(ApiKeyPropKey)
      val client = CivicInformationClient(apiKey)
      val ocdId = "ocd-division/country:us/state:nm"
      logger.info(s"GET representatives for OCD id: $ocdId")
      val response = client.getRepresentativesByOcdId(ocdId).unsafeRunSync()
      println(Serialization.write(response))
    } {
      logger.info(s"The system properties '$ApiKeyPropKey' is not set. Skipping the test case...'")
    }
  }
}
