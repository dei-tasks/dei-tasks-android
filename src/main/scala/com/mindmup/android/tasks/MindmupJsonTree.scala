package com.mindmup.android.tasks


import rapture.json.{ JsonBuffer, jsonBackends, jsonStringContext }
import jsonBackends.json4s._
import android.graphics.Color

object MindmupJsonTree {
  implicit def mindmupJsonTreeLike = new TreeLike[JsonBuffer] {
    import TreeLike._
    type JSON = JsonBuffer
    import rapture.data.Extractor.{ mapExtractor, optionExtractor }
    def title(t: JSON): String = t.title.as[String]
    def setTitle(t: JSON, title: String) = { t.title = title; t }
    val PROGRESS_MAP = Map("passing" -> Done, "in-progress" -> InProgress)
    val PROGRESS_TO_STRING = PROGRESS_MAP.map(_.swap)
    val PROGRESS_TO_COLOR = Map(Done -> "#00CC00", InProgress -> "#FFCC00")
    def progress(t: JSON) = t match {
      case json"""{"attr": {"progress": $progress}}""" => PROGRESS_MAP(progress.as[String])
      case _ => NotStarted
    }
    def setProgress(t: JSON, progress: Progress) = {
      if(progress == NotStarted) {
        t.attr -= "progress"
        t.attr.style -= "background"
      } else {
        t.attr.progress = PROGRESS_TO_STRING(progress)
        t.attr.style.background = PROGRESS_TO_COLOR(progress)
      }
      t
    }
    def attachment(t: JSON): Option[String] = t match {
      case json"""{"atachment": {"content" : $content}}""" => Some(content.toString)
      case _ => None
    }
    def color(t: JSON): Option[Int] = t match {
      case json"""{"attr": {"style": {"background": $bg}}}""" => {
        val background = bg.as[Option[String]]
        background.map(b => Color.parseColor(b.replaceFirst("#", "#FF")))
      }
      case _ => None
    }
    def children(t: JSON) = t match {
      case json"""{"ideas": $children}""" => {
        val keys = children.asInstanceOf[JSON].as[Map[String, JSON]].keys.toList
        keys.map(t.ideas.selectDynamic(_))
      }
      case _ => Seq.empty
    }
  }
}
