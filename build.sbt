scalaVersion := "2.11.6"

minSdkVersion := "9"

resolvers += "jcenter" at "http://jcenter.bintray.com"

libraryDependencies ++= Seq(
  aar("org.macroid" %% "macroid" % "2.0.0-M4"),
  "com.android.support" % "support-v4" % "22.1.1"
)
