package com.mindmup.android.tasks

import org.json4s._

trait TreeLike[T] {
  def name(t: T): String
  def attachment(t: T): Option[String]
  def color(t: T): Option[Int]
  def children(t: T): Seq[T]
}

object TreeLike {
  def allDescendants[T](t: T)(implicit tree: TreeLike[T]): Seq[T] = {
    val children = tree.children(t)
    children ++ children.flatMap(allDescendants(_)(tree))
  }

  def allDescendantsWithPaths[T](t: T)(implicit tree: TreeLike[T]): Seq[List[T]] = {
    val children = tree.children(t)
    (children.map(c => c :: Nil) ++ children.flatMap(allDescendantsWithPaths(_)(tree))).map(c => t :: c)
  }

  implicit class RichTreeLike[T: TreeLike](val t: T) {
    def title: String = implicitly[TreeLike[T]].name(t)
    def attachment: Option[String] = implicitly[TreeLike[T]].attachment(t)
    def color: Option[Int] = implicitly[TreeLike[T]].color(t)
  }
}
