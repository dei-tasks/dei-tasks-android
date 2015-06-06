package com.mindmup.android.tasks

object MindmupJsonTree {
  implicit object MindmupMapTreeLike extends TreeLike[Map[String, Any]] {
    def name(t: Map[String, Any]) = t("title").asInstanceOf[String]
    def children(t: Map[String, Any]) = {
      t.get("ideas").map(_.asInstanceOf[Map[String, Map[String, Any]]].values.toList).getOrElse(List())
    }
  }
  def allDescendants[T](t: T)(implicit tree: TreeLike[T]): Seq[T] = {
    val children = tree.children(t)
    children ++ children.flatMap(allDescendants(_)(tree))
  }

  def allDescendantsWithPaths[T](t: T)(implicit tree: TreeLike[T]): Seq[List[T]] = {
    val children = tree.children(t)
    (children.map(c => c :: Nil) ++ children.flatMap(allDescendantsWithPaths(_)(tree))).map(c => t :: c)
  }
}
