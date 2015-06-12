package com.mindmup.android.tasks

import com.mindmup.android.tasks.TreeLike.{NotStarted, InProgress, Done}

trait TaskUi[T] {
  def treeLike: TreeLike[T]
  val actionMap: Map[Int, T => Unit] = Map(
    R.id.mark_done -> { i: T => treeLike.setProgress(i, Done) },
    R.id.mark_in_progress -> { i: T => treeLike.setProgress(i, InProgress) },
    R.id.mark_not_started -> { i: T => treeLike.setProgress(i, NotStarted) }
  )
}
