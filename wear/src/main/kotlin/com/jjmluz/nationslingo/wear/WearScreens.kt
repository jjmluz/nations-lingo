package com.jjmluz.nationslingo.wear

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.jjmluz.nationslingo.COUNTRIES
import com.jjmluz.nationslingo.Country
import com.jjmluz.nationslingo.Direction
import com.jjmluz.nationslingo.GameViewModel
import com.jjmluz.nationslingo.LOW_TIME_SECONDS
import com.jjmluz.nationslingo.Mode
import com.jjmluz.nationslingo.Round
import com.jjmluz.nationslingo.START_MILLIS
import com.jjmluz.nationslingo.Screen
import com.jjmluz.nationslingo.TimeChange
import com.jjmluz.nationslingo.WholeWordsAutoSize
import com.jjmluz.nationslingo.shared.R
import kotlinx.coroutines.delay
import kotlin.math.sqrt

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

/** How an answer has been marked: the right one, or one already ruled out. */
private enum class Mark { NONE, RIGHT, WRONG }

/**
 * Everything is laid out on a square as wide as the screen, in fractions of its side, keeping clear of
 * the corners so it fits a round watch.
 */
private class Dial(val side: Dp, val textScale: Float) {
    operator fun times(fraction: Float): Dp = side * fraction

    /** A text size that is [fraction] of the side, whatever the font scale. */
    fun text(fraction: Float): TextUnit = TextUnit(side.value * fraction / textScale, TextUnitType.Sp)
}

/** Places a box of [width] by [height] centred on ([x], [y]), all fractions of the dial's side. */
private fun Modifier.at(dial: Dial, x: Float, y: Float, width: Float, height: Float) =
    offset(dial * (x - width / 2), dial * (y - height / 2)).size(dial * width, dial * height)

@Composable
fun WearApp(viewModel: GameViewModel, crown: Crown) {
    val view = LocalView.current
    val screen = viewModel.screen
    // The screen stays on during a game only, to spare the battery.
    SideEffect { view.keepScreenOn = screen == Screen.PLAYING }
    // Swiping back pauses a game, then goes to the title screen. From there it closes the app.
    BackHandler(enabled = screen != Screen.START) { viewModel.back() }

    BoxWithConstraints(Modifier.fillMaxSize().background(Background), Alignment.Center) {
        val dial = Dial(minOf(maxWidth, maxHeight), LocalDensity.current.fontScale)
        Box(Modifier.size(dial.side)) {
            when (screen) {
                Screen.START -> StartScreen(viewModel, dial)
                Screen.PLAYING -> PlayingScreen(viewModel, crown, dial)
                Screen.PAUSED -> PausedScreen(viewModel, dial)
                Screen.OVER -> OverScreen(viewModel, dial)
            }
        }
    }
}

@Composable
private fun BoxScope.StartScreen(viewModel: GameViewModel, dial: Dial) {
    Label(
        text = stringResource(R.string.app_name),
        size = dial.text(0.1f),
        color = Foreground,
        weight = FontWeight.Black,
        modifier = Modifier.at(dial, 0.5f, 0.2f, 0.66f, 0.13f),
        autoSize = TextAutoSize.StepBased(minFontSize = dial.text(0.06f), maxFontSize = dial.text(0.1f)),
    )
    Mode.entries.forEachIndexed { index, mode ->
        Pill(
            dial = dial,
            selected = mode == viewModel.mode,
            modifier = Modifier.at(dial, 0.5f, 0.43f + index * 0.205f, 0.84f, 0.18f),
            onClick = { viewModel.start(mode) },
        ) {
            Label(
                text = stringResource(mode.title),
                size = dial.text(0.068f),
                color = if (mode == viewModel.mode) Background else Foreground,
                weight = FontWeight.Black,
                autoSize = TextAutoSize.StepBased(minFontSize = dial.text(0.045f), maxFontSize = dial.text(0.068f)),
            )
            val best = viewModel.best(mode)
            if (best > 0) {
                Label(
                    text = pluralStringResource(R.plurals.best_count, best, best),
                    size = dial.text(0.045f),
                    color = if (mode == viewModel.mode) Background else Muted,
                )
            }
        }
    }
    Label(
        text = stringResource(viewModel.mode.hint),
        size = dial.text(0.048f),
        color = Soft,
        modifier = Modifier.at(dial, 0.5f, 0.84f, 0.56f, 0.12f),
        autoSize = WholeWordsAutoSize(min = dial.text(0.034f), max = dial.text(0.048f)),
    )
}

