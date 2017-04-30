package com.mogproject.mogami.playground.view

import com.mogproject.mogami._
import com.mogproject.mogami.playground.api.Clipboard
import com.mogproject.mogami.playground.api.Clipboard.Event
import com.mogproject.mogami.playground.controller._
import com.mogproject.mogami.playground.controller.mode.Mode
import com.mogproject.mogami.playground.view.bootstrap.Tooltip
import com.mogproject.mogami.playground.view.modal._

// todo: don't use parts directly but use only sections
import com.mogproject.mogami.playground.view.parts.edit.EditReset
import com.mogproject.mogami.playground.view.parts.language.{MessageLanguageSelector, PieceLanguageSelector, RecordLanguageSelector}
import com.mogproject.mogami.playground.view.parts.manage.SaveLoadButton
import com.mogproject.mogami.playground.view.parts.navigator.FlipButton
import com.mogproject.mogami.playground.view.parts.share._
import com.mogproject.mogami.playground.view.section._
import com.mogproject.mogami.util.Implicits._
import com.mogproject.mogami.util.MapUtil
import org.scalajs.dom
import org.scalajs.dom.html.{Canvas, Div}
import org.scalajs.dom.raw.UIEvent
import org.scalajs.dom.{CanvasRenderingContext2D, Element}

import scalatags.JsDom.all._

/**
  * controls canvas rendering
  */
