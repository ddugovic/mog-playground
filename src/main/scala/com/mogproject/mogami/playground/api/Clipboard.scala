package com.mogproject.mogami.playground.api

import org.scalajs.dom.{Element, NodeListOf}

import scala.scalajs.js
import js.annotation._
import js.|

@js.native
@JSName("Clipboard")
class Clipboard protected() extends js.Object {
  def this(selector: String | Element | NodeListOf[Element], options: Clipboard.Options = ???) = this()

  def on(`type`: String, handler: js.Function): Clipboard = js.native

  def destroy(): Unit = js.native
}

@js.native
@JSName("Clipboard")
object Clipboard extends js.Object {

  @js.native
  trait Options extends js.Object {
    var action: js.Function1[Element, String] = js.native
    var target: js.Function1[Element, Element] = js.native
    var text: js.Function1[Element, String] = js.native
  }

  @js.native
  trait Event extends js.Object {
    var action: String = js.native
    var text: String = js.native
    var trigger: Element = js.native

    def clearSelection(): Unit = js.native
  }

}
