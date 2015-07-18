package com.github.dei_tasks

import java.io.{ByteArrayInputStream, OutputStream, InputStream}
import java.nio.charset.StandardCharsets
import java.util
import java.util.concurrent.TimeUnit

import android.os.ParcelFileDescriptor
import com.google.android.gms.common.api.PendingResult.BatchCallback
import com.google.android.gms.common.api.{ResultCallback, GoogleApiClient, PendingResult, Status}
import com.google.android.gms.drive.DriveFile.DownloadProgressListener
import com.google.android.gms.drive.DriveResource.MetadataResult
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import com.google.android.gms.drive._
import com.google.android.gms.drive.DriveApi._
import com.google.android.gms.drive.query._
import com.google.android.gms.drive.events.{ChangeListener, ChangeEvent}
import scala.collection.mutable

@Implements(classOf[com.google.android.gms.drive.internal.zzq])
class ShadowDriveApi(files: Map[DriveId, String]) extends DriveApi {
  @Implementation
  def getFile(googleApiClient: GoogleApiClient, driveId: DriveId): DriveFile =
    new ShadowDriveFile(driveId, files(driveId))
  def query(var1: GoogleApiClient, var2: Query): PendingResult[DriveApi.MetadataBufferResult] = ???
  def newDriveContents(var1: GoogleApiClient): PendingResult[DriveApi.DriveContentsResult] = ???
  def fetchDriveId(var1: GoogleApiClient, var2: String): PendingResult[DriveApi.DriveIdResult] = ???
  def getRootFolder(var1: GoogleApiClient): DriveFolder = ???
  def getAppFolder(var1: GoogleApiClient): DriveFolder = ???
  def getFolder(var1: GoogleApiClient, var2: DriveId): DriveFolder = ???
  def newOpenFileActivityBuilder: OpenFileActivityBuilder = ???
  def newCreateFileActivityBuilder: CreateFileActivityBuilder = ???
  def requestSync(var1: GoogleApiClient): PendingResult[Status] = ???
  def cancelPendingActions(var1: GoogleApiClient, var2: java.util.List[String]): PendingResult[Status] = ???
}

class ShadowDriveFile(driveId: DriveId, content: String) extends DriveFile {
  override def open(googleApiClient: GoogleApiClient, i: Int, downloadProgressListener: DownloadProgressListener): PendingResult[DriveContentsResult] =
    new ShadowPendingResult[DriveContentsResult](new DriveContentsResult {
      override def getDriveContents: DriveContents = new ShadowDriveContents(driveId = driveId, content)
      override def getStatus: Status = new Status(0)
    })
  override def addChangeListener(googleApiClient: GoogleApiClient, changeListener: ChangeListener): PendingResult[Status] = null
  override def getDriveId: DriveId = driveId
  override def getMetadata(googleApiClient: GoogleApiClient): PendingResult[MetadataResult] = ???
  override def untrash(googleApiClient: GoogleApiClient): PendingResult[Status] = ???
  override def removeChangeListener(googleApiClient: GoogleApiClient, changeListener: ChangeListener): PendingResult[Status] = ???
  override def removeChangeSubscription(googleApiClient: GoogleApiClient): PendingResult[Status] = ???
  override def setParents(googleApiClient: GoogleApiClient, set: util.Set[DriveId]): PendingResult[Status] = ???
  override def listParents(googleApiClient: GoogleApiClient): PendingResult[MetadataBufferResult] = ???
  override def updateMetadata(googleApiClient: GoogleApiClient, metadataChangeSet: MetadataChangeSet): PendingResult[MetadataResult] = ???
  override def trash(googleApiClient: GoogleApiClient): PendingResult[Status] = ???
  override def addChangeSubscription(googleApiClient: GoogleApiClient): PendingResult[Status] = ???
  override def delete(googleApiClient: GoogleApiClient): PendingResult[Status] = ???
}

class ShadowPendingResult[R <: com.google.android.gms.common.api.Result](r: R) extends PendingResult[R] {
  override def await(): R = r
  override def await(l: Long, timeUnit: TimeUnit): R = r
  override def addBatchCallback(batchCallback: BatchCallback): Unit = ???
  override def cancel(): Unit = ???
  override def isCanceled: Boolean = ???
  override def setResultCallback(resultCallback: ResultCallback[R]): Unit = ???
  override def setResultCallback(resultCallback: ResultCallback[R], l: Long, timeUnit: TimeUnit): Unit = ???
}

class ShadowDriveContents(driveId: DriveId, content: String) extends DriveContents {
  override def zzpe(): com.google.android.gms.drive.Contents = ???
  override def zzpf(): Unit = ???
  override def zzpg(): Boolean = ???
  override def reopenForWrite(googleApiClient: GoogleApiClient): PendingResult[DriveContentsResult] = ???
  override def getDriveId: DriveId = driveId
  override def getMode: Int = ???
  override def getParcelFileDescriptor: ParcelFileDescriptor = ???
  override def discard(googleApiClient: GoogleApiClient): Unit = ???
  override def getOutputStream: OutputStream = ???
  override def commit(googleApiClient: GoogleApiClient, metadataChangeSet: MetadataChangeSet): PendingResult[Status] = ???
  override def commit(googleApiClient: GoogleApiClient, metadataChangeSet: MetadataChangeSet, executionOptions: ExecutionOptions): PendingResult[Status] = ???
  override def getInputStream: InputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
}