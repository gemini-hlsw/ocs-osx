// Extracted from `ocs` build.
final case class OcsVersion(semester: String, test: Boolean, xmlCompatibility: Int, serialCompatibility: Int, minor: Int) {

  private val Pat = "(\\d{4})([AB])".r
  private val Pat(year, half) = semester
  private val halfDigit = if (half == "A") 0 else 1

  /** Convert to an OSGi-compatible version. */
  def toOsgiVersion: String =
    f"${year}%s${halfDigit}%d${xmlCompatibility}%02d.${serialCompatibility}%d.${minor}%d"

  def sourceFileName = "CurrentVersion.java"

  override def toString: String = {
    val testString = if (test) "-test" else ""
    s"${semester}${testString}.$xmlCompatibility.$serialCompatibility.$minor"
  }

}
