import scala.xml.dtd.{ DocType, PublicID }
import scala.xml.XML
import sbt.File

object InfoPlist {
  val extId = PublicID("-//Apple Computer//DTD PLIST 1.0//EN",
                       "http://www.apple.com/DTDs/PropertyList-1.0.dtd"
  )
  val docType = DocType("plist", extId, Nil)
}

case class InfoPlist(
  executable:   String,
  id:           String,
  name:         String,
  version:      String,
  shortVersion: String,
  mainClass:    String,
  icon:         Option[File]
) {

  def xml =
    <plist version="1.0">
      <dict>
        <key>CFBundleExecutable</key>
        <string>{executable}</string>
        {
      icon.map { f =>
        <key>CFBundleIconFile</key>
        <string>{f.getName}</string>
      }.toList
    }
        <key>CFBundleIdentifier</key>
        <string>{id}</string>
        <key>CFBundleInfoDictionaryVersion</key>
        <string>6.0</string>
        <key>CFBundleName</key>
        <string>{name}</string>
        <key>CFBundleDisplayName</key>
        <string>{name}</string>
        <key>CFBundlePackageType</key>
        <string>APPL</string>
        <key>CFBundleShortVersionString</key>
        <string>{shortVersion}</string>
        <key>CFBundleVersion</key>
        <string>{version}</string>
        <key>NSHighResolutionCapable</key>
        <true/>
      </dict>
    </plist>

  def write(file: String) {
    XML.save(file, xml, "UTF-8", true, InfoPlist.docType)
  }
}
