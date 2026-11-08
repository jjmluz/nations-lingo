package com.jjmluz.nationslingo

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val Background = Color(0xFF0B1220)
private val Foreground = Color(0xFFF4F6FB)
private val Soft = Color(0xFFC9D0DE)
private val Muted = Color(0xFFA5AFC3)
private val Line = Color(0xFF3A4862)
private val Accent = Color(0xFFFFC83D)
private val Good = Color(0xFF4ADE80)
private val Bad = Color(0xFFFF5A5F)
private val Card = Color(0xFF1C2A44)

private const val TICK_MILLIS = 50L
private const val WALL_COLUMNS = 12
private const val WALL_ROWS = 9

/** How an answer has been marked: the right one, or one already ruled out. */
private enum class Mark { NONE, RIGHT, WRONG }

@Composable
fun NationsLingoApp(viewModel: GameViewModel) {
    val wall = rememberFlagWall()
    Box(Modifier.fillMaxSize().background(Background)) {
        when (viewModel.screen) {
            Screen.START -> StartScreen(viewModel, wall)
            Screen.PLAYING -> PlayingScreen(viewModel)
            Screen.PAUSED -> PausedScreen(viewModel)
            Screen.OVER -> OverScreen(viewModel)
        }
    }
}

/**
 * Small copies of the title screen's flags, decoded once in the background. Full-size flags would take
 * tens of megabytes for a wall that sits behind a dark scrim.
 */
@Composable
private fun rememberFlagWall(): List<ImageBitmap> {
    val resources = LocalResources.current
    val wall by produceState(emptyList<ImageBitmap>(), resources) {
        value = withContext(Dispatchers.Default) { decodeWall(resources) }
    }
    return wall
}

private fun decodeWall(resources: Resources): List<ImageBitmap> {
    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
    return COUNTRIES.shuffled(Random(7)).take(WALL_COLUMNS * WALL_ROWS).map {
        BitmapFactory.decodeResource(resources, it.flag, options).asImageBitmap()
    }
}

@Composable
private fun StartScreen(viewModel: GameViewModel, wall: List<ImageBitmap>) {
    Box(Modifier.fillMaxSize()) {
        FlagWall(wall)
        Box(Modifier.fillMaxSize().background(Background.copy(alpha = 0.84f)))
        Message {
            BasicText(
                text = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    color = Foreground,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-1).sp,
                ),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 40.sp, maxFontSize = 92.sp),
            )
            Spacer(Modifier.height(18.dp))
            Mode.entries.forEach { ModeButton(it, it == viewModel.mode, viewModel.best(it)) }
            Spacer(Modifier.height(18.dp))
            Label(stringResource(viewModel.mode.hint), 19.sp, Soft)
        }
    }
}

