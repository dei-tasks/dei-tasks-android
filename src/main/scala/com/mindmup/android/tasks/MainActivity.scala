package com.mindmup.android.tasks

import android.app._
import android.content.{ Intent, IntentSender, Context, SharedPreferences }
import android.content.IntentSender.SendIntentException
import android.net.Uri
import android.os.Bundle
import android.provider.SearchRecentSuggestions
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
import com.google.android.gms.drive.events.ChangeEvent
import com.google.android.gms.drive.widget.DataBufferAdapter
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import android.support.v4.app.FragmentActivity
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout

import android.support.v4.view.MenuItemCompat
import android.support.v4.view.MenuItemCompat.OnActionExpandListener
import android.support.v7.app._
import android.view.ViewGroup.LayoutParams._
import android.view.Gravity
import android.app.ActionBar._
import android.preference.PreferenceFragment
import android.graphics.Color
import com.softwaremill.debug.DebugConsole._
import macroid.util.Effector
import rx._
import rx.ops._


import rapture.json.{JsonBuffer, Json}

import Implicits._
import MindmupJsonTree._




object OurTweaks {
  def greeting(greeting: String)(implicit appCtx: AppContext) =
    TextTweaks.large +
    text(greeting)

  def orient(implicit appCtx: AppContext) =
    landscape ? horizontal | vertical
}

