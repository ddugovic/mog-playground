package com.mogproject.mogami.playground.view.parts.board

import com.mogproject.mogami._
import com.mogproject.mogami.playground.controller.{Controller, Cursor}
import com.mogproject.mogami.playground.view.layout.{BoardLayout, Layout}
import com.mogproject.mogami.playground.view.parts.common.EventManageable
import com.mogproject.mogami.playground.view.renderer.{Circle, Rectangle}
import com.mogproject.mogami.util.Implicits._
import org.scalajs.dom
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.{CanvasRenderingContext2D, MouseEvent}

/**
  *
  */
trait CursorManageable extends EventManageable {
  // variables
  private[this] var activeCursor: Option[Cursor] = None
  private[this] var selectedCursor: Option[Cursor] = None
  private[this] var animationScheduler: Option[Int] = None
  private[this] var selectAnimationActive: Boolean = false

  // constants
  private[this] val boxPtypes: Seq[Ptype] = Ptype.KING +: Ptype.inHand

  protected val layout: BoardLayout

  protected val canvas2: Canvas
  protected val layer0: CanvasRenderingContext2D
  protected val layer3: CanvasRenderingContext2D
  protected val layer4: CanvasRenderingContext2D
  protected val layer5: CanvasRenderingContext2D

  def isFlipped: Boolean

  /**
    * Convert MouseEvent to Cursor
    *
    * @return Cursor if the mouse position is inside the specific area
    */
  def getCursor(clientX: Double, clientY: Double): Option[Cursor] = {
    val rect = canvas2.getBoundingClientRect()
    val (x, y) = (clientX - rect.left, clientY - rect.top)

    val c = if (layout.board.isInside(x, y)) {
      val file = 9 - ((x - layout.board.left) / layout.PIECE_WIDTH).toInt
      val rank = 1 + ((y - layout.board.top) / layout.PIECE_HEIGHT).toInt
      Some(Cursor(Square(file, rank)))
    } else if (layout.handBlack.isInside(x, y)) {
      getCursorHand(x, isBlack = true)
    } else if (layout.handWhite.isInside(x, y)) {
      getCursorHand(x, isBlack = false)
    } else if (layout.playerBlack.isInside(x, y)) {
      Some(Cursor(Player.BLACK))
    } else if (layout.playerWhite.isInside(x, y)) {
      Some(Cursor(Player.WHITE))
    } else if (layout.pieceBox.isInside(x, y)) {
      val offset = x - layout.pieceBox.left
      val i = (offset / layout.PIECE_WIDTH).toInt
      (i <= 7 && offset % layout.PIECE_WIDTH <= layout.PIECE_WIDTH).option(Cursor(boxPtypes(i)))
    } else {
      None
    }
    isFlipped.when[Option[Cursor]](_.map(!_))(c)
  }

  private[this] def getCursorHand(x: Double, isBlack: Boolean): Option[Cursor] = {
    val offset = isBlack.fold(x - layout.handBlack.left, layout.handWhite.right - x)
    val i = (offset / layout.HAND_PIECE_WIDTH).toInt
    (i <= 6 && offset % layout.HAND_PIECE_WIDTH <= layout.HAND_PIECE_WIDTH).option {
      Cursor(Piece(isBlack.fold(Player.BLACK, Player.WHITE), Ptype.inHand(i)))
    }
  }

  /**
    * Convert Cursor object to Rectangle.
    */
  private[this] def cursorToRect(cursor: Cursor): Rectangle = {
    isFlipped.when[Cursor](!_)(cursor) match {
      case Cursor(None, Some(Hand(Player.BLACK, pt)), None, None) =>
        Rectangle(
          layout.handBlack.left + (pt.sortId - 1) * layout.HAND_PIECE_WIDTH,
          layout.handBlack.top,
          layout.HAND_PIECE_WIDTH,
          layout.HAND_PIECE_HEIGHT
        )
      case Cursor(None, Some(Hand(Player.WHITE, pt)), None, None) =>
        Rectangle(
          layout.handWhite.right - (pt.sortId - 1) * layout.HAND_PIECE_WIDTH - layout.HAND_PIECE_WIDTH,
          layout.handWhite.top,
          layout.HAND_PIECE_WIDTH,
          layout.HAND_PIECE_HEIGHT
        )
      case Cursor(Some(sq), None, None, None) =>
        Rectangle(
          layout.board.left + (9 - sq.file) * layout.PIECE_WIDTH,
          layout.board.top + (sq.rank - 1) * layout.PIECE_HEIGHT,
          layout.PIECE_WIDTH,
          layout.PIECE_HEIGHT
        )
      case Cursor(None, None, Some(pt), None) =>
        Rectangle(
          layout.pieceBox.left + pt.sortId * layout.PIECE_WIDTH,
          layout.pieceBox.top,
          layout.PIECE_WIDTH,
          layout.PIECE_HEIGHT
        )
      case Cursor(None, None, None, Some(Player.BLACK)) => layout.playerBlack
      case Cursor(None, None, None, Some(Player.WHITE)) => layout.playerWhite
      case _ => Rectangle(0, 0, layout.PIECE_WIDTH, layout.PIECE_HEIGHT) // never happens
    }
  }

  /**
    * Draw a highlighted cursor.
    */
  def drawActiveCursor(cursor: Cursor): Unit = {
    clearActiveCursor()
    cursorToRect(cursor).draw(layer3, layout.color.cursor, -2)
    activeCursor = Some(cursor)
  }

  def flashCursor(cursor: Cursor): Unit = {
    val c = cursorToRect(cursor)
    c.draw(layer4, layout.color.flash, -2)
    val f = () => c.clear(layer4)
    dom.window.setTimeout(f, 300)
  }

