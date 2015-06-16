package com.mindmup.android.tasks


import rapture.json.{ JsonBuffer, jsonBackends, jsonStringContext, jsonBufferStringContext }
import jsonBackends.jawn._
import android.graphics.Color

object MindmupJsonTree {
  implicit val mindmupJsonTreeLike = new TreeLike[JsonBuffer] {
    import TreeLike._
    type JSON = JsonBuffer
    val maxIds = scala.collection.mutable.Map.empty[JSON, Int]
    import rapture.data.Extractor.{ mapExtractor, optionExtractor }
    def title(t: JSON): String = t.title.as[String]
    def setTitle(t: JSON, title: String) = { t.title = title; t }
    def findChildByTitle(t: JSON, title: String) = {
      children(t).find(_.title.as[String] == title)
    }
    val PROGRESS_MAP = Map("passing" -> Done, "in-progress" -> InProgress)
    val PROGRESS_TO_STRING = PROGRESS_MAP.map(_.swap)
    val PROGRESS_TO_COLOR = Map(Done -> "#00CC00", InProgress -> "#FFCC00")
    def progress(t: JSON) = t match {
      case json"""{"attr": {"progress": $progress}}""" => PROGRESS_MAP(progress.as[String])
      case _ => NotStarted
    }
    def setProgress(t: JSON, progress: Progress) = {
      if(progress == NotStarted) {
        t match {
          case json"""{"attr" : {"style" : $something}}""" =>
            t.attr.style -= "background"
            t.attr -= "progress"
          case json"""{"attr" : $something}""" =>
            t.attr -= "progress"
          case _ => ()
        }
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
    def ideas(node: JSON) = node match {
      case json"""{"ideas": $ideas}""" => ideas.asInstanceOf[JSON]
      case _ => json"{}".as[JSON]
    }
    def addChild(t: JSON, child: JSON) = {
      val nodeId = child match {
        case json"""{"id": $i}""" => i.as[Int]
        case _ =>
          val root = t.$deref(Vector.empty)
          val maxId = findMaxId(root)
          val newMaxId = maxId + 1
          maxIds(root) = newMaxId
          newMaxId
      }
      val maxIdeaKey = ideas(t).as[Map[String, JSON]].keySet.map(_.toInt).reduceOption(_ max _)
      val newIdeaKey = (maxIdeaKey.getOrElse(0) + 1).toString
      t.ideas.updateDynamic(newIdeaKey)(child)
      t.ideas.selectDynamic(newIdeaKey).id = nodeId
      t
    }
    def findMaxId(t: JSON): Int = {
      maxIds.getOrElseUpdate(t, (id(t) :: children(t).map(findMaxId _).toList).max)
    }
    def id(t: JSON): Int = {
      t.id.as[Int]
    }
    def newNode = {
      val nodeName = s"Node ${System.currentTimeMillis}"
      jsonBuffer"""{"title": $nodeName}"""
    }
  }
}
