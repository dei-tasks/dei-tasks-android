name := """mindmup-tasks"""

version := "1.0.0"

scalaVersion := "2.11.7"

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
  Resolver.bintrayRepo("nightscape", "maven"),
  Resolver.bintrayRepo("amulyakhare", "maven"),
  "RoboTest releases" at "https://raw.github.com/zbsz/mvn-repo/master/releases/",
  Resolver.defaultLocal)


libraryDependencies ++= Seq(
  aar("org.macroid" %% "macroid" % "2.0.0-M4"),
  aar("org.macroid" %% "macroid-viewable" % "2.0.0-M4"),
  aar("com.fortysevendeg" %% "macroid-extras" % "0.1.1"),
  aar("com.github.bmelnychuk" % "atv" % "1.2.4"),
  "com.malinskiy" % "materialicons" % "1.0.1",
  "com.android.support" % "support-v4" % "22.1.1",
  "com.android.support" % "appcompat-v7" % "22.1.1",
  "com.amulyakhare" % "com.amulyakhare.textdrawable" % "1.0.1",
  "net.sf.proguard" % "proguard-base" % "5.2.1",
  "com.google.android.gms" % "play-services-drive" % "7.3.0",
  "com.propensive" %% "rapture-json-jawn" % "1.1.0",
  "com.lihaoyi" %% "scalarx" % "0.2.8",
  "com.softwaremill.scalamacrodebug" %% "macros" % "0.4",
  "com.geteit" %% "robotest" % "0.10" % Test,
  "org.robolectric" % "shadows-play-services" % "3.0-SNAPSHOT" % Test,
  "org.robolectric" % "shadows-support-v4" % "3.0-SNAPSHOT" % Test,
  "org.scalatest" %% "scalatest" % "2.2.5" % Test,
  "org.scalacheck" %% "scalacheck" % "1.12.2" % Test

)


proguardOptions in Android ++= Seq(
  "-ignorewarnings",
  "-keep class scala.Dynamic",
  "-keep class scala.math.Numeric",
  """-keepclassmembers public class scala.util.Try {
    *;
  }""",
  "-keep class scala.concurrent.ExecutionContext",
  "-keep class macroid.*",
  "-keep class macroid.**",
  "-keep class com.malinskiy.materialicons.IconDrawable",

  "-keep class scala.collection.JavaConverters",
  "-keep class com.mindmup.android.tasks.*",
  "-keep class android.support.v4.** { *; }",
  "-keep interface android.support.v4.** { *; }",
  "-keep class android.support.v7.** { *; }",
  "-keep interface android.support.v7.** { *; }",
  "-keep class android.support.v7.widget.SearchView",
  """-keep public class * extends android.support.v7.widget.SearchView {
   public <init>(android.content.Context);
   public <init>(android.content.Context, android.util.AttributeSet);
  }"""
)

dexMaxHeap := "3072m"