@Composable
private fun PlayingScreen(viewModel: GameViewModel, crown: Crown, dial: Dial) {
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MILLIS)
            viewModel.tick()
        }
    }
    val view = LocalView.current
    LaunchedEffect(viewModel.rightAnswer) {
        if (viewModel.rightAnswer != null) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }
    LaunchedEffect(viewModel.timeChange) {
        if (viewModel.timeChange?.correct == false) view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }
    Board(viewModel, crown, dial)
}

@Composable
private fun Board(viewModel: GameViewModel, crown: Crown, dial: Dial) {
    val round = viewModel.round ?: return
    Box(Modifier.fillMaxSize()) {
        TimeRing(viewModel.secondsLeft)
        val seconds = viewModel.secondsLeft
        Label(
            text = "%d:%02d".format(seconds / 60, seconds % 60),
            size = dial.text(0.06f),
            color = if (seconds <= LOW_TIME_SECONDS) Bad else Foreground,
            weight = FontWeight.Bold,
            modifier = Modifier.at(dial, 0.5f, 0.075f, 0.3f, 0.075f),
        )
        Label(
            text = viewModel.score.toString(),
            size = dial.text(0.06f),
            color = Accent,
            weight = FontWeight.Bold,
            modifier = Modifier.at(dial, 0.5f, 0.925f, 0.3f, 0.075f),
        )
        viewModel.timeChange?.let { TimeChangeBadge(it, dial) }

        val highlighted = crown.highlighted(round)
        fun mark(direction: Direction) = when (direction) {
            viewModel.rightAnswer -> Mark.RIGHT
            in round.eliminated -> Mark.WRONG
            else -> Mark.NONE
        }
        fun dimmed(direction: Direction) = viewModel.rightAnswer != null && direction != viewModel.rightAnswer
        val answer = { direction: Direction -> viewModel.answer(direction); Unit }
        when (viewModel.mode) {
            Mode.GUESS_FLAG -> GuessFlagRound(round, dial, highlighted, ::mark, ::dimmed, answer)
            Mode.GUESS_COUNTRY -> GuessCountryRound(round, dial, highlighted, ::mark, ::dimmed, answer)
        }
    }
}

/** The country's name at the top, with four flags in a square under it. */
@Composable
private fun BoxScope.GuessFlagRound(
    round: Round,
    dial: Dial,
    highlighted: Direction?,
    mark: (Direction) -> Mark,
    dimmed: (Direction) -> Boolean,
    answer: (Direction) -> Unit,
) {
    Label(
        text = stringResource(round.target.name),
        size = dial.text(0.085f),
        color = Foreground,
        weight = FontWeight.Bold,
        modifier = Modifier.at(dial, 0.5f, 0.262f, 0.74f, 0.18f),
        autoSize = WholeWordsAutoSize(min = dial.text(0.04f), max = dial.text(0.085f)),
    )
    val width = 0.29f
    val height = width * 3 / 4
    val gap = 0.03f
    ANSWER_ORDER.forEachIndexed { index, direction ->
        // Clockwise from the top left.
        val column = if (index == 0 || index == 3) -1 else 1
        val row = if (index < 2) 0 else 1
        Flag(
            country = round.options.getValue(direction),
            mark = mark(direction),
            dimmed = dimmed(direction),
            highlighted = direction == highlighted,
            modifier = Modifier
                .at(dial, 0.5f + column * (width + gap) / 2, 0.474f + row * (height + gap), width, height)
                .clickable(interactionSource = null, indication = null) { answer(direction) },
        )
    }
}

