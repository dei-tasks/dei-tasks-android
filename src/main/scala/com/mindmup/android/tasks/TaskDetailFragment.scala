package com.mindmup.android.tasks

import android.app._
import android.content.{Intent, IntentSender, Context, SharedPreferences}
import android.content.IntentSender.SendIntentException
import android.graphics.Color
import android.os.Bundle
import android.util.{TypedValue, Log}
import android.view._
import android.view.inputmethod.{InputMethodManager, EditorInfo}
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

class TaskDetailFragment[T: TreeLike](task: List[T]) extends Fragment with Contexts[Fragment] with RxSupport
with TaskUi[T] with IdGeneration {

  import TreeLike._
  import CustomTweaks._

  def treeLike = implicitly[TreeLike[T]]
  var menu: Menu = _
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    setHasOptionsMenu(true)
    import TextTweaks._
    getUi {
      l[LinearLayout](
        w[TextView] <~ text("Title") <~ bold,
        w[EditText] <~
          id(Id.taskTitleEditor) <~
          text(task.last.title) <~
          selectableText <~
          FuncOn.editorAction[EditText] { (v: TextView, actionId: Int, event: KeyEvent) =>
            updateTaskTitle()
            Ui(true)
          } <~
          showKeyboard <~
          imeOption(EditorInfo.IME_ACTION_DONE) <~
          Tweak[View] { v =>
            v.clearFocus()
            v.requestFocus()
          },
        w[TextView] <~ text("Attachment") <~ bold,
        w[EditText] <~ text(task.last.attachment.getOrElse("")) <~ selectableText
      ) <~ vertical
    }
  }
  def updateTaskTitle(): Unit = {
    val edit = this.find[EditText](Id.taskTitleEditor).get.get
    implicitly[TreeLike[T]].setTitle(task.last, edit.getText.toString)
  }

  override def onStop(): Unit = {
    updateTaskTitle()
    super.onStop()
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater): Unit = {
    inflater.inflate(R.menu.progress_menu, menu)
    this.menu = menu
    updateIcons(menu)
    super.onCreateOptionsMenu(menu, inflater)
  }
  override def onPrepareOptionsMenu(menu: Menu): Unit = {
    menu.findItem(R.id.mark_progress).setIcon(PROGRESS_ICONS(task.last.progress))
    super.onPrepareOptionsMenu(menu)
  }
  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    actionMap.get(item.getItemId).map { action =>
      action(task.last)
      menu.findItem(R.id.mark_progress).setIcon(PROGRESS_ICONS(PROGRESS_STATES(item.getItemId)))
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
