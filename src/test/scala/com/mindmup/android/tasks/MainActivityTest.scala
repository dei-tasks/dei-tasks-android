package com.mindmup.android.tasks

import android.app.SearchManager
import android.content.{Context, Intent}
import android.util.FloatMath
import com.google.android.gms.common.ConnectionResult
import com.mindmup.android.tasks.TreeLike._
import org.robolectric.{RuntimeEnvironment, Robolectric}
import org.robolectric.annotation.Config
import org.robolectric.shadows.{ShadowPreferenceManager, ShadowLog}
import org.robolectric.Shadows._
import org.robolectric.internal.ShadowExtractor
import org.robolectric.util.ReflectionHelpers
//import org.robolectric.shadows.gms.ShadowGooglePlayServicesUtil
import com.google.android.gms.drive.{DriveId, Drive}
import org.scalatest.{Matchers, FeatureSpec, RobolectricSuite}
import scala.collection.JavaConverters._
import scala.io.Source
import MindmupJsonTree._

@Config(sdk = Array(21), manifest = "src/main/AndroidManifest.xml")
class MainActivityTest extends FeatureSpec with Matchers with RobolectricSuite {
  override def robolectricShadows = Seq(classOf[ShadowDriveApi])

  ShadowLog.stream = System.out

  val mindmupJsonString = Source.fromURL(getClass.getResource("/mindmup_tasks.mup")).getLines.mkString("\n")
  val mindmupJson = MindmupModel.parseMindmup(mindmupJsonString)
  def setupSettingsAndDrive(): Unit = {
    val driveIdString = "DriveId:CAESHDBCMmh0cDdjdkdMdVZlRVJqWmpRd1QyaDRYMk0YhgIgyMuOlLVTKAA="
    val driveId = DriveId.decodeFromString(driveIdString)
    val shadowDriveApi = new ShadowDriveApi(Map(driveId -> mindmupJsonString))
    ReflectionHelpers.setStaticField(classOf[Drive], "DriveApi", shadowDriveApi)
    val sharedPreferences = ShadowPreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application.getApplicationContext())
    sharedPreferences.edit().putStringSet("selected_mindmups", Set(driveIdString).asJava).commit()
  }

  feature("Task list search") {

    scenario("returns only items that match filter") {
      setupSettingsAndDrive()
      val activity = Robolectric.setupActivity(classOf[MainActivity])
      activity.getQuery should equal("")
      val tasksInFile = allDescendantsWithPaths(mindmupJson)
      shadowOf(activity.taskListFragment.taskListView).populateItems()
      activity.displayedTasks.size should equal(tasksInFile.size)
      activity.setQuery("search")
      shadowOf(activity.taskListFragment.taskListView).populateItems()
      activity.displayedTasks.size should equal(tasksInFile.count(_.exists(_.title.contains("search"))))

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
