package com.mindmup.android.tasks

import android.view._
import android.widget._

import macroid._
import macroid.contrib._
import macroid.FullDsl._
import macroid.viewable._

object Implicits {
  implicit def stringListable(implicit appCtx: AppContext, activityCtx: ActivityContext) =
    Listable[String].tw {
      w[TextView]
    } { string =>
      text(string)
    }
}
