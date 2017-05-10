package com.mogproject.mogami.playground.view.section

import com.mogproject.mogami.playground.view.parts.common.AccordionMenu
import com.mogproject.mogami.playground.view.parts.edit.EditReset

import scalatags.JsDom.all._

/**
  *
  */
object EditSection extends Section {
  override def initialize(): Unit = {
    super.initialize()
    EditReset.initialize()
  }

  override val accordions: Seq[AccordionMenu] = Seq(
    AccordionMenu(
      "Reset",
      "Reset",
      "erase",
      isExpanded = false,
      isVisible = false,
      div(EditReset.output)
    ),
    AccordionMenu(
      "EditHelp",
      "Help",
      "question-sign",
      isExpanded = false,
      isVisible = false,
      div(
        ul(
          li("Click on a player name to set the turn to move."),
          li("Double-click on a piece on the board to change the piece attributes: Black Unpromoted -> Black Promoted -> White Unpromoted -> White Promoted -> Black Unpromoted.")
        ))
    )
  )

}
