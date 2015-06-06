package com.mindmup.android.tasks

import android.view._
import android.widget._

import macroid._
import macroid.contrib._
import macroid.FullDsl._
import macroid.viewable._
import android.util.Log

object Implicits {
  private val TAG = "MindmupTasks"
  def println(s: String): Unit = Log.i(TAG, s)

  implicit def stringListable(implicit appCtx: AppContext, activityCtx: ActivityContext) =
    Listable[String].tw {
      w[TextView]
    } { string =>
      text(string)
    }
  implicit def taskListable(implicit appCtx: AppContext, activityCtx: ActivityContext) =
    Listable[List[Map[String, Any]]].tw {
      w[TextView]
    } { ml =>
      var nodeText = ml.map(_.getOrElse("title", "NO TITLE")).mkString(" / ")
      val color = for {
        last <- ml.lastOption
        attr <- last.get("attr")
        style <- attr.asInstanceOf[Map[String, Any]].get("style")
        background <- style.asInstanceOf[Map[String, Any]].get("background") if background.isInstanceOf[String]
      } yield(android.graphics.Color.parseColor(background.asInstanceOf[String].replaceFirst("#", "#FF")))

      val tt = text(nodeText)
      color.map(c => tt + BgTweaks.color(c)).getOrElse(tt)
    }

}
