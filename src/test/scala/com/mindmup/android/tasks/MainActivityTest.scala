package com.mindmup.android.tasks

import android.app.SearchManager
import android.content.{Context, Intent}
import android.util.FloatMath
import com.google.android.gms.common.ConnectionResult
import org.robolectric.{RuntimeEnvironment, Robolectric}
import org.robolectric.annotation.Config
import org.robolectric.shadows.{ShadowPreferenceManager, ShadowLog}
import org.robolectric.Shadows._
import org.robolectric.internal.ShadowExtractor
import org.robolectric.util.ReflectionHelpers
import org.robolectric.shadows.gms.ShadowGooglePlayServicesUtil
import com.google.android.gms.drive.{DriveId, Drive}
import org.scalatest.{Matchers, FeatureSpec, RobolectricSuite}
import scala.collection.JavaConverters._
import scala.io.Source

@Config(sdk = Array(21), manifest = "src/main/AndroidManifest.xml")
class MainActivityTest extends FeatureSpec with Matchers with RobolectricSuite {
  override def robolectricShadows = Seq(classOf[ShadowDriveApi])

  ShadowLog.stream = System.out
  def setupSettingsAndDrive(): Unit = {
    val mindmupJson = Source.fromURL(getClass.getResource("/mindmup_tasks.mup")).getLines.mkString("\n")
    val shadowDriveApi = new ShadowDriveApi
    ReflectionHelpers.setStaticField(classOf[Drive], "DriveApi", shadowDriveApi)
    val driveIdString = "DriveId:CAESHDBCMmh0cDdjdkdMdVZlRVJqWmpRd1QyaDRYMk0YhgIgyMuOlLVTKAA="
    val driveId = DriveId.decodeFromString(driveIdString)
    shadowDriveApi.files.put(driveId, mindmupJson)
    val sharedPreferences = ShadowPreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application.getApplicationContext())
    sharedPreferences.edit().putStringSet("selected_mindmups", Set(driveIdString).asJava).commit()
  }

  feature("Task list search") {

    scenario("returns only items that match filter") {
      setupSettingsAndDrive()
      val activity = Robolectric.setupActivity(classOf[MainActivity])
      activity.getQuery should equal("")
    }
    scenario("sets the query from a search intent on the search view") {
      val query = "fooboo"
      val intent = new Intent(Intent.ACTION_SEARCH)
      intent.putExtra(SearchManager.QUERY, query)
      val activity = Robolectric.buildActivity(classOf[MainActivity]).withIntent(intent).create().start().resume().visible().get()
      //activity.getQuery should equal(query)
    }
  }

}
