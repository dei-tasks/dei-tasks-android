package com.mindmup.android.tasks

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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.collection.JavaConverters._

class MindmupModel(googleApiClient: GoogleApiClient) {

  val mindmupFilter = Filters.contains(SearchableField.TITLE, ".mup")
  def findMindmups: Seq[Metadata] = {
    println("Finding Mindmups")
    val query = new Query.Builder()
      .addFilter(mindmupFilter)
      .build()
    println("Built query for Mindmups")
    println("Starting to search files")
    println(s"API Client is connected? ${googleApiClient.isConnected}")
    googleApiClient.blockingConnect
    println(s"Blocking connected, API Client is connected? ${googleApiClient.isConnected}")
    val res = Drive.DriveApi.query(googleApiClient, query).await().getMetadataBuffer.iterator.asScala.toList
    println(s"Finished searching files:\n${res}")
    res
  }

  def loadMindmup(encodedId: String) = {
    val driveId = DriveId.decodeFromString(encodedId)
    loadFile(driveId)
  }

  def loadFile(driveId: DriveId): String = {
    googleApiClient.blockingConnect()
    val file = Drive.DriveApi.getFile(googleApiClient, driveId);
    val driveContentsResult =
      file.open(googleApiClient, DriveFile.MODE_READ_ONLY, null).await();
    file.addChangeListener(googleApiClient, { event: ChangeEvent =>
      println(s"Got notified of change event $event for file $file")
      //lastKnownChange() = System.currentTimeMillis
    })
    println(s"Drive content results ${driveContentsResult} ${driveContentsResult.getStatus()}")
    if (!driveContentsResult.getStatus().isSuccess()) {
        return null;
    }
    val driveContents = driveContentsResult.getDriveContents()
    scala.io.Source.fromInputStream(driveContents.getInputStream()).mkString
  }

  def retrieveTasks(currentMindmupIds: Set[String]) = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import org.json4s.JsonDSL._
    println(s"Loading tasks from these Mindmups: $currentMindmupIds")
    val contents = currentMindmupIds.toList.map(loadMindmup)
    val parsed = contents.map(c => parse(c).asInstanceOf[JObject])
    println(s"Successfully parsed ${parsed.size} Mindmups")
    import MindmupJsonTree._
    implicit val formats = DefaultFormats
    val tasks = parsed.flatMap { json =>
      allDescendantsWithPaths(json.extract[Map[String, Any]])
    }
    println(s"Successfully taskified ${tasks.size} nodes")
    tasks
  }
}
