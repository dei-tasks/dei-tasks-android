package com.github.dei_tasks

import android.support.v4.app.Fragment
import android.view._
import android.widget._
import android.os.Bundle
import android.preference._
import android.graphics.Color

import macroid._
import macroid.contrib._
import macroid.FullDsl._
import macroid.viewable._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import com.google.android.gms.drive.Metadata
import rx._
import rx.ops._

class MindmupFileSelection(mindmupFilesFuture: Rx[Seq[Metadata]]) extends PreferenceFragment with Contexts[PreferenceFragment] {
  implicit def fileListable(implicit appCtx: AppContext) =
    Listable[Metadata].tw {
      w[TextView]
    } { meta =>
      text(meta.getTitle)
    }

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.preferences)
  }

  def fileSelectionPimp(preferences: PreferenceScreen, files: Seq[Metadata]) = {
    val multiSelection = preferences.findPreference("selected_mindmups").asInstanceOf[MultiSelectListPreference]
    multiSelection.setEntries(files.map(_.getTitle.asInstanceOf[CharSequence]).toArray)
    multiSelection.setEntryValues(files.map(_.getDriveId.encodeToString.asInstanceOf[CharSequence]).toArray)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    mindmupFilesFuture.foreach { files => fileSelectionPimp(getPreferenceScreen, files)}
  }
}

object MindmupFileSelection {
  def newInstance(mindmupFilesFuture: Rx[Seq[Metadata]]) = new MindmupFileSelection(mindmupFilesFuture)
}