/** The country's flag at the top, with four names under it, each as wide as the screen allows. */
@Composable
private fun BoxScope.GuessCountryRound(
    round: Round,
    dial: Dial,
    highlighted: Direction?,
    mark: (Direction) -> Mark,
    dimmed: (Direction) -> Boolean,
    answer: (Direction) -> Unit,
) {
    Flag(
        country = round.target,
        mark = Mark.NONE,
        dimmed = false,
        highlighted = false,
        modifier = Modifier.at(dial, 0.5f, 0.265f, 0.34f, 0.255f),
    )
    val height = 0.1f
    val gap = 0.02f
    ANSWER_ORDER.forEachIndexed { index, direction ->
        val top = 0.42f + index * (height + gap)
        // The width of the round screen at this row's edge furthest from the middle, less a margin.
        val edge = maxOf(0.5f - top, top + height - 0.5f)
        val width = (2 * sqrt(0.25f - edge * edge) - 0.14f).coerceAtMost(0.76f)
        NameOption(
            name = stringResource(round.options.getValue(direction).name),
            mark = mark(direction),
            dimmed = dimmed(direction),
            highlighted = direction == highlighted,
            dial = dial,
            modifier = Modifier
                .at(dial, 0.5f, top + height / 2, width, height)
                .clickable(interactionSource = null, indication = null) { answer(direction) },
        )
    }
}

/** The clock as a ring round the edge of the screen, full at a minute or more. */
@Composable
private fun TimeRing(secondsLeft: Int) {
    val fraction by animateFloatAsState(
        targetValue = (secondsLeft * 1000f / START_MILLIS).coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "time",
    )
    val color = if (secondsLeft <= LOW_TIME_SECONDS) Bad else Accent
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 4.dp.toPx()
        val inset = 2.dp.toPx() + stroke / 2
        val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
        val style = Stroke(stroke, cap = StrokeCap.Round)
        drawArc(Line, -90f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        if (fraction > 0f) drawArc(color, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = style)
    }
}

@Composable
private fun TimeChangeBadge(change: TimeChange, dial: Dial) {
    val correct = change.correct
    val visibility = remember(change) { Animatable(1f) }
    LaunchedEffect(change) { visibility.animateTo(0f, tween(900)) }
    Label(
        text = if (correct) "+2s" else "−2s",
        size = dial.text(0.055f),
        color = if (correct) Good else Bad,
        weight = FontWeight.Bold,
        modifier = Modifier.at(dial, 0.72f, 0.135f, 0.16f, 0.06f).alpha(visibility.value),
    )
}

@Composable
private fun Flag(country: Country, mark: Mark, dimmed: Boolean, highlighted: Boolean, modifier: Modifier) {
    val corner = 4.dp
    Box(modifier) {
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
        when {
            mark == Mark.RIGHT -> Box(Modifier.fillMaxSize().ring(Good, 3.dp, corner))
            mark == Mark.WRONG -> Box(Modifier.fillMaxSize().ring(Bad, 2.dp, corner))
            highlighted -> Box(Modifier.fillMaxSize().ring(Accent, 3.dp, corner))
        }
        if (mark != Mark.NONE) MarkBadge(mark, Modifier.align(Alignment.Center).size(26.dp))
    }
}

@Composable
private fun NameOption(
    name: String,
    mark: Mark,
    dimmed: Boolean,
    highlighted: Boolean,
    dial: Dial,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .alpha(if (mark == Mark.WRONG || dimmed) 0.6f else 1f)
            .clip(shape)
            .background(Card)
            .then(
                when {
                    mark == Mark.RIGHT -> Modifier.border(2.dp, Good, shape)
                    mark == Mark.WRONG -> Modifier.border(2.dp, Bad, shape)
                    highlighted -> Modifier.border(2.dp, Accent, shape)
                    else -> Modifier
                },
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Label(
            text = name,
            size = dial.text(0.055f),
            color = if (mark == Mark.WRONG) Muted else Foreground,
            weight = FontWeight.Bold,
            autoSize = WholeWordsAutoSize(min = dial.text(0.03f), max = dial.text(0.055f)),
        )
    }
}

