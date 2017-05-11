package com.mogproject.mogami.playground.view.parts.settings

import com.mogproject.mogami.playground.controller.{Configuration, Controller}
import com.mogproject.mogami.playground.view.parts.common.DropdownMenu
import org.scalajs.dom.html.Div

import scalatags.JsDom.all._

/**
  *
  */
object BoardSizeButton {

  /**
    * definitions of board sizes
    */
  sealed abstract class PresetBoardSize(val width: Int, val label: String) {
    override def toString: String = s"${width} - ${label}"
  }

  case object LandscapeIPhone5 extends PresetBoardSize(Configuration.getDefaultCanvasWidth(568, 320 - 44, isLandscape = true), "iPhone 5 (Landscape)")

  case object LandscapeIPhone6 extends PresetBoardSize(Configuration.getDefaultCanvasWidth(667, 375 - 44, isLandscape = true), "iPhone 6 (Landscape)")

  case object LandscapeIPhone6Plus extends PresetBoardSize(Configuration.getDefaultCanvasWidth(736, 414 - 44, isLandscape = true), "iPhone 6 Plus (Landscape)")

  case object PortraitIPhone5 extends PresetBoardSize(Configuration.getDefaultCanvasWidth(320, 568 - 108, isLandscape = false), "iPhone 5 (Portrait)")

  case object PortraitIPhone6 extends PresetBoardSize(Configuration.getDefaultCanvasWidth(375, 667 - 108, isLandscape = false), "iPhone 6 (Portrait)")

  case object ExtraSmall extends PresetBoardSize(120, "Extra Small")

  case object Small extends PresetBoardSize(240, "Small")

  case object Medium extends PresetBoardSize(320, "Medium")

  case object Large extends PresetBoardSize(400, "Large")

  case object ExtraLarge extends PresetBoardSize(480, "Extra Large")


  private[this] lazy val sizeButton = DropdownMenu(
    Vector(ExtraSmall, LandscapeIPhone5, LandscapeIPhone6, LandscapeIPhone6Plus, Small, PortraitIPhone5, Medium, PortraitIPhone6, Large, ExtraLarge),
    8, "Board Size", _ => (), "btn-group", "left"
  )

  private[this] val setButton = button(
    cls := "btn btn-default",
    onclick := { () => Controller.changeBoardSize(sizeButton.getValue) },
    "Set"
  ).render

  lazy val output: Div = div(
    cls := "btn-group pull-right", role := "group", marginTop := (-8).px, sizeButton.output, setButton
  ).render

}