/** A tilted wall of flags filling the screen, wider than it so the tilt leaves no gaps at the corners. */
@Composable
private fun FlagWall(flags: List<ImageBitmap>) {
    if (flags.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds(), Alignment.Center) {
        val gap = maxWidth / 70
        val flagWidth = (maxWidth * 1.35f - gap * (WALL_COLUMNS - 1)) / WALL_COLUMNS
        val shape = RoundedCornerShape(6.dp)
        Column(
            modifier = Modifier.wrapContentSize(unbounded = true).rotate(-8f),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            repeat(WALL_ROWS) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    repeat(WALL_COLUMNS) { column ->
                        Image(
                            bitmap = flags[(row * WALL_COLUMNS + column) % flags.size],
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.size(flagWidth, flagWidth * 3 / 4).clip(shape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeButton(mode: Mode, selected: Boolean, best: Int) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .size(540.dp, 68.dp)
            .clip(shape)
            .background(if (selected) Accent else Card.copy(alpha = 0.9f))
            .then(if (selected) Modifier else Modifier.border(2.dp, Line, shape))
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(
            text = stringResource(mode.title),
            size = 26.sp,
            color = if (selected) Background else Foreground,
            weight = if (selected) FontWeight.Black else FontWeight.Bold,
        )
        if (best > 0) {
            Label(
                text = bestLabel(best),
                size = 17.sp,
                color = if (selected) Background else Muted,
                weight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

/** The board stays, with every flag and name hidden so the paused clock can't be used to study them. */
@Composable
private fun PausedScreen(viewModel: GameViewModel) {
    Board(viewModel, hidden = true)
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Box(Modifier.clip(RoundedCornerShape(28.dp)).background(Background).padding(10.dp)) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .widthIn(min = 360.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Card)
                    .padding(horizontal = 32.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    PauseIcon(Modifier.size(34.dp))
                    Label(stringResource(R.string.paused), 44.sp, Foreground, FontWeight.Black)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    OkKey()
                    Label(stringResource(R.string.continue_game), 24.sp, Foreground, FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    BackKey()
                    Label(stringResource(R.string.quit_to_menu), 22.sp, Muted)
                }
            }
        }
    }
}

@Composable
private fun PauseIcon(modifier: Modifier) = Canvas(modifier) {
    val unit = size.width / 24
    for (left in listOf(5f, 14f)) {
        drawRoundRect(
            color = Accent,
            topLeft = Offset(left * unit, 4 * unit),
            size = Size(5 * unit, 16 * unit),
            cornerRadius = CornerRadius(1.5f * unit),
        )
    }
}

/** The remote's OK button. */
@Composable
private fun OkKey() {
    Box(Modifier.size(40.dp).clip(CircleShape).background(Accent), Alignment.Center) {
        Label("OK", 13.sp, Background, FontWeight.Black)
    }
}

/** The remote's Back button, drawn with the usual return arrow. */
@Composable
private fun BackKey() {
    Box(Modifier.size(40.dp).border(2.dp, Muted, CircleShape), Alignment.Center) {
        Canvas(Modifier.size(20.dp)) {
            val unit = size.width / 24
            val arrow = Path().apply {
                moveTo(9 * unit, 6 * unit)
                lineTo(4 * unit, 11 * unit)
                lineTo(9 * unit, 16 * unit)
                moveTo(4 * unit, 11 * unit)
                lineTo(14 * unit, 11 * unit)
                arcTo(Rect(9 * unit, 11 * unit, 19 * unit, 21 * unit), -90f, 180f, false)
                lineTo(11 * unit, 21 * unit)
            }
            drawPath(arrow, Muted, style = Stroke(2.6f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun OverScreen(viewModel: GameViewModel) {
    val guessedAll = viewModel.score == COUNTRIES.size
    Message {
        Label(stringResource(if (guessedAll) R.string.guessed_all else R.string.time_up), 40.sp, Muted, FontWeight.Bold)
        Label(viewModel.score.toString(), 120.sp, Foreground, FontWeight.Bold)
        Label(pluralStringResource(R.plurals.countries_guessed, viewModel.score), 28.sp, Foreground)
        if (viewModel.isNewBest) {
            Label(stringResource(R.string.new_best), 28.sp, Accent, FontWeight.Bold)
        } else {
            Label(bestLabel(viewModel.best(viewModel.mode)), 24.sp, Muted)
        }
        Spacer(Modifier.height(8.dp))
        Label(stringResource(R.string.press_ok_to_play_again), 32.sp, Accent, FontWeight.Bold)
        Label(stringResource(R.string.press_back_for_menu), 24.sp, Muted)
    }
}

@Composable
private fun PlayingScreen(viewModel: GameViewModel) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MILLIS)
            viewModel.tick()
        }
    }
    Board(viewModel, hidden = false)
}

/** The score, the clock and the round. With [hidden], the answers show as empty outlines. */
@Composable
private fun Board(viewModel: GameViewModel, hidden: Boolean) {
    val round = viewModel.round ?: return

    // Margins keep everything inside the TV's overscan-safe area.
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
        Hud(viewModel, paused = hidden)
        when (viewModel.mode) {
            Mode.GUESS_FLAG -> GuessFlagRound(round, viewModel.rightAnswer, hidden, maxHeight)
            Mode.GUESS_COUNTRY -> GuessCountryRound(round, viewModel.rightAnswer, hidden, maxWidth, maxHeight)
        }
    }
}

private fun Round.markOf(direction: Direction, rightAnswer: Direction?) = when (direction) {
    rightAnswer -> Mark.RIGHT
    in eliminated -> Mark.WRONG
    else -> Mark.NONE
}

/** The country's name in the middle, with a flag on each side. */
@Composable
private fun BoxScope.GuessFlagRound(round: Round, rightAnswer: Direction?, hidden: Boolean, availableHeight: Dp) {
    val flagHeight = availableHeight * 0.3f
    val flagWidth = flagHeight * 4 / 3

    @Composable
    fun FlagAt(direction: Direction, modifier: Modifier = Modifier) = Flag(
        country = round.options.getValue(direction),
        mark = round.markOf(direction, rightAnswer),
        dimmed = rightAnswer != null && direction != rightAnswer,
        hidden = hidden,
        width = flagWidth,
        height = flagHeight,
        modifier = modifier,
    )

    FlagAt(Direction.UP, Modifier.align(Alignment.TopCenter))
    FlagAt(Direction.DOWN, Modifier.align(Alignment.BottomCenter))
    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
        FlagAt(Direction.LEFT)
        Box(Modifier.size(flagWidth * 2, flagHeight).padding(horizontal = 16.dp), Alignment.Center) {
            if (!hidden) {
                BasicText(
                    text = stringResource(round.target.name),
                    style = TextStyle(color = Foreground, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    autoSize = WholeWordsAutoSize(min = 20.sp, max = 40.sp),
                )
            }
        }
        FlagAt(Direction.RIGHT)
    }
}

/** The country's flag in the middle, with a name on each side. */
@Composable
private fun BoxScope.GuessCountryRound(
    round: Round,
    rightAnswer: Direction?,
    hidden: Boolean,
    availableWidth: Dp,
    availableHeight: Dp,
) {
    val flagHeight = availableHeight * 0.52f
    val flagWidth = flagHeight * 4 / 3
    val nameWidth = (availableWidth - flagWidth) / 2 - 12.dp

    @Composable
    fun NameAt(direction: Direction, height: Dp, modifier: Modifier = Modifier) = NameOption(
        name = stringResource(round.options.getValue(direction).name),
        mark = round.markOf(direction, rightAnswer),
        dimmed = rightAnswer != null && direction != rightAnswer,
        hidden = hidden,
        width = nameWidth,
        height = height,
        modifier = modifier,
    )

    val topHeight = availableHeight * 0.2f
    NameAt(Direction.UP, topHeight, Modifier.align(Alignment.TopCenter))
    NameAt(Direction.DOWN, topHeight, Modifier.align(Alignment.BottomCenter))
    Row(
        modifier = Modifier.align(Alignment.Center),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NameAt(Direction.LEFT, topHeight * 1.4f)
        Flag(round.target, Mark.NONE, dimmed = false, hidden = hidden, width = flagWidth, height = flagHeight)
        NameAt(Direction.RIGHT, topHeight * 1.4f)
    }
}

@Composable
private fun Hud(viewModel: GameViewModel, paused: Boolean) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.TopStart)) {
            Label(stringResource(R.string.score), 18.sp, Muted, FontWeight.Bold)
            Label(viewModel.score.toString(), 56.sp, Foreground, FontWeight.Bold)
        }
        Column(Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
            Label(stringResource(R.string.time), 18.sp, Muted, FontWeight.Bold)
            val seconds = viewModel.secondsLeft
            Label(
                text = "%d:%02d".format(seconds / 60, seconds % 60),
                size = 56.sp,
                color = when {
                    paused -> Accent
                    seconds <= LOW_TIME_SECONDS -> Bad
                    else -> Foreground
                },
                weight = FontWeight.Bold,
            )
            if (!paused) viewModel.timeChange?.let { TimeChangeBadge(it) }
        }
    }
}

@Composable
private fun TimeChangeBadge(change: TimeChange) {
    val visibility = remember(change) { Animatable(1f) }
    LaunchedEffect(change) { visibility.animateTo(0f, tween(900)) }
    Label(
        text = if (change.correct) "+2s" else "−2s",
        size = 36.sp,
        color = if (change.correct) Good else Bad,
        weight = FontWeight.Bold,
        modifier = Modifier.alpha(visibility.value),
    )
}

@Composable
private fun Flag(
    country: Country,
    mark: Mark,
    dimmed: Boolean,
    hidden: Boolean,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val corner = 6.dp
    Box(modifier.size(width, height)) {
        if (hidden) {
            Box(Modifier.fillMaxSize().dashedOutline(corner))
            return@Box
        }
        Image(
            painter = painterResource(country.flag),
            contentDescription = stringResource(country.name),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(corner))
                .alpha(
                    when {
                        mark == Mark.WRONG -> 0.3f
                        dimmed -> 0.45f
                        else -> 1f
                    },
                ),
        )
        when (mark) {
            Mark.RIGHT -> {
                Box(Modifier.fillMaxSize().ring(Good, 5.dp, corner))
                MarkBadge(mark, height * 0.41f, Modifier.align(Alignment.TopEnd))
            }
            Mark.WRONG -> {
                Box(Modifier.fillMaxSize().ring(Bad, 3.dp, corner))
                MarkBadge(mark, height * 0.3f, Modifier.align(Alignment.TopEnd))
            }
            Mark.NONE -> Unit
        }
    }
}

@Composable
private fun NameOption(
    name: String,
    mark: Mark,
    dimmed: Boolean,
    hidden: Boolean,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val corner = 10.dp
    Box(modifier.size(width, height)) {
        if (hidden) {
            Box(Modifier.fillMaxSize().dashedOutline(corner))
            return@Box
        }
        val shape = RoundedCornerShape(corner)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (mark == Mark.WRONG || dimmed) 0.6f else 1f)
                .clip(shape)
                .background(Card)
                .then(
                    when (mark) {
                        Mark.RIGHT -> Modifier.border(4.dp, Good, shape)
                        Mark.WRONG -> Modifier.border(4.dp, Bad, shape)
                        Mark.NONE -> Modifier
                    },
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = name,
                style = TextStyle(
                    color = if (mark == Mark.WRONG) Muted else Foreground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                autoSize = WholeWordsAutoSize(min = 14.sp, max = 26.sp),
            )
        }
        if (mark != Mark.NONE) {
            MarkBadge(mark, if (mark == Mark.RIGHT) 40.dp else 34.dp, Modifier.align(Alignment.TopEnd))
        }
    }
}

