package com.mogproject.mogami.playground.view.section

import com.mogproject.mogami.Game
import com.mogproject.mogami.core.game.Game.GamePosition
import com.mogproject.mogami.playground.controller.Language
import com.mogproject.mogami.playground.view.parts.branch.BranchButton
import com.mogproject.mogami.playground.view.parts.common.AccordionMenu
import com.mogproject.mogami.playground.view.parts.manage.SaveLoadButton
import com.mogproject.mogami.playground.view.parts.share._

import scalatags.JsDom.all._

/**
  *
  */
object GameMenuSection extends Section {
  override val accordions: Seq[AccordionMenu] = Seq(
    AccordionMenu(
      "Share",
      "Share",
      isExpanded = true,
      isVisible = true,
      div(
        RecordCopyButton.output,
        RecordShortenButton.output,
        br(),
        SnapshotCopyButton.output,
        SnapshotShortenButton.output,
        br(),
        ImageLinkButton.output,
        br(),
        SfenStringCopyButton.output
      )
    ),
    AccordionMenu(
      "Branch",
      "Branch",
      isExpanded = true,
      isVisible = true,
      div(
        BranchButton.output
      )
    ),
    AccordionMenu(
      "Manage",
      "Manage",
      isExpanded = false,
      isVisible = true,
      div(
        SaveLoadButton.output
      )
    )
  )

  def updateCommentOmissionWarning(displayWarning: Boolean): Unit =
    if (displayWarning)
      RecordCopyButton.showWarning()
    else
      RecordCopyButton.hideWarning()

  def updateBranchButtons(game: Game, gamePosition: GamePosition, language: Language): Unit =
    BranchButton.updateButtons(game, gamePosition, language)

  def showBranchEditMenu(): Unit = BranchButton.showEditMenu()

  def hideBranchEditMenu(): Unit = BranchButton.hideEditMenu()

  override def initialize(): Unit = {
    super.initialize()
    BranchButton.initialize()
  }

}
