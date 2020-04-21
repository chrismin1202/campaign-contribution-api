package com.chrism.api.standard

import org.apache.commons.lang3.StringUtils

/** Represents an ISO 3166-2 subdivision of the United States.
  *
  * @param code the ISO 3166-2 code (lower-cased)
  * @param name the human-readable name
  * @param kind the kind of subdivision, e.g., state
  * @param numCongressionalDistricts the number of congressional districts (optional)
  */
sealed abstract class UsSubdivision(
  final val code: String,
  final val name: String,
  final val kind: SubdivisionKind,
  final val numCongressionalDistricts: Option[Int] = None)
    extends Product
    with Serializable {

  import UsSubdivision.{OcdDivision, UsDivisionId}

  require(StringUtils.isNotBlank(code), "The code cannot be blank!")
  require(StringUtils.isAllLowerCase(code), "The code must be lower-cased!")
  require(code.length == 2, "The code must be 2 letter ISO 3166-2 subdivision code!")
  require(StringUtils.isNotBlank(name), "The name cannot be blank!")

  @transient
  final lazy val divisionId: String = s"$UsDivisionId/$kind:$code"

  @transient
  final lazy val ocdId: String = s"$OcdDivision/$divisionId"

  @transient
  final lazy val congressionalDistrictOcdIds: Option[Seq[String]] =
    numCongressionalDistricts.map(n => (1 to n).map(i => s"$ocdId/cd:$i"))
}

object UsSubdivision {

  import SubdivisionKind.{District, State, Territory}

  val OcdDivision: String = "ocd-division"
  val UsDivisionId: String = "country:us"
  val UsOcdId: String = s"$OcdDivision/$UsDivisionId"

  val Values: Set[UsSubdivision] = Set(
    AL,
    AK,
    AZ,
    AR,
    CA,
    CO,
    CT,
    DE,
    FL,
    GA,
    HI,
    ID,
    IL,
    IN,
    IA,
    KS,
    KY,
    LA,
    ME,
    MD,
    MA,
    MI,
    MN,
    MS,
    MO,
    MT,
    NE,
    NV,
    NH,
    NJ,
    NM,
    NY,
    NC,
    ND,
    OH,
    OK,
    OR,
    PA,
    RI,
    SC,
    SD,
    TN,
    TX,
    UT,
    VT,
    VA,
    WA,
    WV,
    WI,
    WY,
    DC,
    AS,
    GU,
    MP,
    PR,
    UM,
    VI,
  )

  def parseOrNone(codeOrName: String): Option[UsSubdivision] =
    Option(codeOrName).flatMap(n => Values.find(v => v.code.equalsIgnoreCase(n) || v.name.equalsIgnoreCase(n)))

  def parse(codeOrName: String): UsSubdivision =
    parseOrNone(codeOrName)
      .getOrElse(throw new IllegalArgumentException(s"$codeOrName is not a parsable subdivision code or name!"))

  case object AL extends UsSubdivision("al", "Alabama", State)
  case object AK extends UsSubdivision("ak", "Alaska", State)
  case object AZ extends UsSubdivision("az", "Arizona", State)
  case object AR extends UsSubdivision("ar", "Arkansas", State)
  case object CA extends UsSubdivision("ca", "California", State)
  case object CO extends UsSubdivision("co", "Colorado", State)
  case object CT extends UsSubdivision("ct", "Connecticut", State)
  case object DE extends UsSubdivision("de", "Delaware", State)
  case object FL extends UsSubdivision("fl", "Florida", State)
  case object GA extends UsSubdivision("ga", "Georgia", State)
  case object HI extends UsSubdivision("hi", "Hawaii", State)
  case object ID extends UsSubdivision("id", "Idaho", State)
  case object IL extends UsSubdivision("il", "Illinois", State)
  case object IN extends UsSubdivision("in", "Indiana", State)
  case object IA extends UsSubdivision("ia", "Iowa", State)
  case object KS extends UsSubdivision("ks", "Kansas", State)
  // division lookup via Google Civic Information API fails to return congressional districts for some reason
  case object KY extends UsSubdivision("ky", "Kentucky", State, numCongressionalDistricts = Some(6))
  case object LA extends UsSubdivision("la", "Louisiana", State)
  case object ME extends UsSubdivision("me", "Maine", State)
  case object MD extends UsSubdivision("md", "Maryland", State)
  case object MA extends UsSubdivision("ma", "Massachusetts", State)
  case object MI extends UsSubdivision("mi", "Michigan", State)
  case object MN extends UsSubdivision("mn", "Minnesota", State)
  case object MS extends UsSubdivision("ms", "Mississippi", State)
  case object MO extends UsSubdivision("mo", "Missouri", State)
  case object MT extends UsSubdivision("mt", "Montana", State)
  case object NE extends UsSubdivision("ne", "Nebraska", State)
  case object NV extends UsSubdivision("nv", "Nevada", State)
  case object NH extends UsSubdivision("nh", "New Hampshire", State)
  case object NJ extends UsSubdivision("nj", "New Jersey", State)
  case object NM extends UsSubdivision("nm", "New Mexico", State)
  case object NY extends UsSubdivision("ny", "New York", State)
  case object NC extends UsSubdivision("nc", "North Carolina", State)
  case object ND extends UsSubdivision("nd", "North Dakota", State)
  case object OH extends UsSubdivision("oh", "Ohio", State)
  case object OK extends UsSubdivision("ok", "Oklahoma", State)
  case object OR extends UsSubdivision("or", "Oregon", State)
  case object PA extends UsSubdivision("pa", "Pennsylvania", State)
  case object RI extends UsSubdivision("ri", "Rhode Island", State)
  case object SC extends UsSubdivision("sc", "South Carolina", State)
  case object SD extends UsSubdivision("sd", "South Dakota", State)
  case object TN extends UsSubdivision("tn", "Tennessee", State)
  case object TX extends UsSubdivision("tx", "Texas", State)
  case object UT extends UsSubdivision("ut", "Utah", State)
  case object VT extends UsSubdivision("vt", "Vermont", State)
  case object VA extends UsSubdivision("va", "Virginia", State)
  case object WA extends UsSubdivision("wa", "Washington", State)
  case object WV extends UsSubdivision("wv", "West Virginia", State)
  case object WI extends UsSubdivision("wi", "Wisconsin", State)
  case object WY extends UsSubdivision("wy", "Wyoming", State)
  case object DC extends UsSubdivision("dc", "District of Columbia", District)
  case object AS extends UsSubdivision("as", "American Samoa", Territory)
  case object GU extends UsSubdivision("gu", "Guam", Territory)
  case object MP extends UsSubdivision("mp", "Northern Mariana Islands", Territory)
  case object PR extends UsSubdivision("pr", "Puerto Rico", Territory)
  case object UM extends UsSubdivision("um", "United States Minor Outlying Islands", Territory)
  case object VI extends UsSubdivision("vi", "US Virgin Islands", Territory)
}
