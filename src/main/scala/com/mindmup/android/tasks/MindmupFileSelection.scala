package com.mindmup.android.tasks

import android.support.v4.app.Fragment
import android.view._
import android.widget._
import android.os.Bundle

import macroid._
import macroid.contrib._
import macroid.FullDsl._
import macroid.viewable._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

import com.google.android.gms.drive.Metadata

class MindmupFileSelection(mindmupFilesFuture: Future[Seq[Metadata]]) extends Fragment with Contexts[Fragment] {
  implicit def fileListable(implicit appCtx: AppContext) =
    Listable[Metadata].tw {
      w[TextView]
    } { meta =>
      text(meta.getTitle)
    }

  implicit def stringListable(implicit appCtx: AppContext) =
    Listable[String].tw {
      w[TextView]
    } { string =>
      text(string)
    }

  def fileListTweak = if (false) Future {
    Thread.sleep(3000)
    List("Go", "yeah", "go").listAdapterTweak
  } else
  mindmupFilesFuture.map { mindmupFiles =>
    mindmupFiles.listAdapterTweak + FuncOn.itemClick[ListView] { (parent: AdapterView[_], view: View, position: Int, id: Long) =>
      val selected = mindmupFiles(position)
      println(s"You clicked ${selected.getTitle} (${selected.getDriveId}) $parent $view $position $id")
      Ui(true)
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = getUi {
    l[LinearLayout](
      w[TextView] <~ text("This should show immediately"),
      w[ListView] <~ fileListTweak,
      w[TextView] <~ text("This I'm not sure...")
    ) <~ vertical
  }
}

object MindmupFileSelection {
  def newInstance(mindmupFilesFuture: Future[Seq[Metadata]]) = new MindmupFileSelection(mindmupFilesFuture)
}
