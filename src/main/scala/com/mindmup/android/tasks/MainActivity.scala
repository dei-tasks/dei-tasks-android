package com.mindmup.android.tasks

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import android.widget._
import macroid._
import macroid.contrib._
import macroid.FullDsl._
import macroid.viewable._
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

object OurTweaks {
  def greeting(greeting: String)(implicit appCtx: AppContext) =
    TextTweaks.large +
    text(greeting) +
    hide

  def orient(implicit appCtx: AppContext) =
    landscape ? horizontal | vertical

  // defines how to view a User in a list
  implicit def userListable(implicit ctx: ActivityContext, appCtx: AppContext) =
    Listable[User].tw {
      // the layout is a TextView
      w[TextView]
    } { user ⇒
      // to display a user, we tweak the layout
      text(user.name) + TextTweaks.size(user.age + 10)
    }
}

// our data type
case class User(name: String, age: Int)

class MainActivity extends Activity with Contexts[Activity] with ConnectionCallbacks {
  private val TAG = "MindmupTasks"

  private val REQUEST_CODE_OPENER = 1
  private val REQUEST_CODE_CAPTURE_IMAGE = 1
  private val REQUEST_CODE_CREATOR = 2
  private val REQUEST_CODE_RESOLUTION = 3
  var greeting = slot[TextView]
  lazy val googleApiClient: GoogleApiClient = {
    new GoogleApiClient.Builder(this).addApi(Drive.API)
    .addScope(Drive.SCOPE_FILE)
    .addConnectionCallbacks(this)
    .addOnConnectionFailedListener((result: ConnectionResult) => {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString)
        if (!result.hasResolution()) {
          GooglePlayServicesUtil.getErrorDialog(result.getErrorCode, this, 0)
            .show()
          return
        }
        try {
          result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION)
        } catch {
          case e: SendIntentException => Log.e(TAG, "Exception while starting resolution activity", e)
        }
      })
    .build()
  }

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)

    val myListView = w[ListView]

    import OurTweaks.userListable
    // now we simply tweak the ListView
    val tweakedList = myListView <~ List(User("Alice", 12), User("Bob", 23)).listAdapterTweak

    setContentView {
      getUi {
        l[LinearLayout](
          tweakedList,
          w[Button] <~
            text("Click me") <~
            On.click {
              greeting <~ text("Here should have been some JSON") <~ show
            },
          w[TextView] <~
            wire(greeting) <~
            OurTweaks.greeting("Hello!")
        ) <~ OurTweaks.orient
      }
    }
  }
  override def onStart: Unit = {
    super.onStart();
    setupGoogleApi()
  }

  def setupGoogleApi(): Unit = {
    googleApiClient.connect()
  }
  protected override def onResume() {
    super.onResume()
    setupGoogleApi()
  }

  protected override def onPause() {
    if (googleApiClient != null) {
      googleApiClient.disconnect()
    }
    super.onPause()
  }





  override def onConnectionSuspended(cause: Int) {
    Log.i(TAG, "GoogleApiClient connection suspended")
  }

  override def onConnected(connectionHint: Bundle) {
    val intentSender = Drive.DriveApi.newOpenFileActivityBuilder().setMimeType(Array("application/json"))
      .build(googleApiClient)
    try {
      startIntentSenderForResult(intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0)
    } catch {
      case e: SendIntentException => Log.w(TAG, "Unable to send intent", e)
    }
  }

  protected override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) = requestCode match {
    case REQUEST_CODE_OPENER =>
      if (resultCode == Activity.RESULT_OK) {
        val driveId = data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID).asInstanceOf[DriveId]
        showMessage("Selected file's ID: " + loadFile(driveId))
      }


    case _ => super.onActivityResult(requestCode, resultCode, data)
  }

  def loadFile(driveId: DriveId): String = {
    Log.i(TAG, s"Google Api connected before? ${googleApiClient.isConnected}")
    googleApiClient.connect()
    Log.i(TAG, s"Google Api connected after? ${googleApiClient.isConnected}")
    val file = Drive.DriveApi.getFile(googleApiClient, driveId);
    val driveContentsResult =
            file.open(googleApiClient, DriveFile.MODE_READ_ONLY, null).await();
    if (!driveContentsResult.getStatus().isSuccess()) {
        return null;
    }
    val driveContents = driveContentsResult.getDriveContents()
    scala.io.Source.fromInputStream(driveContents.getInputStream()).mkString
  }

  /**
   * Shows a toast message.
   */
  def showMessage(message: String): Unit = {
      Toast.makeText(this, message, Toast.LENGTH_LONG).show()
  }
}
