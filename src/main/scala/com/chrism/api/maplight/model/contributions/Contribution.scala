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

import java.time.LocalDate

import org.json4s.FieldSerializer
import org.json4s.FieldSerializer.{renameFrom, renameTo}

final case class Contribution(
  electionCycle: Int,
  transactionDate: LocalDate,
  transactionAmount: BigDecimal,
  donorName: String,
  donorOrganization: String,
  candidateName: String,
  candidateMapLightId: Long,
  candidateFecId: String)

object Contribution {

  @transient
  lazy val fieldSerializer: FieldSerializer[Contribution] =
    FieldSerializer[Contribution](
      serializer = renameTo("electionCycle", "ElectionCycle")
        .orElse(renameTo("transactionDate", "TransactionDate"))
        .orElse(renameTo("transactionAmount", "TransactionAmount"))
        .orElse(renameTo("donorName", "DonorName"))
        .orElse(renameTo("donorOrganization", "DonorOrganization"))
        .orElse(renameTo("candidateName", "CandidateName"))
        .orElse(renameTo("candidateMapLightId", "CandidateMaplightID"))
        .orElse(renameTo("candidateFecId", "CandidateFECID")),
      deserializer = renameFrom("ElectionCycle", "electionCycle")
        .orElse(renameFrom("TransactionDate", "transactionDate"))
        .orElse(renameFrom("TransactionAmount", "transactionAmount"))
        .orElse(renameFrom("DonorName", "donorName"))
        .orElse(renameFrom("DonorOrganization", "donorOrganization"))
        .orElse(renameFrom("CandidateName", "candidateName"))
        .orElse(renameFrom("CandidateMaplightID", "candidateMapLightId"))
        .orElse(renameFrom("candidateMaplightID", "candidateMapLightId"))
        .orElse(renameFrom("candidateMaplightId", "candidateMapLightId"))
        .orElse(renameFrom("CandidateFECID", "candidateFecId"))
        .orElse(renameFrom("candidateFECID", "candidateFecId"))
    )
}