case class Renderer(elem: Element, layout: Layout) extends CursorManageable {

  // main canvas
  protected val canvas0: Canvas = createCanvas(0)
  protected val canvas1: Canvas = createCanvas(1)
  protected val canvas2: Canvas = createCanvas(2)
  protected val canvas3: Canvas = createCanvas(3)
  protected val canvas4: Canvas = createCanvas(4)
  protected val canvases: List[Canvas] = List(canvas0, canvas1, canvas2, canvas3, canvas4)

  protected val layer0: CanvasRenderingContext2D = canvas0.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
  protected val layer1: CanvasRenderingContext2D = canvas1.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
  protected val layer2: CanvasRenderingContext2D = canvas2.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
  protected val layer3: CanvasRenderingContext2D = canvas3.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
  protected val layer4: CanvasRenderingContext2D = canvas4.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

  // elements
  private[this] val canvasContainer: Div = div(
    padding := 0,
    height := layout.canvasHeightCompact,
    canvases
  ).render

  private[this] val navigatorSection = NavigatorSection(layout)

  private[this] lazy val controlSection = ControlSection(layout.canvasWidth, layout.isMobile)

  private[this] lazy val mainPane = div(
    div(cls := "navbar",
      tag("nav")(cls := "navbar navbar-default navbar-fixed-top", navigatorSection.output)
    ),
    div(cls := "container",
      layout.isMobile.fold(Seq(position := position.fixed.v, width := "100%"), ""),
      div(cls := "row",
        div(cls := "col-sm-7 col-md-6 col-lg-5", paddingLeft := 0, paddingRight := 0,
          div(margin := "auto", padding := 0, width := layout.canvasWidth,
            canvasContainer,
            controlSection.output
          )
        ),
        div(cls := "col-sm-5 col-md-6 col-lg-7 hidden-xs", paddingLeft := 0,
          div(cls := "row",
            div(cls := "col-md-4 col-lg-3 hidden-sm", paddingLeft := 0, controlSection.outputLongSelector),
            div(cls := "col-md-8 col-lg-9", MenuPane.output)
          )
        )
      ),
      hr(),
      small(p(textAlign := "right", "Shogi Playground © 2017 ", a(href := "http://mogproject.com", target := "_blank", "mogproject")))
    )
  ).render

  initialize()

  private[this] def initialize(): Unit = {
    elem.appendChild(mainPane)

    // register events to the canvas
    if (hasTouchEvent) {
      setEventListener("touchstart", touchStart)
      setEventListener("touchend", touchEnd)
      setEventListener("touchcancel", { _: UIEvent => clearHoldEvent() })
    } else {
      setEventListener("mousemove", mouseMove)
      setEventListener("mousedown", mouseDown)
      setEventListener("mouseup", mouseUp)
      setEventListener("mouseout", { _: UIEvent => clearHoldEvent() })
    }

    navigatorSection.initialize()
    controlSection.initialize()
    MenuPane.initialize()

    // initialize clipboard.js
    val cp = new Clipboard(".btn")
    cp.on("success", (e: Event) => Tooltip.display(e.trigger, "Copied!"))
    cp.on("error", (e: Event) => Tooltip.display(e.trigger, "Failed!"))

    // initialize tooltips
    Tooltip.enableHoverToolTip(layout)
  }

  private[this] def createCanvas(zIndexVal: Int): Canvas = {
    canvas(
      widthA := layout.canvasWidth,
      heightA := layout.canvasHeightCompact,
      marginLeft := "auto",
      marginRight := "auto",
      left := 0,
      right := 0,
      top := 0,
      zIndex := zIndexVal
    ).render
  }

  def setEventListener[A](eventType: String, f: A => Unit): Unit = canvasContainer.addEventListener(eventType, f, useCapture = false)

  def expandCanvas(): Unit = {
    canvasContainer.style.height = layout.canvasHeight + "px"
    canvases.foreach(_.height = layout.canvasHeight)
  }

  def contractCanvas(): Unit = {
    canvasContainer.style.height = layout.canvasHeightCompact + "px"
    canvases.foreach(_.height = layout.canvasHeightCompact)
  }

  def drawBoard(): Unit = {
    layout.board.draw(layer1)
    layout.handWhite.draw(layer1)
    layout.handBlack.draw(layer1)
    layout.playerBlack.draw(layer1)
    layout.playerWhite.draw(layer1)

    for (i <- 1 to 8) {
      val x = layout.board.left + layout.PIECE_WIDTH * i
      val y = layout.board.top + layout.PIECE_HEIGHT * i

      Line(x, layout.board.top, x, layout.board.bottom).draw(layer1)
      Line(layout.board.left, y, layout.board.right, y).draw(layer1)

      if (i % 3 == 0) {
        Circle(x, layout.board.top + layout.PIECE_HEIGHT * 3, layout.DOT_SIZE).draw(layer1)
        Circle(x, layout.board.top + layout.PIECE_HEIGHT * 6, layout.DOT_SIZE).draw(layer1)
      }
    }
  }

  def drawPlayerIcon(config: Configuration): Unit = {
    val (b, w) = config.flip.fold((layout.color.white, layout.color.fg), (layout.color.fg, layout.color.white))
    val ctx = layer2

    // clear
    clearPlayerIcon()

    // draw
    List((w, layout.playerIconWhite, true), (b, layout.playerIconBlack, false)).foreach { case (c, r, rot) =>
      TextRenderer(ctx, "☗", layout.font.playerIcon, c, r.left, r.top, r.width, r.height)
        .alignCenter.alignMiddle.withRotate(rot).withStroke(layout.color.fg, layout.strokeSize).render()
    }
  }

  def clearPlayerIcon(): Unit = {
    val ctx = layer2
    layout.playerIconWhite.clear(ctx)
    layout.playerIconBlack.clear(ctx)
  }

  def drawPlayerNames(config: Configuration, blackName: String, whiteName: String): Unit = {
    drawPlayerIcon(config)

    val ctx = layer0

    // clear
    clearPlayerNames()

    // draw
    List(
      (config.flip.fold(whiteName, blackName), layout.playerNameBlack, false),
      (config.flip.fold(blackName, whiteName), layout.playerNameWhite, true)).foreach { case (t, r, rot) =>
      TextRenderer(ctx, t, layout.font.playerName, layout.color.fg, r.left, r.top, r.width, r.height)
        .withTrim.alignLeft.alignMiddle.withRotate(rot).render()
    }
  }

  def clearPlayerNames(): Unit = {
    val ctx = layer0
    layout.playerNameWhite.clear(ctx, -1)
    layout.playerNameBlack.clear(ctx, -1)
  }

  def drawIndexes(config: Configuration): Unit = {
    val ctx = layer3
    val fileIndex = "１２３４５６７８９"
    val rankIndex = config.recordLang match {
      case Japanese => "一二三四五六七八九"
      case English => "abcdefghi"
    }

    // clear
    layout.fileIndex.clear(ctx)
    layout.rankIndex.clear(ctx)

    // file
    for (i <- 0 until 9) {
      val text = fileIndex.charAt(config.flip.fold(8 - i, i)).toString
      val left = layout.fileIndex.left + layout.PIECE_WIDTH * (8 - i)
      val top = layout.fileIndex.top

      TextRenderer(ctx, text, layout.font.numberIndex, layout.color.fg, left, top, layout.PIECE_WIDTH, layout.fileIndex.height)
        .alignCenter.alignMiddle.render()
    }

    //rank
    for (i <- 0 until 9) {
      val text = rankIndex.charAt(config.flip.fold(8 - i, i)).toString
      val left = layout.rankIndex.left
      val top = layout.rankIndex.top + layout.PIECE_HEIGHT * i
      TextRenderer(ctx, text, layout.font.numberIndex, layout.color.fg, left, top, layout.rankIndex.width, layout.PIECE_HEIGHT)
        .alignCenter.alignMiddle.render()
    }
  }

  private[this] def drawPieces(config: Configuration, board: BoardType, hand: HandType): Unit = {
    val pr = config.pieceRenderer

    clearPieces()
    board.foreach { case (sq, pc) => pr.drawOnBoard(layer2, config.flip.when[Piece](!_)(pc), config.flip.when[Square](!_)(sq)) }
    hand.withFilter(_._2 > 0).foreach { case (pc, n) => pr.drawInHand(layer2, config.flip.when[Hand](!_)(pc), n) }
  }

  def drawPieces(config: Configuration, state: State): Unit = drawPieces(config, state.board, state.hand)

  /**
    * Draw illegal state
    */
  def drawIllegalStatePieces(config: Configuration, state: State, move: Move): Unit = {
    val releaseBoard: BoardType => BoardType = move.from.when(sq => b => b - sq)
    val releaseHand: HandType => HandType = move.isDrop.when(MapUtil.decrementMap(_, Hand(move.newPiece)))
    val obtainHand: HandType => HandType = move.capturedPiece.when(p => h => MapUtil.incrementMap(h, Hand(!p.demoted)))

    val board = releaseBoard(state.board) + (move.to -> move.newPiece)
    val hand = (releaseHand andThen obtainHand) (state.hand)
    drawPieces(config, board, hand)
  }

  def clearPieces(): Unit = {
    layout.board.clear(layer2)
    layout.handWhite.clear(layer2, -4)
    layout.handBlack.clear(layer2, -4)
  }

  def drawEditingPieces(config: Configuration, board: BoardType, hand: HandType, box: Map[Ptype, Int]): Unit = {
    val pr = config.pieceRenderer

    clearPieces()
    clearPiecesInBox()
    board.foreach { case (sq, pc) => pr.drawOnBoard(layer2, config.flip.when[Piece](!_)(pc), config.flip.when[Square](!_)(sq)) }
    hand.withFilter(_._2 > 0).foreach { case (pc, n) => pr.drawInHand(layer2, config.flip.when[Hand](!_)(pc), n) }
    box.withFilter(_._2 > 0).foreach { case (pt, n) => pr.drawInBox(layer2, pt, n) }
  }

  def clearPiecesInBox(): Unit = {
    layout.pieceBox.clear(layer2, -4)
  }

  def drawIndicators(config: Configuration, turn: Player, status: GameStatus): Unit = {
    val t = config.flip.fold(!turn, turn) // flip turn

    def f(r: Rectangle, text: String, rotated: Boolean): Unit = {
      TextRenderer(layer0, text, layout.font.indicator, layout.color.white, r.left, r.top, r.width, r.height)
        .alignCenter.alignMiddle.withRotate(rotated).render()
    }

    def drawWinLose(turnWins: Boolean): Unit = {
      val winner = (t.isBlack ^ turnWins).fold(layout.indicatorWhite, layout.indicatorBlack)
      val loser = (t.isBlack ^ turnWins).fold(layout.indicatorBlack, layout.indicatorWhite)
      winner.drawFill(layer0, layout.color.win)
      f(winner, "WIN", t.isBlack ^ turnWins)
      loser.drawFill(layer0, layout.color.lose)
      f(loser, "LOSE", t.isWhite ^ turnWins)
    }

    status match {
      case GameStatus.Playing =>
        (if (t.isBlack) layout.indicatorWhite else layout.indicatorBlack).clear(layer0, -1)
        val r = if (t.isBlack) layout.indicatorBlack else layout.indicatorWhite
        r.drawFill(layer0, layout.color.active)
        f(r, "TURN", t.isWhite)
      case GameStatus.Mated | GameStatus.Resigned | GameStatus.TimedUp | GameStatus.IllegallyMoved => drawWinLose(false)
      case GameStatus.PerpetualCheck | GameStatus.Uchifuzume => drawWinLose(true)
      case GameStatus.Drawn =>
        layout.indicatorWhite.drawFill(layer0, layout.color.draw)
        layout.indicatorBlack.drawFill(layer0, layout.color.draw)
        f(layout.indicatorBlack, "DRAW", rotated = false)
        f(layout.indicatorWhite, "DRAW", rotated = true)
    }
  }

  def drawPieceBox(): Unit = {
    val r = layout.pieceBox
    r.draw(layer1, layout.color.pieceBox, 3)
    TextRenderer(layer1, "UNUSED PIECES", layout.font.pieceBoxLabel, layout.color.fg, r.left, r.top - layout.MARGIN_BLOCK, r.width, layout.MARGIN_BLOCK * 3 / 2)
      .alignCenter.alignMiddle.render()
  }

  def hidePieceBox(): Unit = {
    layout.pieceBox.clear(layer1, -3)
    clearPiecesInBox()
  }

  def getImageBase64: String = {
    val c = canvases.head

    // create a hidden canvas
    val hiddenCanvas: Canvas = canvas(
      widthA := c.width,
      heightA := c.height
    ).render

    val hiddenContext: CanvasRenderingContext2D = hiddenCanvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

    // set background as white
    hiddenContext.fillStyle = "#ffffff"
    hiddenContext.fillRect(0, 0, c.width, c.height)

    // copy layers
    canvases.foreach(src => hiddenContext.drawImage(src, 0, 0))

    // export image
    hiddenCanvas.toDataURL("image/png")
  }

  /**
    * Display
    */
  def drawAsImage(): Unit = {
    dom.window.document.body.style.backgroundColor = "black"

    val t = "Snapshot - Shogi Playground"
    val base64 = getImageBase64
    val elem = a(attr("download") := "snapshot.png", title := t, href := base64, img(alt := t, src := base64))
    dom.window.document.body.innerHTML = elem.toString
  }

  def showEditSection(): Unit = EditSection.show()

  def hideEditSection(): Unit = EditSection.hide()

  def showControlSection(): Unit = List(controlSection, GameMenuSection, GameHelpSection).foreach(_.show())

  def hideControlSection(): Unit = List(controlSection, GameMenuSection, GameHelpSection).foreach(_.hide())

  def askPromote(config: Configuration, piece: Piece, callbackUnpromote: () => Unit, callbackPromote: () => Unit): Unit = {
    PromotionDialog(config, piece, callbackUnpromote, callbackPromote).show()
  }

  def askConfirm(lang: Language, callback: () => Unit): Unit = {
    val s = lang match {
      case Japanese => p("棋譜の情報が失われますが、よろしいですか?")
      case English => p("The record will be discarded. Are you sure?")
    }
    YesNoDialog(lang, s, callback).show()
  }

  def askDeleteBranch(lang: Language, branchNo: BranchNo, callback: () => Unit): Unit = {
    val s = lang match {
      case Japanese => p(s"現在の変化 (Branch#${branchNo}) が削除されます。コメントも失われますが、よろしいですか?")
      case English => p(s"Branch#${branchNo} will be deleted. Comments on this branch will also be removed.\n\nAre you sure?")
    }
    YesNoDialog(lang, s, callback).show()
  }

  def alertEditedState(msg: String, lang: Language): Unit = {
    val s = lang match {
      case Japanese => p("不正な局面です。", br, s"(${msg})")
      case English => p("Invalid state.", br, s"(${msg})")
    }
    AlertDialog(lang, s).show()
  }

  def showMenuModal(): Unit = MenuDialog.show(layout)

  def hideMenuModal(timeout: Double): Unit = dom.window.setTimeout({ () => MenuDialog.hide() }, timeout)

  def showGameInfoModal(config: Configuration, gameInfo: GameInfo): Unit = GameInfoDialog(config, gameInfo).show()

  // share section
  def updateSnapshotUrl(url: String): Unit = SnapshotCopyButton.updateValue(url)

  def getSnapshotUrl: String = SnapshotCopyButton.getValue

  def updateSnapshotShortUrl(url: String, completed: Boolean): Unit = SnapshotShortenButton.updateValue(url, completed)

  def updateRecordUrl(url: String): Unit = RecordCopyButton.updateValue(url)

  def getRecordUrl: String = RecordCopyButton.getValue

  def updateRecordShortUrl(url: String, completed: Boolean): Unit = RecordShortenButton.updateValue(url, completed)

  def updateImageLinkUrl(url: String): Unit = ImageLinkButton.updateValue(url)

  def updateSfenString(sfen: String): Unit = SfenStringCopyButton.updateValue(sfen)

  def updateCommentOmissionWarning(displayWarning: Boolean): Unit = GameMenuSection.updateCommentOmissionWarning(displayWarning)

  // navigator section
  def updateMode(mode: Mode): Unit = navigatorSection.updateMode(mode)

  def updateFlip(config: Configuration): Unit = FlipButton.updateValue(config.flip)

  // record
  def updateRecordContent(game: Game, branchNo: BranchNo, lng: Language): Unit = controlSection.updateRecordContent(game, branchNo, lng)

  def updateRecordIndex(index: Int): Unit = controlSection.updateRecordIndex(index)

  def getRecordIndex(index: Int): Int = controlSection.getRecordIndex(index)

  def getMaxRecordIndex: Int = controlSection.getMaxRecordIndex

  def getSelectedIndex: Int = controlSection.getSelectedIndex

  // control section
  def updateControlBar(backwardEnabled: Boolean, forwardEnabled: Boolean): Unit =
    controlSection.updateLabels(backwardEnabled: Boolean, forwardEnabled: Boolean)

  def updateEditResetLabel(lang: Language): Unit = EditReset.updateLabel(lang)

  def updateComment(text: String): Unit = controlSection.updateComment(text)

  def showCommentModal(config: Configuration): Unit = CommentDialog(config, controlSection.getComment).show()

  // languages
  def updateMessageLang(lang: Language): Unit = MessageLanguageSelector.updateValue(lang)

  def updateRecordLang(lang: Language): Unit = RecordLanguageSelector.updateValue(lang)

  def updatePieceLang(lang: Language): Unit = PieceLanguageSelector.updateValue(lang)

  // tooltip messages
  def displayFileLoadMessage(message: String): Unit = SaveLoadButton.displayFileLoadMessage(message)

  def displayFileLoadTooltip(message: String): Unit = SaveLoadButton.displayFileLoadTooltip(message)

  def displayTextLoadMessage(message: String): Unit = SaveLoadButton.displayTextLoadMessage(message)

  def displayTextLoadTooltip(message: String): Unit = SaveLoadButton.displayTextLoadTooltip(message)

  // branch section
  def updateBranchButtons(game: Game, gamePosition: GamePosition, language: Language): Unit = GameMenuSection.updateBranchButtons(game, gamePosition, language)

  def showBranchEditMenu(): Unit = GameMenuSection.showBranchEditMenu()

  def hideBranchEditMenu(): Unit = GameMenuSection.hideBranchEditMenu()

  def getIsNewBranchMode: Boolean = GameMenuSection.getIsNewBranchMode

  // action section
  def showActionSection(): Unit = ActionSection.show()

  def hideActionSection(): Unit = ActionSection.hide()

  def updateActionSection(lang: Language, canResign: Boolean): Unit = {
    // @note setTimeout is necessary for data-dismiss modal closing
    dom.window.setTimeout(() => ActionSection.update(lang, canResign), 0)
  }
}