class MainActivity extends AppCompatActivity with Contexts[FragmentActivity]
  with ConnectionCallbacks with IdGeneration with RxSupport with SharedPreferences.OnSharedPreferenceChangeListener {
  
  type TaskType = JsonBuffer
  private val TAG = "MindmupTasks"

  private val REQUEST_CODE_OPENER = 1
  private val REQUEST_CODE_CREATOR = 2
  var greeting = slot[TextView]

  var navSlot = slot[ListView]
  var drawerSlot = slot[DrawerLayout]
  val currentMindmupIds = Var[Set[String]](Set.empty)
  val lastKnownChange = Var[Long](System.currentTimeMillis)
  private val googleApiClient = Promise[GoogleApiClient]()
  private val mindmupModel = googleApiClient.future.map(new MindmupModel(_))
  val currentFilesWithJson = Rx {
    val mmIds = currentMindmupIds()
    val lastChange = lastKnownChange()
    println(s"Retrieving tasks for $mmIds, last known change $lastChange")
    mindmupModel.map(_.retrieveTasks(mmIds))
  }.async(Map.empty)


  val selectableMindmups = Var[Seq[Metadata]](Seq.empty)

/*  override def onPostCreate(savedInstanceState: Bundle) {
    super.onPostCreate(savedInstanceState)
    drawerToggle.syncState()
    drawerToggle.setDrawerIndicatorEnabled(true)
  }
*/

  private def createGoogleApiClientOnlyWhenInOnStart = {
    val connectionFailedListener: GoogleApiClient.OnConnectionFailedListener =
      (result: ConnectionResult) => {
      println(s"GoogleApiClient connection failed: $result")
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

  var todos = slot[ListView]

  def sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
  lazy val suggestionsCursorAdapter = {
    val suggestionsUri = Uri.parse(s"content://${RecentSearchesSuggestionProvider.AUTHORITY}/search_suggest_query?limit=50")
    val selectionClause = " ?"
    val selectionArgs = Array("")
    val cursor = getContentResolver.query(suggestionsUri, null, selectionClause, selectionArgs, null)
    new SimpleCursorAdapter(
      getApplicationContext(),
      android.R.layout.simple_list_item_activated_1,
      cursor,
      Array("suggest_text_1"),
      Array(android.R.id.text1),
      0)

  }
  def refreshAvailableMindmups(): Unit = {
    mindmupModel.map(_.findMindmups).foreach { mms => selectableMindmups() = mms }
  }
  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    import android.support.v7.widget.Toolbar
    var toolbar = slot[Toolbar]
    refreshAvailableMindmups()
    currentMindmupIds() = sharedPreferences.getStringSet("selected_mindmups", java.util.Collections.emptySet[String]).asScala.toSet
    val taskListFragment = f[TaskListFragment[TaskType, TextView]](
      currentFilesWithJson,
      MindmupModel.queryInterpreter[TaskType],
      taskListable[TaskType],
      MindmupJsonTree.mindmupJsonTreeLike
      ).framed(Id.taskList, Tag.taskList)
    val drawer = l[DrawerLayout](
      l[LinearLayout](
        taskListFragment
      ) <~ matchParent,
      l[LinearLayout](
        f[MindmupFileSelection](selectableMindmups).framed(Id.mindmupFiles, Tag.mindmupFilesTag),
        w[Button] <~ text("Add additional files") <~ On.click {
          openMindmupSelectionDialog
          refreshAvailableMindmups()
          Ui(true)
        },
        w[Button] <~ text("Clear recent searches") <~
          On.click {
            val suggestions = new SearchRecentSuggestions(this,
              RecentSearchesSuggestionProvider.AUTHORITY, RecentSearchesSuggestionProvider.MODE)
            suggestions.clearHistory()
            refreshRecentQueries()
            Ui(true)
          },
        w[ListView] <~ Tweak[ListView] { listView =>
          listView.setClickable(true)
          listView.setAdapter(suggestionsCursorAdapter)
        } <~
          FuncOn.itemClick[ListView] { (view: AdapterView[_], _: View, index: Int, _: Long) =>
            val cursor = suggestionsCursorAdapter.getCursor
            cursor.moveToPosition(index)
            val query = cursor.getString(cursor.getColumnIndex("suggest_text_1"))
            val searchIntent = new Intent(MainActivity.this, classOf[MainActivity])
            searchIntent.setAction(Intent.ACTION_SEARCH)
            searchIntent.putExtra(SearchManager.QUERY, query);
            startActivity(searchIntent);
            Ui(true)
          }
      ) <~ vertical
        <~ matchParent
        <~ BgTweaks.color(Color.parseColor("#FF164b64"))
        <~ Tweak[LinearLayout] { lv =>
          val p = new DrawerLayout.LayoutParams(240 dp, android.view.ViewGroup.LayoutParams.MATCH_PARENT, GravityCompat.START)
          lv.setLayoutParams(p)
          lv.setAlpha(255)
        }
    ) <~ matchParent <~ Tweak[DrawerLayout] { d =>
      d.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)

      val actionBarDrawerToggle = new ActionBarDrawerToggle(MainActivity.this, d, toolbar.get, R.string.app_name, R.string.app_name)
      d.setDrawerListener(actionBarDrawerToggle)
    }

    val view = l[LinearLayout](
      w[Toolbar] <~ matchWidth <~ wire(toolbar) <~ Tweak[Toolbar] { t =>
        t.setPopupTheme(R.style.AppTheme)
        setSupportActionBar(t)
        getSupportActionBar.setHomeAsUpIndicator(R.drawable.ic_drawer)
        getSupportActionBar.setDisplayHomeAsUpEnabled(true)
        getSupportActionBar.setHomeButtonEnabled(true)
      },
      drawer
    ) <~ vertical <~ matchParent
    setContentView(getUi(view))
  }

  protected override def onResume() {
    super.onResume()
    sharedPreferences.registerOnSharedPreferenceChangeListener(this)
  }

  protected override def onPause() {
    super.onPause()
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
  }
  override def onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    if (key.equals("selected_mindmups")) {
      println(s"Changed selected mindmups: ${sharedPreferences.getAll}")
      currentMindmupIds() = sharedPreferences.getStringSet("selected_mindmups", java.util.Collections.emptySet[String]).asScala.toSet
    }
  }
  override def onNewIntent(intent: Intent): Unit = {
    handleIntent(intent)
  }

  def refreshRecentQueries(): Unit = {
    suggestionsCursorAdapter.getCursor.requery()
    suggestionsCursorAdapter.notifyDataSetChanged()
  }
  def handleIntent(intent: Intent): Unit = {
    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      val query = intent.getStringExtra(SearchManager.QUERY)
      val suggestions = new SearchRecentSuggestions(this,
                RecentSearchesSuggestionProvider.AUTHORITY, RecentSearchesSuggestionProvider.MODE)
      suggestions.saveRecentQuery(query, null)
      refreshRecentQueries()
      taskListFragment.taskFilterString() = query
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater();
    inflater.inflate(R.menu.options_menu, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = item.getItemId match {
    case R.id.save =>
      println("Ok, saving")
      val successesFuture = Future.sequence(currentFilesWithJson().map { case(driveIdString, json) =>
        val driveId = DriveId.decodeFromString(driveIdString)
        mindmupModel.map { mm =>
          mm.saveFile(driveId, json)
        }
      })
      successesFuture.map { successes =>
        if(successes.exists(!_)) {
          runUi {
            toast("Not all files could be saved successfully") <~ fry
          }
        }
      }
      true
    case _ => super.onOptionsItemSelected(item)
  }

  def taskListFragment = getUi(this.findFrag[TaskListFragment[TaskType, TextView]](Tag.taskList)).get
  var itemSelectionObserver: Obs = null
  var selectedItem: Rx[Option[List[Map[String, Any]]]] = Var(None)
  override def onStart: Unit = {
    if(!googleApiClient.isCompleted)
      googleApiClient success createGoogleApiClientOnlyWhenInOnStart
    super.onStart()
    handleIntent(getIntent())
    itemSelectionObserver = Obs(taskListFragment.itemSelections, skipInitial=true){
      val selectedItem = taskListFragment.itemSelections()
      selectedItem.foreach { task =>
        val taskList = this.find[FrameLayout](Id.taskList)
        val manager = getSupportFragmentManager
        val builder = f[TaskDetailFragment[TaskType]](task, MindmupJsonTree.mindmupJsonTreeLike)
        val frag = builder.factory.get
        val stateId = manager.beginTransaction().replace(Id.taskList, frag, null).addToBackStack("Details").commit()
      }
    }
  }


  def openMindmupSelectionDialog: Unit = {
    val openIntentFuture = googleApiClient.future.map { gapi =>
      gapi.blockingConnect()
      val intentSender = Drive.DriveApi.newOpenFileActivityBuilder()
        //.setSelectionFilter(mindmupFilter)
        .setMimeType(Array("application/json"))
        .build(gapi)
      try {
        startIntentSenderForResult(intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0)
      } catch {
        case e: SendIntentException => Log.w(TAG, "Unable to send intent", e)
      }
    }
    openIntentFuture.onFailure {
      case f => println(s"Something went wrong while openening dialog for selecting files: $f")
    }
  }

  override def onConnected(connectionHint: Bundle) {
  }

  override def onConnectionSuspended(cause: Int) {
    println("GoogleApiClient connection suspended")
  }
}
