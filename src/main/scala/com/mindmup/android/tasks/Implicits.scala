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
  implicit def taskListable[T: TreeLike](implicit appCtx: AppContext, activityCtx: ActivityContext) =
    Listable[List[T]].tw {
      w[TextView]
    } { ml =>
      import TreeLike._
      var nodeText = ml.map(_.title).mkString(" / ")
      val color = for {
        last <- ml.lastOption
        color <- last.color
      } yield(color)

      val tt = text(nodeText)
      val transparentColor = android.graphics.Color.parseColor("#00000000")
      tt + BgTweaks.color(color.getOrElse(transparentColor))
    }

}
