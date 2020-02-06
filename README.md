This project is meant to build notarized versions of the PIT/OT/QPT distributions for MacOS

Ideally this would be integrated into the OCS build but given how old the current build is,
and the complexities due to OSGi, it is easier to do the build of the DMGs separately.

Notarization is required by Apple starting on Feb 3rd 2020 and it means applications
not distrubiuted over the AppStore need to be signed and sent to apple for notarization.

Otherwise it will be harder to install these applications on Catalina starting on Feb 3rd.

# JRE

We have always embedded a jvm into our dmgs to avoid issues when users use their own jvm.
Unfortunately not every JDK can be notarized, in particular JDK 1.8 cannot be notarized as the
toolchain used to build it is too old.

Thus we opted to use JDK 13 though the application itself is compiled on JDK 1.8

The selected jdk is AdoptOpenJDK version 13 which can be downloaded at
https://adoptopenjdk.net/?variant=openjdk13&jvmVariant=hotspot

Note you need to get the macOS version of the JRE, at the time of this writing it is version:
13.0.2+8

AdoptOpenJDK lets you freely distribute the JRE without worries about licensing

# JRE Installation

The JRE comes in a zip with a root directory, you need to unzip and copy the contents to the dir:

$HOME/.jres13/jre

Note that the top dir under jre must be `Contents`

# Launcher

This project doesn't use OSGi as it is hard to run OSGi in newer jdks. Instead we do the
wiring ourselves and launch as a regular Java application. In the case of the PIT, there is a
PITLauncher bundle that already does most of the wiring, thus in the pit case there is no need
of extra code and it is enough to just declare `pitlauncher` as a dependency.

Note this assumes all modules have been properly published, either locally or to some public repo.
All testing has been done with local publication

# Signatures and app passwords

All code need to be signed, you need to import the public key and certificate into your keychain
https://github.com/gemini-hlsw/ocs/wiki/Signing-Applications-for-OSX#install-certificate

The certificate is managed by ITS and they can provide a copy if needed

Additionally an app password needs to be on the keychain according to
https://developer.apple.com/documentation/xcode/notarizing_macos_software_before_distribution/customizing_the_notarization_workflow

The password is available at the interal password site

# Packaging
The project uses sbt-native-packager but it adds a custom format that will build the dmg, sign it and
notarize it, it can be called via e.g.

`notarizedDmgFormat:packageBin`

For the particular case of the pit an alias has been created `pitDmg`

Mappings and settings can be configured as usual for a Java Application, see:
https://sbt-native-packager.readthedocs.io/en/latest/archetypes/java_app/index.html

Please note that the dock:icon and dock:name require special settings that you can see
on the example for the PIT

Other JVM arguments can be passed as for any java application

Some useful links:
https://developer.apple.com/documentation/xcode/notarizing_macos_software_before_distribution
https://developer.apple.com/documentation/xcode/notarizing_macos_software_before_distribution/customizing_the_notarization_workflow
https://sbt-native-packager.readthedocs.io/en/latest/index.html
