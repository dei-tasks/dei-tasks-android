package com.mindmup.android.tasks

import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

import android.graphics.Color
object MindmupJsonTree {
  implicit object MindmupJsonTreeLike extends TreeLike[JObject] {
    implicit val formats = DefaultFormats
    def name(t: JObject): String = (t \ "title").extract[String]
    def attachment(t: JObject): Option[String] = (t \ "attachment" \ "content").extract[Option[String]]
    def color(t: JObject): Option[Int] = {
      val backgroundStringOption = (t \ "attr" \ "style" \ "background" \\ classOf[JString]).headOption
      backgroundStringOption.map { background =>
          android.graphics.Color.parseColor(background.replaceFirst("#", "#FF"))
      }
    }
    def children(t: JObject) = {
      (t \ "ideas").children.map(_.asInstanceOf[JObject])
    }
  }
}
