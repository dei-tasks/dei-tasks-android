package com.mindmup.android.tasks

import android.app._
import android.content.{ Intent, IntentSender, Context, SharedPreferences }
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import android.view._
import android.widget._
import macroid._
import macroid.contrib._
import macroid.FullDsl._
import macroid.IdGeneration
import macroid.viewable._
import macroid.contrib.LpTweaks._

import android.support.v4.app.Fragment

import rx._
import rx.ops._


class TaskDetailFragment(task: List[Map[String, Any]]) extends Fragment with Contexts[Fragment] with RxSupport {
  import FilterableListableListAdapter._
  import Implicits._


  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    getUi { w[TextView] <~ text("Details") }
  }
}

object TaskDetailFragment {
  def newInstance(task: List[Map[String, Any]]) = new TaskDetailFragment(task)
}
