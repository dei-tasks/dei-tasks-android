package com.mindmup.android.tasks

import org.json4s._

trait TreeLike[T] {
  def name(t: T): String
  def children(t: T): Seq[T]
}
