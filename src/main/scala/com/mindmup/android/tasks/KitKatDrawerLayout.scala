package com.mindmup.android.tasks

import android.view._
import android.widget._
import android.support.v4.widget.DrawerLayout
import android.content.Context
import android.os.Build
import android.view.View._
import android.graphics.Rect
// workaround for https://code.google.com/p/android/issues/detail?id=63777
class KitKatDrawerLayout(c: Context) extends DrawerLayout(c) {
  var baseline = Integer.MAX_VALUE
  var change = 0

  override def fitSystemWindows(insets: Rect) = {
    val adj = insets.top + insets.bottom
    baseline = math.min(adj, baseline)

    if (baseline != Integer.MAX_VALUE && adj > baseline) {
      change = adj - baseline
    } else if (adj == baseline) {
      change = 0
    }

    super.fitSystemWindows(insets)
  }

  override def onMeasure(mw: Int, mh: Int) {
    if (Build.VERSION.SDK_INT >= 19) {
      val h = MeasureSpec.getSize(mh)
      val s = MeasureSpec.getMode(mh)
      super.onMeasure(mw, MeasureSpec.makeMeasureSpec(h - change, s))
    } else {
      super.onMeasure(mw, mh)
    }
  }
}
