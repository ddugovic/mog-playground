package com.mogproject.mogami.playground.view.section

import com.mogproject.mogami.playground.view.parts.common.AccordionMenu

import scalatags.JsDom.all._

/**
  *
  */
object GameHelpSection extends Section {
  override val accordions: Seq[AccordionMenu] = Seq(
    AccordionMenu(
      "Help",
      "Help",
      isExpanded = true,
      isVisible = true,
      div(
        ul(
          li("Click on a player name to set the game information."),
          li("In Play Mode, you can move pieces by a flick."),
          li("In View Mode, click on any squares on the right-hand side of the board to move to the next position, and click the left-hand side to the previous position."),
          li("If you click and hold 'forward' or 'backward' button, the position changes continuously.")
        )
      )
    )
  )
}
