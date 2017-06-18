package com.mogproject.mogami.playground.view.parts.share

import com.mogproject.mogami.playground.view.parts.common.CopyButtonLike
import org.scalajs.dom.html.Div

import scalatags.JsDom.all._

/**
  *
  */
object RecordCopyButton extends CopyButtonLike with WarningLabelLike {
  override protected val ident = "record-copy"

  override protected val labelString = "Record URL"

  override lazy val output: Div = div(
    warningLabel,
    label(labelString),
    div(cls := "input-group",
      inputElem,
      div(
        cls := "input-group-btn",
        copyButton
      )
    )
  ).render

}
