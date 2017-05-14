package com.mogproject.mogami.playground.view.renderer.piece

import com.mogproject.mogami.core.Player.BLACK
import com.mogproject.mogami.playground.view.layout.BoardLayout
import com.mogproject.mogami.playground.view.renderer.TextRenderer
import com.mogproject.mogami.{Hand, Piece, Ptype, Square}
import org.scalajs.dom.CanvasRenderingContext2D

trait PieceRenderer {

  def layout: BoardLayout

  def drawPiece(ctx: CanvasRenderingContext2D, piece: Piece, left: Int, top: Int, scale: Double = 1.0): Unit

  def drawOnBoard(ctx: CanvasRenderingContext2D, piece: Piece, square: Square): Unit = {
    val left = layout.board.left + layout.PIECE_WIDTH * (9 - square.file)
    val top = layout.board.top + layout.PIECE_HEIGHT * (square.rank - 1)
    drawPiece(ctx, piece, left, top)
  }

  private[this] def drawNumbers(ctx: CanvasRenderingContext2D, n: Int, left: Int, top: Int, rotated: Boolean, scale: Double = 1.0): Unit = {
    if (n > 1) {
      TextRenderer(ctx, n.toString, layout.font.numberOfPieces, layout.color.number,
        left, top, (layout.PIECE_WIDTH * scale).toInt, (layout.PIECE_HEIGHT * scale).toInt
      ).alignRight.alignBottom.withRotate(rotated).withStroke(layout.color.stroke, layout.strokeSize).render()
    }
  }

  def drawInHand(ctx: CanvasRenderingContext2D, piece: Hand, numPieces: Int): Unit = {
    // piece type
    val (left, top) = if (piece.owner.isBlack) {
      (layout.handBlack.left + layout.HAND_PIECE_WIDTH * (piece.ptype.sortId - 1), layout.handBlack.top)
    } else {
      (layout.handWhite.right - layout.HAND_PIECE_WIDTH * piece.ptype.sortId, layout.handWhite.top)
    }
    drawPiece(ctx, piece.toPiece, left, top, 6.0 / 7)

    // number of pieces
    drawNumbers(ctx, numPieces, left, top, piece.owner.isWhite, 6.0 / 7)
  }

  def drawInBox(ctx: CanvasRenderingContext2D, ptype: Ptype, numPieces: Int): Unit = {
    val left = layout.pieceBox.left + layout.PIECE_WIDTH * ptype.sortId
    val top = layout.pieceBox.top
    drawPiece(ctx, Piece(BLACK, ptype), left, top)
    drawNumbers(ctx, numPieces, left, top, rotated = false)
  }
}
