package com.mindmup.android.tasks

import android.view.inputmethod.EditorInfo
import android.widget._
import macroid._
import macroid.contrib._
import macroid.FullDsl._
import macroid.IdGeneration
import macroid.viewable._
import macroid.contrib.LpTweaks._

object CustomTweaks {
  val selectableText = Tweak[TextView] { tv =>
    tv.setTextIsSelectable(true)
  }
  def imeOption(option: Int) = Tweak[EditText] { edit =>
    edit.setImeOptions(option)
  }
}
