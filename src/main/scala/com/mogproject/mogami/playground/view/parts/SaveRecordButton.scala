package com.mogproject.mogami.playground.view.parts

import com.mogproject.mogami.playground.controller.Controller
import org.scalajs.dom.html.{Button, Div}

import scalatags.JsDom.all._

/**
  *
  */
object SaveRecordButton {
  lazy val output: Div = div(
    label("Save to File"),
    div(cls := "row", inputs.map(e => div(cls := "col-md-3 col-xs-4", e)))
  ).render

  private[this] val inputs = List(
    generateInput("CSA", () => Controller.saveRecordCsa()),
    generateInput("KIF", () => Controller.saveRecordKif()),
    generateInput("KI2", () => Controller.saveRecordKi2())
  )

  private[this] def generateInput(title: String, f: () => Unit): Button = button(
    tpe := "button",
    cls := "btn btn-default btn-block",
    data("toggle") := "tooltip",
    data("placement") := "bottom",
    data("dismiss") := "modal",
    data("original-title") := {
      if (title == "CSA") s"Save the record as ${title} Format" else "To be implemented..."
    },
    onclick := f,
    title
  ).render

}
