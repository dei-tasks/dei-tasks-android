package com.github.dei_tasks

import macroid.util.Effector
import rx._
import rx.ops._



// support for Scala.Rx
// will be a part of macroid-frp
trait RxSupport {
  var refs = List.empty[AnyRef]
  implicit def rxEffector = new Effector[Rx] {
    override def foreach[A](fa: Rx[A])(f: A ⇒ Any): Unit =
      refs ::= fa.foreach(f andThen (_ ⇒ ()))
  }
}
