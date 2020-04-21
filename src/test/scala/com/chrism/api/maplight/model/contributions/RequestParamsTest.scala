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
package com.chrism.api.maplight.model.contributions

import com.chrism.api.FunTestSuite

final class RequestParamsTest extends FunTestSuite {

  test("constructing URL parameters") {
    val params = RequestParams(
      candidateName = Some(Seq("Donald Trump", "Joseph Biden")),
      candidateFecId = Some(Seq("P80001571", "P80000722")),
      candidateMapLightId = Some(Seq(9528L, 4533L)),
      electionCycle = Some(Seq(2020)),
      donorOrganization = Some(Seq("The Light Guy", "Daily Journal"))
    )
    params.urlParams should contain theSameElementsAs Map(
      "candidate_name" -> "Donald Trump|Joseph Biden",
      "candidate_fecid" -> "P80001571|P80000722",
      "candidate_mlid" -> "9528|4533",
      "election_cycle" -> "2020",
      "donor_organization" -> "The Light Guy|Daily Journal",
    )
  }
}
