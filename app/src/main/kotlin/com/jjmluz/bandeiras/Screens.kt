package com.jjmluz.bandeiras

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Background = Color(0xFF0B1220)
private val Foreground = Color(0xFFF4F6FB)
private val Muted = Color(0xFFA5AFC3)
private val Accent = Color(0xFFFFC83D)
private val Good = Color(0xFF4ADE80)
private val Bad = Color(0xFFFF5A5F)
private val Card = Color(0xFF1C2A44)

private const val TICK_MILLIS = 50L

@Composable
fun BandeirasApp(viewModel: GameViewModel) {
    Box(Modifier.fillMaxSize().background(Background)) {
        when (viewModel.screen) {
            Screen.START -> StartScreen(viewModel)
            Screen.PLAYING -> PlayingScreen(viewModel)
            Screen.PAUSED -> PausedScreen()
            Screen.OVER -> OverScreen(viewModel)
        }
    }
}

@Composable
private fun StartScreen(viewModel: GameViewModel) {
    Message {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Mode.entries.forEach { ModeCard(it, it == viewModel.mode, viewModel.best(it)) }
        }
    }
}

@Composable
private fun ModeCard(mode: Mode, selected: Boolean, best: Int) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .size(370.dp, 128.dp)
            .clip(shape)
            .background(if (selected) Card else Background)
            .border(if (selected) 4.dp else 1.dp, if (selected) Accent else Muted, shape)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Label(mode.title, 30.sp, if (selected) Accent else Foreground, FontWeight.Bold)
        Label(mode.hint, 20.sp, Foreground)
        if (best > 0) Label("Recorde: ${countLabel(best)}", 18.sp, Muted)
    }
}

@Composable
private fun PausedScreen() {
    Message {
        Label("Em pausa", 64.sp, Foreground, FontWeight.Bold)
        Label("Carrega em OK para continuar", 32.sp, Accent, FontWeight.Bold)
        Label("Carrega em Voltar para sair", 24.sp, Muted)
    }
}

@Composable
private fun OverScreen(viewModel: GameViewModel) {
    val guessedAll = viewModel.score == COUNTRIES.size
    Message {
        Label(if (guessedAll) "Acertaste em todos!" else "Acabou o tempo!", 40.sp, Muted, FontWeight.Bold)
        Label(viewModel.score.toString(), 120.sp, Foreground, FontWeight.Bold)
        Label(if (viewModel.score == 1) "país adivinhado" else "países adivinhados", 28.sp, Foreground)
        if (viewModel.isNewBest) {
            Label("Novo recorde!", 28.sp, Accent, FontWeight.Bold)
        } else {
            Label("Recorde: ${countLabel(viewModel.best(viewModel.mode))}", 24.sp, Muted)
        }
        Spacer(Modifier.height(8.dp))
        Label("Carrega em OK para jogar outra vez", 32.sp, Accent, FontWeight.Bold)
        Label("Carrega em Voltar para ir ao menu", 24.sp, Muted)
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
    val round = viewModel.round ?: return

    // Margins keep everything inside the TV's overscan-safe area.
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
        Hud(viewModel)
        when (viewModel.mode) {
            Mode.GUESS_FLAG -> GuessFlagRound(round, viewModel.rightAnswer, maxHeight)
            Mode.GUESS_COUNTRY -> GuessCountryRound(round, viewModel.rightAnswer, maxWidth, maxHeight)
        }
    }
}

/** The country's name in the middle, with a flag on each side. */
@Composable
private fun BoxScope.GuessFlagRound(round: Round, rightAnswer: Direction?, availableHeight: Dp) {
    val flagHeight = availableHeight * 0.3f
    val flagWidth = flagHeight * 4 / 3

    @Composable
    fun FlagAt(direction: Direction, modifier: Modifier = Modifier) = Flag(
        country = round.options.getValue(direction),
        rejected = direction in round.eliminated,
        right = direction == rightAnswer,
        width = flagWidth,
        height = flagHeight,
        modifier = modifier,
    )

    FlagAt(Direction.UP, Modifier.align(Alignment.TopCenter))
    FlagAt(Direction.DOWN, Modifier.align(Alignment.BottomCenter))
    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
        FlagAt(Direction.LEFT)
        Box(Modifier.size(flagWidth * 2, flagHeight).padding(horizontal = 16.dp), Alignment.Center) {
            BasicText(
                text = round.target.name,
                style = TextStyle(color = Foreground, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 40.sp),
            )
        }
        FlagAt(Direction.RIGHT)
    }
}

/** The country's flag in the middle, with a name on each side. */
@Composable
private fun BoxScope.GuessCountryRound(round: Round, rightAnswer: Direction?, availableWidth: Dp, availableHeight: Dp) {
    val flagHeight = availableHeight * 0.52f
    val flagWidth = flagHeight * 4 / 3
    val nameWidth = (availableWidth - flagWidth) / 2 - 12.dp

    @Composable
    fun NameAt(direction: Direction, height: Dp, modifier: Modifier = Modifier) = NameOption(
        name = round.options.getValue(direction).name,
        rejected = direction in round.eliminated,
        right = direction == rightAnswer,
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
        Flag(round.target, rejected = false, right = false, width = flagWidth, height = flagHeight)
        NameAt(Direction.RIGHT, topHeight * 1.4f)
    }
}

@Composable
private fun Hud(viewModel: GameViewModel) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.TopStart)) {
            Label("PONTOS", 18.sp, Muted, FontWeight.Bold)
            Label(viewModel.score.toString(), 56.sp, Foreground, FontWeight.Bold)
        }
        Column(Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
            Label("TEMPO", 18.sp, Muted, FontWeight.Bold)
            val seconds = viewModel.secondsLeft
            Label(
                text = "%d:%02d".format(seconds / 60, seconds % 60),
                size = 56.sp,
                color = if (seconds <= LOW_TIME_SECONDS) Bad else Foreground,
                weight = FontWeight.Bold,
            )
            viewModel.timeChange?.let { TimeChangeBadge(it) }
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
    rejected: Boolean,
    right: Boolean,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(modifier.size(width, height).clip(shape), Alignment.Center) {
        Image(
            painter = painterResource(country.flag),
            contentDescription = country.name,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize().alpha(if (rejected) 0.3f else 1f),
        )
        if (rejected) {
            Box(Modifier.fillMaxSize().border(5.dp, Bad, shape))
            Label("✕", 64.sp, Bad, FontWeight.Bold)
        }
        if (right) {
            Box(Modifier.fillMaxSize().background(Good.copy(alpha = 0.3f)).border(6.dp, Good, shape))
            Box(Modifier.size(80.dp).clip(CircleShape).background(Good), Alignment.Center) {
                Label("✓", 52.sp, Background, FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NameOption(
    name: String,
    rejected: Boolean,
    right: Boolean,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .size(width, height)
            .clip(shape)
            .background(if (right) Good else Card)
            .then(if (rejected) Modifier.border(5.dp, Bad, shape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = if (right) "✓ $name" else name,
            style = TextStyle(
                color = if (right) Background else if (rejected) Muted else Foreground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            // The cap keeps the longest single word (Turquemenistão) on one line.
            autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 26.sp),
        )
    }
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

private fun countLabel(count: Int) = if (count == 1) "1 país" else "$count países"
