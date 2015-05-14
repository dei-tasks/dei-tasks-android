package com.mindmup.android.tasks

import android.app._
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import android.view._
import android.widget._
import macroid._
import macroid.contrib._
import macroid.FullDsl._
import macroid.IdGeneration
import macroid.viewable._
import macroid.contrib.LpTweaks._
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.drive.Drive
import com.google.android.gms.drive.DriveContents
import com.google.android.gms.drive.DriveFile
import com.google.android.gms.drive.DriveId
import com.google.android.gms.drive.MetadataChangeSet
import com.google.android.gms.drive.OpenFileActivityBuilder
import com.google.android.gms.drive.DriveApi.DriveIdResult
import com.google.android.gms.drive.DriveApi.DriveContentsResult
import com.google.android.gms.drive.DriveApi.MetadataBufferResult
import com.google.android.gms.drive.query.Filters
import com.google.android.gms.drive.query.Query
import com.google.android.gms.drive.query.SearchableField
import com.google.android.gms.drive.Metadata
import com.google.android.gms.drive.widget.DataBufferAdapter
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import android.support.v4.app.FragmentActivity
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app._
import android.view.ViewGroup.LayoutParams._
import android.view.Gravity
import android.app.ActionBar._
import android.preference.PreferenceFragment
import android.graphics.Color

object OurTweaks {
  def greeting(greeting: String)(implicit appCtx: AppContext) =
    TextTweaks.large +
    text(greeting)

  def orient(implicit appCtx: AppContext) =
    landscape ? horizontal | vertical
}

class LoginFragment extends Fragment with Contexts[Fragment] {
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = getUi {
    l[LinearLayout](
      w[TextView] <~ text("Fragment demo")
    )
  }

}

