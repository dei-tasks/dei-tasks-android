package com.mindmup.android.tasks

import android.app._
import android.content.{ Intent, IntentSender, Context, SharedPreferences }
import android.content.IntentSender.SendIntentException
import android.graphics.Color
import android.os.Bundle
import android.util.{TypedValue, Log}
import android.view._
import android.view.inputmethod.EditorInfo
import android.widget._
import com.amulyakhare.textdrawable.TextDrawable
import com.google.android.gms.drive.DriveId
import com.malinskiy.materialicons.IconDrawable
import com.malinskiy.materialicons.Iconify.IconValue
import com.mindmup.android.tasks.Implicits._
import macroid._
import macroid.contrib._
import macroid.FullDsl._
import macroid.IdGeneration
import macroid.viewable._
import macroid.contrib.LpTweaks._

import android.support.v4.app.Fragment
import rapture.json.JsonBuffer

import rx._
import rx.ops._

import scala.concurrent.Future
import TreeLike._

class TaskDetailFragment[T: TreeLike](task: List[T]) extends Fragment with Contexts[Fragment] with RxSupport with TaskUi[T] {
  import TreeLike._
  def treeLike = implicitly[TreeLike[T]]
  var menu: Menu = _
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    setHasOptionsMenu(true)
    import TextTweaks._
    import CustomTweaks._
    getUi {
      l[LinearLayout](
        w[TextView] <~ text("Title") <~ bold,
        w[EditText] <~
          text(task.last.title) <~
          selectableText <~
          imeOption(EditorInfo.IME_ACTION_DONE) <~
          FuncOn.editorAction[EditText] { (v: TextView, actionId: Int, event: KeyEvent) =>
            implicitly[TreeLike[T]].setTitle(task.last, v.getText.toString)
            Ui(true)
          },
        w[TextView] <~ text("Attachment") <~ bold,
        w[TextView] <~ text(task.last.attachment.getOrElse("")) <~ selectableText
      ) <~ vertical
    }
  }

  def actionBarHeight = TypedValue.applyDimension(1, 24.0f, this.getActivity.getResources().getDisplayMetrics()).toInt
  def progressIcon(letter: String, color: Int) = {
    TextDrawable.builder()
      .beginConfig()
      .width(actionBarHeight)  // width in px
      .height(actionBarHeight) // height in px
      .endConfig()
      .buildRect(letter, color)
  }
  lazy val PROGRESS_ICONS = Map(
    Done -> progressIcon("D", Color.GREEN),
    InProgress -> progressIcon("P", Color.YELLOW),
    NotStarted -> progressIcon("N", Color.GRAY)
  )
  val PROGRESS_STATES = Map(
    R.id.mark_done -> Done,
    R.id.mark_in_progress -> InProgress,
    R.id.mark_not_started -> NotStarted
  )
  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater): Unit = {
    inflater.inflate(R.menu.progress_menu, menu)
    PROGRESS_STATES.foreach { case(id, progress) =>
      menu.findItem(id).setIcon(PROGRESS_ICONS(progress))
    }
    this.menu = menu
    super.onCreateOptionsMenu(menu, inflater)
  }
  override def onPrepareOptionsMenu(menu: Menu): Unit = {
    menu.findItem(R.id.mark_progress).setIcon(PROGRESS_ICONS(task.last.progress))
    super.onPrepareOptionsMenu(menu)
  }
  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    actionMap.get(item.getItemId).map { action =>
      action(task.last)
      menu.findItem(R.id.mark_progress).setIcon(PROGRESS_ICONS(task.last.progress))
      true
    }.getOrElse(super.onOptionsItemSelected(item))
  }

}

object TaskDetailFragment {
  def newInstance[T](task: List[T], treelike: TreeLike[T]) = {
    implicit val tl = treelike
    new TaskDetailFragment[T](task)
  }
}
