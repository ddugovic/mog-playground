package com.mogproject.mogami.playground.controller.mode

import com.mogproject.mogami.{HandType, BoardType, _}
import com.mogproject.mogami.playground.controller._
import com.mogproject.mogami.util.MapUtil
import com.mogproject.mogami.util.Implicits._
import com.mogproject.mogami.core.state.StateCache.Implicits._
import com.mogproject.mogami.playground.view.renderer.Renderer

import scala.util.{Failure, Success, Try}

/**
  * Edit mode
  */
case class EditModeController(renderer: Renderer,
                              config: Configuration,
                              turn: Player,
                              board: BoardType,
                              hand: HandType,
                              box: Map[Ptype, Int],
                              override val gameInfo: GameInfo,
                              lastCursorInvoked: Option[Cursor] = None
                             ) extends ModeController {

  val mode: Mode = Editing

  override def initialize(): Unit = {
    super.initialize()
    renderer.hideControlSection()
    renderer.expandCanvas()
    renderer.showEditSection()
    renderer.updateRecordContent(Game(), 0, config.recordLang)
    renderer.drawPieceBox()
    renderAll()
  }

  override def terminate(): Unit = {
    super.terminate()
    renderer.showControlSection()
    renderer.hideEditSection()
    //    renderer.hidePieceBox() // unnecessary
    renderer.contractCanvas()
    renderer.drawBoard()
  }

  /**
    * Set new config
    */
  override def updateConfig(config: Configuration): ModeController = this.copy(config = config)

  override def updateGameInfo(gameInfo: GameInfo): ModeController = this.copy(gameInfo = gameInfo)

  override def renderAll(): Unit = {
    super.renderAll()

    renderer.updateEditResetLabel(config.messageLang)

    renderer.drawIndicators(turn, GameStatus.Playing)
    renderer.drawEditingPieces(board, hand, box)
  }

  override def initializeBoardControl(): Unit = {
    renderer.hideControlSection()
    renderer.expandCanvas()
    renderer.drawPieceBox()
    renderAll()
  }

  override def startMoveAnimation(): Unit = {
    renderer.startMoveAction(lastCursorInvoked)
  }

  override def canActivate(cursor: Cursor): Boolean = true

  override def canSelect(cursor: Cursor): Boolean = cursor match {
    case Cursor(Some(sq), None, None, None) => board.contains(sq)
    case Cursor(None, Some(h), None, None) => hand(h) > 0
    case Cursor(None, None, Some(pt), None) => box(pt) > 0
    case _ => false
  }

  /**
    * Exchange action in Edit Mode
    *
    * @param selected from
    * @param invoked  to
    */
  override def invokeCursor(selected: Cursor, invoked: Cursor, isFlipped: Boolean): Option[ModeController] = {
    (selected, invoked) match {
      // square is selected
      case (Cursor(Some(s1), None, None, None), Cursor(Some(s2), None, None, None)) =>
        (board(s1), board.get(s2)) match {
          case (p1, Some(p2)) if s1 == s2 =>
            // change piece attributes
            Some(this.copy(board = board.updated(s1, p1.canPromote.fold(p1.promoted, !p1.demoted)), lastCursorInvoked = Some(invoked)))
          case (p1, Some(p2)) =>
            // change pieces
            Some(this.copy(board = board.updated(s1, p2).updated(s2, p1), lastCursorInvoked = Some(invoked)))
          case (p1, None) =>
            // move to an empty square
            Some(this.copy(board = board.updated(s2, p1) - s1, lastCursorInvoked = Some(invoked)))
        }
      case (Cursor(Some(s), None, None, None), Cursor(None, Some(h), None, None)) if board(s).ptype != KING =>
        // board to hand
        val pt = board(s).ptype.demoted
        val newHand = Hand(h.owner, pt)
        Some(this.copy(board = board - s, hand = MapUtil.incrementMap(hand, newHand), lastCursorInvoked = Some(Cursor(newHand))))
      case (Cursor(Some(s), None, None, None), Cursor(None, None, Some(_), None)) =>
        // board to box
        val pt = board(s).ptype.demoted
        Some(this.copy(board = board - s, box = MapUtil.incrementMap(box, pt), lastCursorInvoked = Some(Cursor(pt))))

      // hand is selected
      case (Cursor(None, Some(h), None, None), Cursor(Some(s), None, None, None)) if !board.get(s).exists(_.ptype == KING) =>
        // hand to board
        val hx = MapUtil.decrementMap(hand, h)
        val hy = board.get(s).map { p => MapUtil.incrementMap(hx, Hand(h.owner, p.ptype.demoted)) }.getOrElse(hx)
        Some(this.copy(board = board.updated(s, h.toPiece), hand = hy, lastCursorInvoked = Some(invoked)))
      case (Cursor(None, Some(h1), None, None), Cursor(None, Some(h2), None, None)) if h1.owner != h2.owner =>
        // hand to hand
        val hx = MapUtil.decrementMap(hand, h1)
        val hy = MapUtil.incrementMap(hx, Hand(!h1.owner, h1.ptype))
        val newHand = Hand(!h1.owner, h1.ptype)
        Some(this.copy(hand = hy, lastCursorInvoked = Some(Cursor(newHand))))
      case (Cursor(None, Some(h), None, None), Cursor(None, None, Some(_), None)) =>
        // hand to box
        Some(this.copy(hand = MapUtil.decrementMap(hand, h), box = MapUtil.incrementMap(box, h.ptype), lastCursorInvoked = Some(Cursor(h.ptype))))

      // box is selected
      case (Cursor(None, None, Some(pt), None), Cursor(Some(s), None, None, None)) =>
        // box to board
        val bx = MapUtil.decrementMap(box, pt)
        val by = board.get(s).map { p => MapUtil.incrementMap(bx, p.ptype.demoted) }.getOrElse(bx)
        Some(this.copy(board = board.updated(s, Piece(Player.BLACK, pt)), box = by, lastCursorInvoked = Some(invoked)))
      case (Cursor(None, None, Some(pt), None), Cursor(None, Some(h), None, None)) if pt != KING =>
        // box to hand
        val newHand = Hand(h.owner, pt)
        Some(this.copy(hand = MapUtil.incrementMap(hand, newHand), box = MapUtil.decrementMap(box, pt), lastCursorInvoked = Some(Cursor(newHand))))

      // player is clicked
      case (_, Cursor(None, None, None, Some(p))) =>
        Some(this.copy(turn = p, lastCursorInvoked = None))
      case _ => None
    }
  }

  /**
    * Mouse up event in Edit Mode
    *
    * @param selected from
    * @param released to
    * @return
    */
  override def processMouseUp(selected: Cursor, released: Cursor): Option[Cursor] = (!selected.isPlayer).option(released)

  //
  // Actions
  //
  override def setMode(nextMode: Mode): Option[ModeController] = if (nextMode != Editing) {
    Try(State(turn, board, hand, None)) match {
      case Success(st) =>
        nextMode match {
          case Playing => Some(PlayModeController(renderer, config, Game(Branch(st), gameInfo = gameInfo), 0, 0))
          case Viewing => Some(ViewModeController(renderer, config, Game(Branch(st), gameInfo = gameInfo), 0, 0))
          case Editing => None
        }
      case Failure(e) =>
        renderer.alertEditedState(e.getMessage, config.messageLang)
        None
    }
  } else None

  override def toggleFlip(): Option[ModeController] = Some(this.copy(config = config.copy(flip = !config.flip)))

  override def setEditTurn(player: Player): Option[ModeController] =
    (player != turn).option(this.copy(turn = player))

  override def setEditInitialState(initialState: State, isHandicap: Boolean): Option[ModeController] = {
    Some(this.copy(
      turn = initialState.turn,
      board = initialState.board,
      hand = initialState.hand,
      box = initialState.unusedPtypeCount,
      gameInfo = isHandicap.fold(GameInfo(Map(
        'blackName -> handicapNames((config.recordLang, BLACK)),
        'whiteName -> handicapNames((config.recordLang, WHITE))
      )), GameInfo())
    ))
  }

}
