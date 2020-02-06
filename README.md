# OCS OSX
This project is meant to build notarized versions of the PIT/OT/QPT distributions for MacOS

Notarization is required by Apple starting on Feb 3rd 2020 and it means applications
not distrubiuted over the AppStore need to be signed and sent to apple for notarization.

Otherwise it will be harder to install these applications on Catalina starting on Feb 3rd.

Ideally this would be integrated into the OCS build but given how old and complex the current
build is (mainly due to OSGi), it is easier to do the build of the DMGs separately. Nonetheless
it maybe possible to add this build to ocs given enough time and interest

# JRE

We have always embedded a jre into our dmgs to avoid issues when users use their own jvm.
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

``$HOME/.jres13/jre``

Note that the top dir under jre must be `Contents`

# Launcher

This project doesn't use OSGi as it is hard to run OSGi in newer jdks. Instead we do the
wiring ourselves and launch as a regular Java application. In the case of the PIT, there is a
PITLauncher bundle that already does most of the wiring, thus in the pit case there is no need
of extra code and it is enough to just declare `pitlauncher` as a dependency.

Note this assumes all modules have been properly published, either locally or to some public repo.
All testing has been done with local publication

# Code changes
Given the ocs code is written on jdk 1.8 but runs on jdk 13, there can be code incompatibilities due
to deprecated code or newer bugs:
The following have been identified so far:

* Apple custom ui classes (eawt) have been deprecated. They were called via reflection originally thus
it is not a real issue
* Legacy actors: Scala actors won't run on jdks newer than 1.8, the code needs to be rewritten

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

# Notarization

The task will send the code to be notarized and it will uplooad the code and give a reference UUID like

``````
[info] RequestUUID = bb2ba1c8-250c-4059-881c-d478db8b2e42``
``````

The notarization process takes a few minutes and ideally we'd check this later on with the command

``````
xcrun altool --notarization-info  2aa00d95-9348-4205-b4d6-ce70439db87d -u "its@gemini.edu" -p "@keychain:AC_PASSWORD"
``````

A final step of stapling the dmg is possible (though not required) to let users without internet open the app

``````
xcrun stapler staple "<DmgPath>.dmg"
``````
# Some useful links
https://developer.apple.com/documentation/xcode/notarizing_macos_software_before_distribution

https://developer.apple.com/documentation/xcode/notarizing_macos_software_before_distribution/customizing_the_notarization_workflow


https://sbt-native-packager.readthedocs.io/en/latest/index.html