/**
 * A round badge over the top-right corner, with a drawn check for the right answer or a cross for one
 * ruled out. A ring of background colour separates it from the flag or name under it.
 */
@Composable
private fun MarkBadge(mark: Mark, size: Dp, modifier: Modifier = Modifier) {
    val outside = size * 0.2f
    Canvas(modifier.offset(x = outside, y = -outside).size(size)) {
        val radius = this.size.minDimension / 2
        val unit = this.size.width / 24
        drawCircle(Background, radius = radius + this.size.width * 0.09f)
        drawCircle(if (mark == Mark.RIGHT) Good else Bad, radius = radius)
        val symbol = Path().apply {
            if (mark == Mark.RIGHT) {
                moveTo(7.8f * unit, 12.3f * unit)
                lineTo(10.5f * unit, 15f * unit)
                lineTo(16.2f * unit, 9.3f * unit)
            } else {
                moveTo(9.3f * unit, 9.3f * unit)
                lineTo(14.7f * unit, 14.7f * unit)
                moveTo(14.7f * unit, 9.3f * unit)
                lineTo(9.3f * unit, 14.7f * unit)
            }
        }
        drawPath(symbol, Background, style = Stroke(1.9f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** An outline just outside the bounds, with a gap of background between it and the content. */
private fun Modifier.ring(color: Color, width: Dp, corner: Dp, gap: Dp = 4.dp) = drawBehind {
    val outset = (gap + width / 2).toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(-outset, -outset),
        size = Size(size.width + 2 * outset, size.height + 2 * outset),
        cornerRadius = CornerRadius(corner.toPx() + outset),
        style = Stroke(width.toPx()),
    )
}

/** The empty place of an answer while the game is paused. */
private fun Modifier.dashedOutline(corner: Dp) = drawBehind {
    val stroke = 2.dp.toPx()
    drawRoundRect(
        color = Line,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(corner.toPx()),
        style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
    )
}

@Composable
private fun Message(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) { content() }
}

@Composable
private fun Label(
    text: String,
    size: TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
) {
    BasicText(text, modifier, TextStyle(color = color, fontSize = size, fontWeight = weight, textAlign = TextAlign.Center))
}

@Composable
private fun bestLabel(count: Int) = pluralStringResource(R.plurals.best_count, count, count)

/**
 * Picks the largest size, from [max] down to [min], at which the text fits without a word broken across
 * lines. Country names in some languages have long single words, like "Zentralafrikanische".
 */
private data class WholeWordsAutoSize(val min: TextUnit, val max: TextUnit) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        var size = max.value
        while (size > min.value) {
            val layout = performLayout(constraints, text, size.sp)
            if (!layout.hasVisualOverflow && !layout.breaksWord(text)) return size.sp
            size -= 1
        }
        return min
    }

    private fun TextLayoutResult.breaksWord(text: AnnotatedString): Boolean =
        (0 until lineCount - 1).any { line ->
            val end = getLineEnd(line)
            end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()
        }
}
