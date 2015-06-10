package com.mindmup.android.tasks

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._
import com.gu.json._
import J._
import CursorArrowSyntax._
import PStateSyntax._
import com.gu.json.syntax._
import com.gu.json.Lenses._
import android.graphics.Color
object MindmupJsonTree {
  implicit def mindmupJsonTreeLike[J: JsonLike] = new TreeLike[Cursor[J]] {
    implicit val formats = DefaultFormats
    def title(t: Cursor[J]): String = (for {
      tit <- t.field("title")
      titleString <- asString(tit.focus)
    } yield(titleString)).get
    def setTitle(t: Cursor[J], title: String) = t.field("title").map(_.replace(string(title))).get
    def attachment(t: Cursor[J]): Option[String] = for {
      att <- t.field("attachment")
      cont <- att.field("content")
      str <- asString(cont.focus)
    } yield(str)
    def color(t: Cursor[J]): Option[Int] = for {
      attr <- t.field("attr")
      style <- attr.field("style")
      background <- style.field("background")
      backgroundString <- asString(background.focus)
    } yield(Color.parseColor(backgroundString.replaceFirst("#", "#FF")))
    def children(t: Cursor[J]) = (for {
      ideas <- t.field("ideas")
      keys <- ideas.keySet
    } yield {
        println(s"Ideas $ideas has the following keys: $keys")
        keys.flatMap(ideas.field(_))
      }.toSeq).getOrElse(Seq.empty)

  }
}