/** A check for the right answer or a cross for one ruled out, in a round badge. */
@Composable
private fun MarkBadge(mark: Mark, modifier: Modifier) {
    Canvas(modifier) {
        val radius = size.minDimension / 2
        val unit = size.width / 24
        drawCircle(Background, radius = radius + size.width * 0.09f)
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
        drawPath(symbol, Background, style = Stroke(2.2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** An outline just outside the bounds, with a gap of background between it and the content. */
private fun Modifier.ring(color: Color, width: Dp, corner: Dp, gap: Dp = 2.dp) = drawBehind {
    val outset = (gap + width / 2).toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(-outset, -outset),
        size = Size(size.width + 2 * outset, size.height + 2 * outset),
        cornerRadius = CornerRadius(corner.toPx() + outset),
        style = Stroke(width.toPx()),
    )
}

/** The answers are hidden while paused, so the stopped clock can't be used to study them. */
@Composable
private fun BoxScope.PausedScreen(viewModel: GameViewModel, dial: Dial) {
    val seconds = viewModel.secondsLeft
    Label(
        text = stringResource(R.string.paused),
        size = dial.text(0.1f),
        color = Foreground,
        weight = FontWeight.Black,
        modifier = Modifier.at(dial, 0.5f, 0.22f, 0.66f, 0.13f),
        autoSize = TextAutoSize.StepBased(minFontSize = dial.text(0.06f), maxFontSize = dial.text(0.1f)),
    )
    Label(
        text = "${viewModel.score}  ·  %d:%02d".format(seconds / 60, seconds % 60),
        size = dial.text(0.07f),
        color = Accent,
        weight = FontWeight.Bold,
        modifier = Modifier.at(dial, 0.5f, 0.36f, 0.7f, 0.1f),
    )
    Pill(dial, selected = true, Modifier.at(dial, 0.5f, 0.55f, 0.8f, 0.16f), onClick = { viewModel.confirm() }) {
        ButtonLabel(stringResource(R.string.continue_game), dial, Background)
    }
    Pill(dial, selected = false, Modifier.at(dial, 0.5f, 0.74f, 0.66f, 0.14f), onClick = { viewModel.back() }) {
        ButtonLabel(stringResource(R.string.quit_to_menu), dial, Foreground)
    }
}

@Composable
private fun BoxScope.OverScreen(viewModel: GameViewModel, dial: Dial) {
    val guessedAll = viewModel.score == COUNTRIES.size
    Column(
        modifier = Modifier.at(dial, 0.5f, 0.33f, 0.74f, 0.44f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Label(
            text = stringResource(if (guessedAll) R.string.guessed_all else R.string.time_up),
            size = dial.text(0.06f),
            color = Muted,
            weight = FontWeight.Bold,
        )
        Label(viewModel.score.toString(), dial.text(0.17f), Foreground, FontWeight.Bold)
        Label(pluralStringResource(R.plurals.countries_guessed, viewModel.score), dial.text(0.05f), Foreground)
        if (viewModel.isNewBest) {
            Label(stringResource(R.string.new_best), dial.text(0.055f), Accent, FontWeight.Bold)
        } else {
            val best = viewModel.best(viewModel.mode)
            Label(pluralStringResource(R.plurals.best_count, best, best), dial.text(0.045f), Muted)
        }
    }
    Pill(dial, selected = true, Modifier.at(dial, 0.5f, 0.68f, 0.76f, 0.15f), onClick = { viewModel.confirm() }) {
        ButtonLabel(stringResource(R.string.play_again), dial, Background)
    }
    Pill(dial, selected = false, Modifier.at(dial, 0.5f, 0.85f, 0.46f, 0.12f), onClick = { viewModel.back() }) {
        ButtonLabel(stringResource(R.string.menu), dial, Foreground)
    }
}

@Composable
private fun ButtonLabel(text: String, dial: Dial, color: Color) = Label(
    text = text,
    size = dial.text(0.06f),
    color = color,
    weight = FontWeight.Bold,
    autoSize = TextAutoSize.StepBased(minFontSize = dial.text(0.04f), maxFontSize = dial.text(0.06f)),
)

/** A rounded button, filled when [selected]. */
@Composable
private fun Pill(
    dial: Dial,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) Accent else Card)
            .then(if (selected) Modifier else Modifier.border(1.5.dp, Line, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = dial * 0.06f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

/** Text centred in its box. With [autoSize] it shrinks to fit, on one line unless it keeps words whole. */
@Composable
private fun Label(
    text: String,
    size: TextUnit,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    autoSize: TextAutoSize? = null,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        BasicText(
            text = text,
            style = TextStyle(color = color, fontSize = size, fontWeight = weight, textAlign = TextAlign.Center),
            maxLines = if (autoSize == null || autoSize is WholeWordsAutoSize) Int.MAX_VALUE else 1,
            autoSize = autoSize,
        )
    }
}
