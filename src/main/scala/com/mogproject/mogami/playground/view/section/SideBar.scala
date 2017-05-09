package com.mogproject.mogami.playground.view.section

import com.mogproject.mogami.playground.controller.Controller
import org.scalajs.dom.html.{Div, Heading}

import scalatags.JsDom.all._

/**
  *
  */
object SideBar {
  private[this] var isCollapsedValue = false

  val EXPANDED_WIDTH: Int = 460

  val COLLAPSED_WIDTH: Int = 60

  lazy val titleExpanded: Heading = h4(
    a(href := "#", onclick := { () => Controller.collapseSideBar() }, span(cls := "glyphicon glyphicon-minus")),
    span(paddingLeft := 14.px, "Menu")
  ).render

  lazy val titleCollapsed: Heading = h4(
    display := display.none.v,
    a(href := "#", onclick := { () => Controller.expandSideBar() }, span(cls := "glyphicon glyphicon-plus"))
  ).render


  lazy val output: Div = div(cls := "hidden-xs sidebar sidebar-right",
    width := EXPANDED_WIDTH,
    div(
      titleExpanded,
      titleCollapsed,
      MenuPane.output
    )
  ).render

  def collapseSideBar(): Unit = if (!isCollapsedValue) {
    output.style.width = COLLAPSED_WIDTH.px
    titleExpanded.style.display = display.none.v
    titleCollapsed.style.display = display.block.v
    MenuPane.collapseMenu()
    isCollapsedValue = true
  }

  def expandSideBar(): Unit = if (isCollapsedValue) {
    output.style.width = EXPANDED_WIDTH.px
    titleCollapsed.style.display = display.none.v
    titleExpanded.style.display = display.block.v
    MenuPane.expandMenu()
    isCollapsedValue = false
  }

  def isCollapsed: Boolean = isCollapsedValue
}
