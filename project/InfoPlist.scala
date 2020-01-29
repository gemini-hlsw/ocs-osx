import scala.xml.dtd.{ PublicID, DocType }
import scala.xml.XML
import sbt.File

object InfoPlist {
  val extId = PublicID("-//Apple Computer//DTD PLIST 1.0//EN", "http://www.apple.com/DTDs/PropertyList-1.0.dtd")
  val docType = DocType("plist", extId, Nil)
}

case class InfoPlist(id: String, name: String, version: String, shortVersion: String, mainClass: String, props: Map[String, String], vmOpts: Seq[String], icon: Option[File], jreDir: String) {

        // <key>CFBundleExecutable</key>
        // <string>{MacDistHandler.launcher}</string>
  def xml =
    <plist version="1.0">
      <dict>
          {(icon map { f =>
              <key>CFBundleIconFile</key>
              <string>{f.getName}</string>
          }).toList}
        <key>CFBundleIdentifier</key>
        <string>{ id }</string>
        <key>CFBundleInfoDictionaryVersion</key>
        <string>6.0</string>
        <key>CFBundleName</key>
        <string>{ name }</string>
        <key>CFBundleDisplayName</key>
        <string>{ name }</string>
        <key>CFBundlePackageType</key>
        <string>APPL</string>
        <key>CFBundleShortVersionString</key>
        <string>{ shortVersion }</string>
        <key>CFBundleVersion</key>
        <string>{ version }</string>
        <key>JVMRuntime</key>
        <string>{jreDir}</string>
        <key>JVMMainClassName</key>
        <string>{ mainClass }</string>
        <key>JVMOptions</key>
        <array>
          {
            vmOpts.map { x =>
              <string>{ x }</string>
            }
          }
          {
            props.map {
              case (k, v) =>
                <string>{s"-D$k=$v"}</string>
            }
          }
          <string>-Xdock:name={ name}</string>
        </array>
        <key>WorkingDirectory</key>
        <string>$APP_ROOT/Contents/Resources</string>
        <key>NSHighResolutionCapable</key>
        <true/>
      </dict>
    </plist>
}
