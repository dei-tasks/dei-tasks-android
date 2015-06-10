package com.mindmup.android.tasks

import org.json4s._

trait TreeLike[T] {
  def title(t: T): String
  def setTitle(t: T, title: String): T
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
    def title: String = implicitly[TreeLike[T]].title(t)
    def title_= (title: String) = implicitly[TreeLike[T]].setTitle(t, title)
    def attachment: Option[String] = implicitly[TreeLike[T]].attachment(t)
    def color: Option[Int] = implicitly[TreeLike[T]].color(t)
  }
}
