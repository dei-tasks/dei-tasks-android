name := """mindmup-tasks"""

version := "1.0.0"

scalaVersion := "2.11.6"

minSdkVersion := "14"

platformTarget in Android := "android-22"

scalacOptions ++= Seq("-target:jvm-1.7", "-Xexperimental")

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

// Repositories for dependencies
resolvers ++= Seq(Resolver.mavenLocal,
  DefaultMavenRepository,
  Resolver.typesafeRepo("releases"),
  Resolver.typesafeRepo("snapshots"),
  Resolver.typesafeIvyRepo("snapshots"),
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  "jcenter" at "http://jcenter.bintray.com",
  Resolver.defaultLocal)


libraryDependencies ++= Seq(
  aar("org.macroid" %% "macroid" % "2.0.0-M4"),
  aar("org.macroid" %% "macroid-viewable" % "2.0.0-M4"),
  aar("com.fortysevendeg" %% "macroid-extras" % "0.1.1"),
  "com.android.support" % "support-v4" % "22.1.1",
  "com.android.support" % "appcompat-v7" % "22.1.1",
  "net.sf.proguard" % "proguard-base" % "5.1",
  "com.google.android.gms" % "play-services-drive" % "7.3.0",
  "net.virtual-void" %%  "json-lenses" % "0.6.0"
)


proguardOptions in Android ++= Seq(
  "-ignorewarnings",
  "-keep class scala.Dynamic",
  "-keep class scala.math.Numeric",
  "-keep class scala.concurrent.ExecutionContext",
  "-keep class macroid.*",
  "-keep class macroid.**",
  "-keep class scala.collection.JavaConverters"
)
