<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

Campaign Contribution API
=========================

This is a simple example that uses [Google Civic Information API](https://developers.google.com/civic-information)
and [MapLight Contribution Search API](https://maplight.org/data_guide/maplight-contribution-search-api/)
to aggregate campaign contributions stats, e.g., min, max, average, total, for each political party.

Scope
-----

First and foremost, this implementation is nowhere near production ready. This is merely a proof of concept (POC). However, one can improve upon this to use it in a production environment.

As this is a contrived example, the scope of the implementation is limited to major political parties, Democratic Party and Republican Party, and federal-level candidates:
  1. President
  1. Vice President
  1. U.S. Senators
  1. U.S. Representatives

There is a hardcoded list of [ISO 3166-2 subdivision codes](https://en.wikipedia.org/wiki/ISO_3166-2:US) for the United States; the list comprises 50 states, 1 district (Washington D.C.), and 6 territories. Refer to [UsSubdivision.scala](https://github.com/chrismin1202/campaign-contribution-api/blob/master/src/main/scala/com/chrism/api/standard/UsSubdivision.scala). This implementation collects information only from the 50 states.<br />
<br />
First, Google Civic Information API's [divisions endpoint](https://developers.google.com/civic-information/docs/v2/divisions) is used to look up congressional districts for each state. The endpoint returns Open Civic Data identifiers (`ocdId`) for the given query. The query sent to the endpoint has the following format:
```
country:us/state:[alpha2_state_code_goes_here]/
```
`[alpha2_state_code_goes_here]` is relplaced with the actual state code for each state, e.g., `nm` for New Mexico. Note that the trailing `/` at the end is important. Without it, the endpoint returns 503. One caveat to note is that `country:us/state:ky/` does not return the congressional districts for the state of Kentucky for some reason. As a hack, a field named `numCongressionalDistricts` is added to `UsSubdivision` as an optional field. The value for `KY` is set to 6 as there are 6 congressional districts in Kentucky currently. If the number of districts change due to redestricting, the value needs to be updated accordingly. Alternatively, you can hardcode the number of districts for all 50 states and avoid calling the divisions endpoint entirely. According to the documentation, most query operators of the Apache Lucene library are supported. So if you know Lucene syntax, you can improve the query. Although the endpoint returns not only congressional districts but also state-level districts, the client filters out all non-federal seats. Refer to [`CivicInformationClient.scala`](https://github.com/chrismin1202/campaign-contribution-api/blob/master/src/main/scala/com/chrism/api/google/CivicInformationClient.scala).<br />
<br />
Upon retrieving the OCD id for the congressional districts for the 50 states, [representativeInfoByDivision endpoint](https://developers.google.com/civic-information/docs/v2/representatives/representativeInfoByDivision) is used to look up the representatives for each state. As aforementioned, the scope is limited to federal level politicians. For President and Vice President, `ocd-division/country:us` is sent as `ocdId` as they represent the division `country:us`. For Senators, `ocd-division/country:us/state:[alpha2_state_code_goes_here]` is sent as `ocdId` as senators represent the entire state. Lastly, for Representatives, or what the API refers to as `legislatorLowerBody`, `ocd-division/country:us/state:[alpha2_state_code_goes_here]/cd:[district_number_goes_here]` is sent as `ocdId`. In total, the endpoint needs to be called 536 times for the implementation: 1 call for `ocd-division/country:us`, 100 calls for Senators, and 435 calls for Representatives. The response payload contains lots of information about the representatives of the divisions, but the bits of information that matters to this implementation are `officials[].name`, the name of the politician, and `officials[].party`, the political party the politician belongs. Refer to [Representatives](https://developers.google.com/civic-information/docs/v2/representatives) for the properties returned in the payload.<br />
<br />

After collecting all politicians via Civic Information API, the names of the politicians are sent to MapLight API's [Contributions endpoint](https://maplight.org/data_guide/contribution-search-api-documentation/) to look up donations they have received. If endpoint does not return any results, the name is sent to [Candidate name search endpoint](https://maplight.org/data_guide/contribution-search-api-documentation/). Candidate name search endpoint returns top 10 matches for the given search term, that is, the name of the politician. If there are 1 or more candidates returned by the endpoint, the candidates along with the name obtained from Civic Information API are passed to `matchCandidate` method in [`StatsLoader`](https://github.com/chrismin1202/campaign-contribution-api/blob/master/src/main/scala/com/chrism/api/stats/StatsLoader.scala) object. Note that the method is naively implemented; it just matches the OCD division and name. If there are 2 or more candidates that match the criteria, the method does not disambiguate any furuther and gives up. If the method finds the matching candidate, the candidate's MapLight id, the unique numeric identifier generated by MapLight for each candidate, is sent to Contribution endpoint. If the endpoint does not return any results, chances are that the donation information for the candidate is not indexed in MapLight's dataset.<br />
<br />
The response payload of Contributions endpoint contains aggregate totals and transaction details for each election cycle since 2008, but this implementation only looks at the aggregate totals for the sake of simplicity. Also, not all transaction details are captured anyway. For example,
```json
"aggregate_totals": [
  {
    "total_amount": 3829887.43,
    "contributions": 3239
  }
]
```
there are 3239 contributions, but the transaction details captured in `rows` field do not contain all 3239 contributions.<br />
<br />
The values of `total_amount` and `contributions` are then first aggregated for each candidate and persisted for reuse later. The values are aggregated again at party level, i.e., grouped by party name and the statistics are persisted. For each candidate, the implementation simply sums `total_amount` and `contributions`, whereas for each party, it computes
  - `numCandidates`: the number of candidates for the party
  - `totalContributions`: the total number of contributions, i.e., the sum of `contributions` for all candidates belong to the party
  - `minContributions`: the minimum number of contributions,
  - `maxContributions`: the maximum number of contributions,
  - `avgContributions`: the average number of contributions per candidate,
  - `totalDonation`: the total donation amount, i.e., the sum of `total_amount` for all candidates belong to the party,
  - `minDonation`: the minimum amount of donation received by a candidate,
  - `maxDonation`: the maximum amount of donation received by a candidate,
  - `avgDonation`: the average donation amount (per transaction) computed by dividing `totalDonation` by `totalContributions`

Note that the values of `minContributions` and `maxContributions` are the minimum and the maximum number of donation received by a single candidate for the party respectively, but the name of the candidate is not captured as there can be more than 1 candidate with the same value. Similarly, `minDonation` and `maxDonation` are the minimum and the maximum amount of donation aggregated for a candidate, but again, the name of the candidate is not captured as there can be 1 or more candidate with the same donation amount.

Architecture
------------

The key underlying technologies of the implementation includes [`http4s`](https://http4s.org/v0.21/) for handling HTTP (REST), [`json4s`](http://json4s.org/) for handling JSON, and [Apache Spark](https://spark.apache.org/docs/2.4.5/).

  - `http4s`<br />
    Used for calling Civic Information and MapLight APIs as well as building the API for this implementation.
  - `json4s`<br />
    Although `http4s` supports several different JSON libraries, `json4` is chosen as Spark uses `json4s` for handling JSON.
  - Apache Spark<br />
    Spark is used for calling the 3rd party APIs used in this implementation and aggregating the data. Note that this implementation is designed to be run on a single machine; therefore, the use of Spark is actually an overkill. However, this layer is added as this implementation is a POC to demonstrate how Spark _can be_ used in production. In a real production environment, Spark is used in a distributed computing cluster, such as Hadoop, so larger amount of data can be processed and more API calls can be made in parallel. However, calling 3rd party API in a Spark application can be dangerous as one can easily DDoS the API server. In fact, in this implementation, when `representativeInfoByDivision` is called, the `Dataset` is `repartition`ed to 1 due to Google's rate-limiting.

At the application startup, the Spark application for aggregating data is run ([`StatsLoader`](https://github.com/chrismin1202/campaign-contribution-api/blob/master/src/main/scala/com/chrism/api/stats/StatsLoader.scala)) to preload the data to the server. This can take a while as over 1000 API calls need to be made to 3 different 3rd party API endpoints. Once the server is preloaded with the data, the server is ready to use.<br />
Currently, there is no authentication layer, so one can test the API using `curl` or Postman without obtaining OAuth token or API key.

How to Run
----------

There are 2 ways to run this application.
  1. Command line
  1. In an IDE (preferably IntelliJ)

For both approaches, one must use the the correct version of Java, Scala, and sbt.
  - **Java**: 8
  - **Scala**: 2.12.10
  - **sbt**: 1.3.8
Note that any subversions of Scala 2.12.x should work, but Scala 2.12.8 or higher is recommended. Similarly, any subversion of sbt 1.3.x should be compatible.

### Using command line

If you are on macOS or Linux, this instruction should work, but if you are on Windows, god bless ya.<br />
<br />
Assuming that Linux users are savvy enough to install the required versions of Java, Scala, and sbt on their own, here's the instruction for macOS using [Homebrew](https://brew.sh/).<br />
<br />
  1. Set up Google API key.<br />
      The endpoints used in this implementation does not require OAuth token, so API key should suffice. Refer to [this instruction](https://support.google.com/googleapi/answer/6158862?hl=en&ref_topic=7013279).
  1. Install Java (OpenJDK).
      ```sh
      brew tap AdoptOpenJDK/openjdk
      ```
      and then
      ```sh
      brew cask install adoptopenjdk8
      ```
      If you prefer Oracle JDK, Google how to install or downgrade (assuming that you have Java 9 or higher on your system) Oracle JDK.
  1. Install Scala.
      ```sh
      brew install scala@2.12
      ```
      The command should install the latest version of Scala 2.12.
  1. Install sbt.
      ```sh
      brew install sbt
      ```
      The command shoud install the latest version of sbt.
  1. Clone this repository and `cd` into it.
  1. Compile the sbt project.
      ```sh
      sbt assembly
      ```
      `assembly` command generates an uber jar in `target` directory.
  1. Run the application.
      ```sh
      java \
        -cp target/scala-2.12/campaign-contribution-api-assembly-0.0.1.jar \
          -Dapi.basePath="/any/arbitrary/empty/directory/that/exits" \
          -Dapi.google.apiKey="YOUR_GOOGLE_API_KEY_GOES_HERE" \
      com.chrism.api.CampaignContributionService
      ```
      If you are not running from the root directory of your repository, you need to replace `target/scala-2.12/campaign-contribution-api-assembly-0.0.1.jar` with the fully qualified path. `/any/arbitrary/empty/directory/that/exits` should be replaced with the actual path you want to use for persisting data, e.g., `/tmp/campaign-contribution-test`, and `YOUR_GOOGLE_API_KEY_GOES_HERE` with the actual API key.<br />
      Note that this implementation uses Java System Properties for setting configurations rather than command line arguments or environment variables. Note `-Dapi.basePath` and `-Dapi.google.apiKey`.<br />
      Also note that due to the number of API calls that need to be made, the application limits the number of states to a few. Refer to `CampaignContributionService.DefaultStates` for the states that are included in the aggregates. To override, you can pass in `-Dapi.states` to the command. To include all 50 states:
      ```sh
      -Dapi.states=all
      ```
      To include sepecific set of states, comma-separate the state codes:
      ```sh
      -Dapi.states=ga,ca,la
      ```
      State names are acceptable as well.
  1. Test the API.
      Run the following `curl` command from another terminal
      ```sh
      curl http://127.0.0.1:8080/contributions/[PATH_GOES_HERE]
      ```
      or paste `http://127.0.0.1:8080/contributions/[PATH_GOES_HERE]` in your browser.<br />
      The base URL should be `http://127.0.0.1:8080`, but check when the application boots up.
      ```sh
      20/04/21 19:59:41 INFO
          _   _   _        _ _
        | |_| |_| |_ _ __| | | ___
        | ' \  _|  _| '_ \_  _(_-<
        |_||_\__|\__| .__/ |_|/__/
                    |_|
      20/04/21 19:59:42 INFO http4s v0.21.3 on blaze v0.14.11 started at http://127.0.0.1:8080/
      ```
      The actual host and port should be printed to the console. Note that the application can fail to boot up if the port `8080` is already bound. If the port `8080` _is_ actually bound, the port number needs to be changed by passing in another property:
      ```sh
      -Dapi.port=8081
      ```
      Replace `8081` with the actual port number that is not in use.<br />
      Currently, there is only 1 endpoint: `contributions`. `[PATH_GOES_HERE]` should be replaced with one of the following values:
        - `all`: returns the stats for all political parties
        - `democrat`: returns the stats for Democratic Party<br />
          Other acceptable variations include `D`, `Democrats`, `Democratic`, and `Democratic Party`.
        - `republican`: returns the stats for Republican Party<br />
          Other accetable variations include `R`, `Republicans`, and `Republican Party`.

      Note that the values are case-insensitive.<br />
      The endpoint should return JSON that looks similar to this depending on which party you choose:
      ```json
      {
        "donations": [
          {
            "party": "Republican Party",
            "totalContributions": 28988,
            "minContributions": 35,
            "maxContributions": 21285,
            "avgContributions": 7247,
            "totalDonation": 45562788.69,
            "minDonation": 33878,
            "maxDonation": 31952254.65,
            "avgDonation": 1571.7810366358492,
            "numCandidates": 5
          },
          {
            "party": "Democratic Party",
            "totalContributions": 80164,
            "minContributions": 2970,
            "maxContributions": 72732,
            "avgContributions": 40082,
            "totalDonation": 21947158.56,
            "minDonation": 1950808.91,
            "maxDonation": 12783218.45,
            "avgDonation": 273.7782366149394,
            "numCandidates": 3
          }
        ]
      }
      ```
      except that the returned JSON is not beautified.

### IDE (IntelliJ)

[IntelliJ](https://www.jetbrains.com/idea/) is a good IDE for Scala development as it comes with bundled Scala SDKs and sbt plugins. As long as you choose the correct version of Scala SDK (2.12.10), you should be able to run the unit test cases within IntelliJ.

  1. Open/import the project as an sbt project in IntelliJ.
  1. Once IntelliJ finishes building the project, open [`CampaignContributionServiceTest`](https://github.com/chrismin1202/campaign-contribution-api/blob/master/src/test/scala/com/chrism/api/CampaignContributionServiceTest.scala).<br />
     If you run the test without setting the API key to the JVM System Properties, the test case will not run, but it will not fail. To add your API key to System Properties, click `Edit Configurations...` from `Run` menu and paste `-Dapi.google.apiKey="YOUR_GOOGLE_API_KEY_GOES_HERE"` to `VM options`. Alternatively, you can alter the test by removing `withSystemPropertiesIfSet` and set the API key directly to `apiKey` variable.

Future Works
------------

There are several limitations to this implementation and those limitations need to be addressed before productionizing.

  - The implementation is batch-oriented.<br />
    At startup, the Spark batch job is executed and aggregates the data once and for all. It is not too bad to incur the startup overhead once, but it is not a good idea to use Spark directly from a web API unless latency is acceptable. There needs to be a real-time streaming pipeline that continuously augment the data. The options for real-time pipeline include Kafka, Spark Streaming, and cloud-native data streaming service like Kinesis.
  - Persistence<br />
    Local file system is used rather than HDFS, cloud-native file storage solution, e.g., S3 on AWS or Blob Storage on Azure, RDBMS, or NoSQL. Local file system is local to each machine so if multiple server instances exist, this approach will not work. Therefore, storage needs to be decoupled from compute. Furthermore, for real-time or incremental data ingestion, the storage solution needs to be able to handle not only insert but also update operation. If HDFS or S3 is used, [Delta Lake](https://delta.io/) can be used for UPSERT operation.
  - Spark is used locally in a single machine.<br />
    As the Spark master is set to `local[*]`, this implementation does not require you to set up your environment for `spark-submit`. In a real production environment, there should be a Spark cluster on the side decoupled from the API.
  - HTTPS<br />
    This implementation does not support HTTP. In a production environment, HTTPS should be used obviously.

License
-------
This code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0).
