package com.mindmup.android.tasks

import scala.collection.mutable.Map
object MindmupMapTree {
  implicit object MindmupMapTreeLike extends TreeLike[Map[String, Any]] {
    def title(t: Map[String, Any]): String = t("title").asInstanceOf[String]
    def setTitle(t: Map[String, Any], title: String) = t.updated("title", title)
    def attachment(t: Map[String, Any]): Option[String] = t.get("attachment").map(_.asInstanceOf[Map[String, String]]("content"))
    def color(t: Map[String, Any]): Option[Int] = {
      for {
        attr <- t.get("attr")
        style <- attr.asInstanceOf[Map[String, Any]].get("style")
        background <- style.asInstanceOf[Map[String, Any]].get("background") if background.isInstanceOf[String]
      } yield(android.graphics.Color.parseColor(background.asInstanceOf[String].replaceFirst("#", "#FF")))
    }
    def children(t: Map[String, Any]) = {
      t.get("ideas").map(_.asInstanceOf[Map[String, Map[String, Any]]].values.toList).getOrElse(List())
    }
  }
}
