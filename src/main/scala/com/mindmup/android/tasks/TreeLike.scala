package com.mindmup.android.tasks

import org.json4s._

trait TreeLike[T] {
  def title(t: T): String
  def setTitle(t: T, title: String): T
  def findChildByTitle(t: T, title: String): Option[T]
  def progress(t: T): TreeLike.Progress
  def setProgress(t: T, progress: TreeLike.Progress): T
  def attachment(t: T): Option[String]
  def color(t: T): Option[Int]
  def children(t: T): Seq[T]
  def addChild(t: T, child: T): T
  def newNode: T
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
    def tl: TreeLike[T] = implicitly[TreeLike[T]]
    def title: String = tl.title(t)
    def title_= (title: String) = tl.setTitle(t, title)
    def findChildByTitle(title: String) = tl.findChildByTitle(t, title)
    def attachment: Option[String] = tl.attachment(t)
    def color: Option[Int] = tl.color(t)
    def progress: Progress = tl.progress(t)
    def progress_=(progress: Progress) = tl.setProgress(t, progress)
  }
  implicit def pathTreeLike[T: TreeLike] = new TreeLike[List[T]] {
    val treeLike = implicitly[TreeLike[T]]
    type PATH = List[T]
    def title(t: PATH): String = treeLike.title(t.last)
    def progress(t: PATH): Progress = treeLike.progress(t.last)
    def setTitle(t: PATH, title: String) = t.dropRight(1) ++ List(treeLike.setTitle(t.last, title))
    def findChildByTitle(t: PATH, title: String) = treeLike.findChildByTitle(t.last, title).map(c => t ++ List(c))
    def setProgress(t: PATH, progress: Progress) = t.dropRight(1) ++ List(treeLike.setProgress(t.last, progress))
    def attachment(t: PATH): Option[String] = treeLike.attachment(t.last)
    def color(t: PATH): Option[Int] = treeLike.color(t.last)
    def children(t: PATH) = treeLike.children(t.last).map(c => t ++ List(c))
    def addChild(t: PATH, child: PATH) = t.dropRight(1) ++ List(treeLike.addChild(t.last, child.last))
    def newNode = List(treeLike.newNode)
  }
}