  /**
    * Clear an active cursor.
    */
  def clearActiveCursor(): Unit = {
    activeCursor.foreach(cursorToRect(_).clear(layer3))
    activeCursor = None
  }

  /**
    * Draw the selected area.
    */
  def drawSelectedArea(cursor: Cursor): Unit = {
    cursorToRect(cursor).drawFill(layer0, layout.color.cursor, 2)
    selectedCursor = Some(cursor)
    startSelectAnimation(cursor)
  }

  /**
    * Clear a selected area.
    */
  def clearSelectedArea(): Unit = {
    selectedCursor.foreach(cursorToRect(_).clear(layer0))
    selectedCursor = None
    stopSelectAnimation()
  }

  /**
    * Draw the last move area.
    */
  def drawLastMove(move: Option[Move]): Unit = {
    val newArea: Set[Cursor] = move match {
      case None => Set.empty
      case Some(mv) =>
        val fr = mv.from match {
          case None => Cursor(mv.player, mv.oldPtype)
          case Some(sq) => Cursor(sq)
        }
        Set(fr, Cursor(mv.to))
    }

    clearLastMove()
    newArea.foreach(a => cursorToRect(a).drawFill(layer0, layout.color.light, 1))
  }

  def clearLastMove(): Unit = {
    layout.board.clear(layer0)
    layout.handWhite.clear(layer0)
    layout.handBlack.clear(layer0)
  }


  /**
    * Circular animation
    */
  case class CircularAnimation(centerX: Double, centerY: Double, minRadius: Double, maxRadius: Double, strokeWidth: Int, strokeColor: String) {
    private[this] lazy val layer: CanvasRenderingContext2D = layer5

    private[this] lazy val drawArea = Rectangle(
      (centerX - maxRadius - strokeWidth).toInt - 1,
      (centerY - maxRadius - strokeWidth).toInt - 1,
      ((maxRadius + strokeWidth).toInt + 1) * 2,
      ((maxRadius + strokeWidth).toInt + 1) * 2
    )

    def draw(ratio: Double): Unit = {
      drawArea.clear(layer)
      val r = minRadius + (maxRadius - minRadius) * ratio
      Circle(centerX, centerY, r).stroke(layer, strokeColor, math.max(1, strokeWidth), 1.0 - ratio)
    }

    def clear(): Unit = {
      drawArea.clear(layer)
    }
  }

  object CircularAnimation {
    def apply(cursor: Cursor, color: String): CircularAnimation = {
      val rect = cursorToRect(cursor)
      val centerX = (rect.left + rect.right) / 2.0
      val centerY = (rect.top + rect.bottom) / 2.0
      CircularAnimation(centerX, centerY, layout.PIECE_WIDTH * 0.4, layout.PIECE_WIDTH * 2.0, (layout.PIECE_WIDTH / 10.0).toInt, color)
    }
  }

  /**
    * Animation while selecting a piece
    */
  def startSelectAnimation(cursor: Cursor): Unit = {
    val animation = CircularAnimation(cursor, layout.color.flash)

    def f(frame: Int): Unit = if (selectAnimationActive) {
      if (frame <= 40) animation.draw(frame / 40.0)
      dom.window.requestAnimationFrame(_ => f((frame + 1) % 100))
    } else {
      animation.clear()
    }

    selectAnimationActive = true
    dom.window.requestAnimationFrame(_ => f(0))
  }

  def stopSelectAnimation(): Unit = {
    selectAnimationActive = false
  }

  /**
    * Animation after making a move
    */
  def startMoveAnimation(cursor: Cursor): Unit = {
    val animation = CircularAnimation(cursor, layout.color.cursor)

    def f(frame: Int): Unit = if (frame <= 20) {
      animation.draw(frame / 20.0)
      dom.window.requestAnimationFrame(_ => f(frame + 1))
    }

    dom.window.requestAnimationFrame(_ => f(0))
  }

  //
  // mouseDown
  //
  protected def mouseDown(x: Double, y: Double): Unit = mouseDown(getCursor(x, y))

  private[this] def mouseDown(cursor: Option[Cursor]): Unit = {
    cursor.foreach(c => if (Controller.canActivate(c)) flashCursor(c))
    (selectedCursor, cursor) match {
      case (_, Some(invoked)) if Controller.canInvokeWithoutSelection(invoked) =>
        Controller.invokeCursor(invoked, invoked, isFlipped)
        registerHoldEvent(() => Controller.invokeHoldEvent(invoked, isFlipped))
      case (Some(sel), Some(invoked)) =>
        clearSelectedArea()
        Controller.invokeCursor(sel, invoked, isFlipped)
        clearHoldEvent()
      case (Some(sel), None) =>
        clearSelectedArea()
      case (None, Some(sel)) if Controller.canSelect(sel) =>
        drawSelectedArea(sel)
        registerHoldEvent(() => Controller.invokeHoldEvent(sel, isFlipped))
      case _ => // do nothing
    }
  }

  //
  // mouseUp
  //
  protected def mouseUp(x: Double, y: Double): Unit = mouseUp(getCursor(x, y))

  private[this] def mouseUp(cursor: Option[Cursor]): Unit = {
    (cursor, selectedCursor) match {
      case (Some(released), Some(selected)) if released != selected => Controller.processMouseUp(selected, released) match {
        case Some(adjusted) => mouseDown(Some(adjusted))
        case _ =>
      }
      case _ =>
    }
    clearHoldEvent()
  }

  //
  // mouseMove
  //
  protected def mouseMove(evt: MouseEvent): Unit = getCursor(evt.clientX, evt.clientY) match {
    case x if x == activeCursor => // do nothing
    case Some(cursor) if Controller.canActivate(cursor) => drawActiveCursor(cursor)
    case _ => clearActiveCursor()
  }

}
