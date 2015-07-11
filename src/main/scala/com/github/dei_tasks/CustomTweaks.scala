package com.github.dei_tasks

import android.content.Context
import android.view.View
import android.view.inputmethod.{InputMethodManager, EditorInfo}
import android.widget._
import com.github.dei_tasks.Implicits._
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
  def showKeyboard(implicit activity: ActivityContext) = FuncOn.focusChange {
    (v: View, hasFocus: Boolean) =>
      v.post(() => {
        val imm = activity.get.getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
        imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
      })
      Ui(true)
  }
}