class MainActivity extends AppCompatActivity with Contexts[FragmentActivity]
  with ConnectionCallbacks with IdGeneration {
  private val TAG = "MindmupTasks"

  private val REQUEST_CODE_OPENER = 1
  private val REQUEST_CODE_CREATOR = 2
  var greeting = slot[TextView]
  // -- in the activity:

  var navSlot = slot[ListView]
  var drawerSlot = slot[DrawerLayout]


/*  override def onPostCreate(savedInstanceState: Bundle) {
    super.onPostCreate(savedInstanceState)
    drawerToggle.syncState()
    drawerToggle.setDrawerIndicatorEnabled(true)
  }
*/
  def println(s: String): Unit = Log.i(TAG, s)
  lazy val googleApiClient: GoogleApiClient = {
    val connectionFailedListener: GoogleApiClient.OnConnectionFailedListener =
      (result: ConnectionResult) => {
      Log.i(TAG, "GoogleApiClient connection failed: " + result.toString)
      if (!result.hasResolution()) {
        GooglePlayServicesUtil.getErrorDialog(result.getErrorCode, this, 0)
          .show()
        return
      }
    }
    new GoogleApiClient.Builder(this)
    .enableAutoManage(this, 0 /* clientId */, connectionFailedListener)
    .addApi(Drive.API)
    .addScope(Drive.SCOPE_FILE)
    .addConnectionCallbacks(this)
    .build()
  }

  class PrefsFragment extends PreferenceFragment {
    override def onCreate(savedInstanceState: Bundle) = {
      super.onCreate(savedInstanceState)
      addPreferencesFromResource(R.xml.preferences)
    }
  }

  override def onCreate(savedInstanceState: Bundle) = {
    import Implicits._
    super.onCreate(savedInstanceState)
    import android.support.v7.widget.Toolbar
    var toolbar = slot[Toolbar]

    val items = List("bla", "foo", "bar")
    lazy val drawer = l[DrawerLayout](
      f[PrefsFragment].framed(Id.something, Tag.elss) <~ matchParent,
      w[ListView] <~ matchParent <~ ListTweaks.noDivider <~ items.listAdapterTweak
        <~ Tweak[ListView] { lv =>
          val p = new DrawerLayout.LayoutParams(240 dp, android.view.ViewGroup.LayoutParams.MATCH_PARENT, GravityCompat.START)
          lv.setLayoutParams(p)
          lv.setAlpha(1)
        }
    ) <~ matchParent <~ Tweak[DrawerLayout] { d =>
      //d.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)

      val actionBarDrawerToggle = new ActionBarDrawerToggle(MainActivity.this, d, toolbar.get, R.string.app_name, R.string.app_name)
      d.setDrawerListener(actionBarDrawerToggle)
    }

    val view = l[LinearLayout](
      w[Toolbar] <~ matchWidth <~ wire(toolbar) <~ Tweak[Toolbar] { t =>
        t.setPopupTheme(R.style.AppTheme)
        setSupportActionBar(t)
        getSupportActionBar.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar.setHomeButtonEnabled(true)
      },
      drawer
    ) <~ vertical <~ matchParent

    setContentView(getUi(view))
  }

  def onCreate2(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    var button = slot[Button]
    // -- in onCreate:

    // ListView tweaks
    def checkItem(pos: Int) = Tweak[ListView](_.setItemChecked(pos, true))
    val singleNoDivider = Tweak[ListView] { lv ⇒
      lv.setDividerHeight(0)
      lv.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE)
    }

    // Drawer tweaks
    val closeDrawers = Tweak[DrawerLayout](_.closeDrawers())
    //val drawerShadow = Tweak[DrawerLayout](_.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START))

    // navigation
    val nav = w[ListView] <~
      layoutParams[DrawerLayout](240 dp, MATCH_PARENT, Gravity.START) <~
      BgTweaks.color(Color.parseColor("#FF363636")) <~
      singleNoDivider <~
      new MindmupFileSelection(findMindmups).fileListTweak <~
      wire(navSlot)

    navSlot <~ new MindmupFileSelection(findMindmups).fileListTweak
    // f[...].framed returns a FrameLayout
    // there is probably no point in wrapping
    // it into an additional LinearLayout
    val view = f[PrefsFragment].framed(Id.provinceOverview, Tag.provinceOverview) <~
      layoutParams[DrawerLayout](MATCH_PARENT, WRAP_CONTENT)

    // the drawer
    val drawer = l[DrawerLayout](
      w[TextView] <~ text("Main content"),
      view,
      nav
    ) <~
      //drawerShadow <~
      wire(drawerSlot)

    setContentView(getUi(drawer))
/*    setContentView {
      getUi {
        l[LinearLayout](
          w[TextView] <~ text("Before"),
          f[MindmupFileSelection](findMindmups).framed(Id.mindmupFiles, Tag.mindmupFilesTag),
          w[TextView] <~ text("After")
        ) <~ vertical
      }
    }
    */
  }



  override def onStart: Unit = {
    googleApiClient
    super.onStart();
  }

  def findMindmups: Future[Seq[Metadata]] = {
    val query = new Query.Builder()
      .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/json"))
      .build();
    Future {
      println(s"Starting to search files, API Client is connected? ${googleApiClient.isConnected}")
      googleApiClient.blockingConnect
      println(s"Blocking connected, API Client is connected? ${googleApiClient.isConnected}")
      val res = Drive.DriveApi.query(googleApiClient, query).await().getMetadataBuffer.iterator.asScala.toList
      println(s"Finished searching files:\n${res}")
      res
    }
  }

  override def onConnected(connectionHint: Bundle) {
    /*
    val intentSender = Drive.DriveApi.newOpenFileActivityBuilder().setMimeType(Array("application/json"))
      .build(googleApiClient)
    try {
      startIntentSenderForResult(intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0)
    } catch {
      case e: SendIntentException => Log.w(TAG, "Unable to send intent", e)
    }
    */

  }

  override def onConnectionSuspended(cause: Int) {
    Log.i(TAG, "GoogleApiClient connection suspended")
  }

  def loadFile(driveId: DriveId): String = {
    googleApiClient.blockingConnect()
    val file = Drive.DriveApi.getFile(googleApiClient, driveId);
    val driveContentsResult =
      file.open(googleApiClient, DriveFile.MODE_READ_ONLY, null).await();
    Log.i(TAG, s"Drive content results ${driveContentsResult} ${driveContentsResult.getStatus()}")
    if (!driveContentsResult.getStatus().isSuccess()) {
        return null;
    }
    val driveContents = driveContentsResult.getDriveContents()
    val json = scala.io.Source.fromInputStream(driveContents.getInputStream()).mkString
    Log.i(TAG, s"Read the following:\n$json")
    Ui(showMessage(json))

    json
  }

  /**
   * Shows a toast message.
   */
  def showMessage(message: String): Unit = {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
  }
}
