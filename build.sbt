scalaVersion := "2.11.6"

minSdkVersion := "9"

scalacOptions ++= Seq("-target:jvm-1.7", "-Xexperimental")

javacOptions ++= Seq("-source", "1.7")

resolvers += "jcenter" at "http://jcenter.bintray.com"

libraryDependencies ++= Seq(
  aar("org.macroid" %% "macroid" % "2.0.0-M4"),
  aar("org.macroid" %% "macroid-viewable" % "2.0.0-M4"),
  "com.android.support" % "support-v4" % "22.1.1",
  "com.google.android.gms" % "play-services-drive" % "7.3.0",
  "net.virtual-void" %%  "json-lenses" % "0.6.0"
)
