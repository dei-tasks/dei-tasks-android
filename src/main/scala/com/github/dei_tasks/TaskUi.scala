package com.github.dei_tasks

import android.support.v4.app.Fragment
import android.graphics.Color
import android.util.TypedValue
import android.view.Menu
import com.amulyakhare.textdrawable.TextDrawable
import com.github.dei_tasks.TreeLike.{NotStarted, InProgress, Done}

trait TaskUi[T] { this: Fragment =>
  def treeLike: TreeLike[T]
  val actionMap: Map[Int, T => Unit] = Map(
    R.id.mark_done -> { i: T => treeLike.setProgress(i, Done) },
    R.id.mark_in_progress -> { i: T => treeLike.setProgress(i, InProgress) },
    R.id.mark_not_started -> { i: T => treeLike.setProgress(i, NotStarted) }
  )
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
  def updateIcons(menu: Menu): Unit = {
    PROGRESS_STATES.foreach { case(id, progress) =>
      menu.findItem(id).setIcon(PROGRESS_ICONS(progress))
    }
  }
}
