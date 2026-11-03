# Bandeiras

A timed flag quiz for Android TV, in Portuguese from Portugal, played with the remote's D-pad. No internet
needed.

The title screen offers two games with the same rules:

- Adivinha a Bandeira: a country name in the middle of the screen, with four flags around it.
- Adivinha o País: a flag in the middle of the screen, with four country names around it.

Press the D-pad towards the right answer to move on to the next country.

- The game starts with 1 minute on the clock.
- Right answer: 1 point, plus +2 seconds if it was the first try on that country.
- A right answer shows in green with a checkmark for a moment, with the clock frozen.
- Wrong answer: −2 seconds, and you stay on the same country. The rejected flag is crossed out, and
  pressing it again costs nothing.
- The game ends when the clock reaches zero, or when all 197 countries have been guessed.
- Back pauses the game. Going to the home screen pauses it too.

## Build and install

Needs Android Studio's JDK (21) and the Android SDK; `local.properties` holds the SDK path.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest installDebug
```

If several devices are connected, pick one with `ANDROID_SERIAL` (see `adb devices`).

## Layout

- `Game.kt`: the rules, with no Android UI code. Tested in `GameTest.kt`.
- `GameViewModel.kt`: runs a game on the real clock, maps remote keys, saves the best score of each game.
- `Screens.kt`: the Compose screens.
- `Sounds.kt`: plays the sound effects.
- `Countries.kt`: the 197 countries (193 UN members, Palestina, Vaticano, Kosovo, Taiwan).

## Flags

Flags come from [flag-icons](https://github.com/lipis/flag-icons) (MIT). They are rendered to
`res/drawable-nodpi` by `tools/generate_flags.cjs`, which also makes the launcher banner and icon, and
lists flags that look alike. Pairs too close to tell apart go in `LOOKALIKES` in `Game.kt`.

## Sounds

The sound effects are synthesized by `tools/generate_sounds.py` into `res/raw`.
