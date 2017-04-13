package com.mogproject.mogami.playground.view.parts.common

import org.scalajs.dom.html.Div

import scalatags.JsDom.all._

/**
  * Dropdown menu
  */

case class DropdownMenu(items: Vector[String], default: Int, onChange: Int => Unit) {
  private[this] var value: Int = 0

  private[this] val labelButton = button(
    tpe := "button",
    cls := "btn btn-default dropdown-toggle",
    data("toggle") := "dropdown"
  ).render

  private[this] def initialize(): Unit = updateValue(default)

  initialize()

  lazy val output: Div = div(
    cls := "input-group-btn",
    labelButton,
    ul(
      cls := "dropdown-menu",
      h6(cls := "dropdown-header", "Image Size"),
      items.zipWithIndex.map { case (s, i) => li(a(onclick := { () => updateValue(i); onChange(i) }, s)) }
    )
  ).render

  private[this] def updateValue(index: Int): Unit = if (0 <= index && index < items.length) {
    // update label
    labelButton.innerHTML = items(index) + " " + span(cls := "caret")

    // update the internal value
    value = index
  }

  def getValue: Int = value
}