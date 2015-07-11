package com.github.dei_tasks

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
import com.google.android.gms.drive.events.{ChangeListener, ChangeEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.collection.JavaConverters._

import rapture.json._
import jsonBackends.jawn._

class MindmupModel(googleApiClient: GoogleApiClient) {
  import MindmupModel._
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
  val fileChangeListener: ChangeListener = { event: ChangeEvent =>
    println(s"Got notified of change event $event for file ${event.getDriveId}")
    //lastKnownChange() = System.currentTimeMillis
  }
  def loadFile(driveId: DriveId): String = {
    googleApiClient.blockingConnect()
    val file = Drive.DriveApi.getFile(googleApiClient, driveId);
    val driveContentsResult =
      file.open(googleApiClient, DriveFile.MODE_READ_ONLY, null).await();
    file.addChangeListener(googleApiClient, fileChangeListener)
    println(s"Drive content results ${driveContentsResult} ${driveContentsResult.getStatus()}")
    if (!driveContentsResult.getStatus().isSuccess()) {
        return null;
    }
    val driveContents = driveContentsResult.getDriveContents()
    scala.io.Source.fromInputStream(driveContents.getInputStream()).mkString
  }

  def saveFile(driveId: DriveId, content: MindmupModel.JSON): Boolean = {
    googleApiClient.blockingConnect()
    val file = Drive.DriveApi.getFile(googleApiClient, driveId);
    val driveContentsResult =
      file.open(googleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
    println(s"Drive content results ${driveContentsResult} ${driveContentsResult.getStatus()}")
    if (!driveContentsResult.getStatus().isSuccess()) {
      println(s"Problem writing to file $driveId")
    }
    val driveContents = driveContentsResult.getDriveContents()
    import formatters.humanReadable._
    val out = Json.format(content)
    println(s"Saving to $driveId the following content:\n$out")
    driveContents.getOutputStream().write(out.getBytes())
    val saveStatus = driveContents.commit(googleApiClient, null).await()
    println(s"Save status is $saveStatus)")
    saveStatus.isSuccess
  }
  def retrieveTasks(currentMindmupIds: Set[String]) = {
    println(s"Loading tasks from these Mindmups: $currentMindmupIds")
    val contents = currentMindmupIds.toList.map(loadMindmup)
    currentMindmupIds.zip(contents.map(parseMindmup)).toMap
  }
}

object MindmupModel {
  type JSON = JsonBuffer
  def queryInterpreter[T: TreeLike]: CharSequence => List[T] => Boolean = { query =>
    val ql = query.toString.toLowerCase.split(" ")
    val tl = implicitly[TreeLike[T]]
    val filter = { ml: List[T] =>
      val titlePath = ml.map(tl.title(_)).mkString(" ").toLowerCase
      ql.forall(titlePath.contains)
    }
    filter
  }
  def parseMindmup(json: String): MindmupModel.JSON = {
    Json.parse(json).as[MindmupModel.JSON]
  }

}
