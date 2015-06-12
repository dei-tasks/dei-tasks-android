package com.mindmup.android.tasks

import org.json4s._

trait TreeLike[T] {
  def title(t: T): String
  def setTitle(t: T, title: String): T
  def progress(t: T): TreeLike.Progress
  def setProgress(t: T, progress: TreeLike.Progress): T
  def attachment(t: T): Option[String]
  def color(t: T): Option[Int]
  def children(t: T): Seq[T]
}

object TreeLike {
  sealed trait Progress
  object Done extends Progress
  object InProgress extends Progress
  object NotStarted extends Progress

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
    def progress: Progress = implicitly[TreeLike[T]].progress(t)
    def progress_=(progress: Progress) = implicitly[TreeLike[T]].setProgress(t, progress)
  }
  implicit def pathTreeLike[T: TreeLike] = new TreeLike[List[T]] {
    val treeLike = implicitly[TreeLike[T]]
    type PATH = List[T]
    def title(t: PATH): String = treeLike.title(t.last)
    def progress(t: PATH): Progress = treeLike.progress(t.last)
    def setTitle(t: PATH, title: String) = t.dropRight(1) ++ List(treeLike.setTitle(t.last, title))
    def setProgress(t: PATH, progress: Progress) = t.dropRight(1) ++ List(treeLike.setProgress(t.last, progress))
    def attachment(t: PATH): Option[String] = treeLike.attachment(t.last)
    def color(t: PATH): Option[Int] = treeLike.color(t.last)
    def children(t: PATH) = treeLike.children(t.last).map(c => t ++ List(c))
  }
}
