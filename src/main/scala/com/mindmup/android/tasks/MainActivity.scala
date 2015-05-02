package com.mindmup.android.tasks

import android.app.Activity
import android.os.Bundle
import android.widget._
import macroid._
import macroid.contrib._
import macroid.FullDsl._

object OurTweaks {
  def greeting(greeting: String)(implicit appCtx: AppContext) =
    TextTweaks.large +
    text(greeting) +
    hide

  def orient(implicit appCtx: AppContext) =
    landscape ? horizontal | vertical
}

class MainActivity extends Activity with Contexts[Activity] {
  var greeting = slot[TextView]

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)

    setContentView {
      getUi {
        l[LinearLayout](
          w[Button] <~
            text("Click me") <~
            On.click {
              greeting <~ show
            },
          w[TextView] <~
            wire(greeting) <~
            OurTweaks.greeting("Hello!")
        ) <~ OurTweaks.orient
      }
    }
  }
}
