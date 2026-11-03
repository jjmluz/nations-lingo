// Renders every flag referenced by Countries.kt from the flag-icons SVGs to a WebP drawable,
// and prints the pairs of flags that look almost the same (see LOOKALIKES in Game.kt).
//
//   npm install --ignore-scripts --prefix <deps> flag-icons sharp
//   NODE_PATH=<deps>/node_modules node tools/generate_flags.cjs
const fs = require('fs');
const path = require('path');
const sharp = require('sharp');

const WIDTH = 480;
const HEIGHT = 360;
const BLEED = 4;
// Mean per-channel difference (0-255) below which two flags count as lookalikes.
const LOOKALIKE_THRESHOLD = 26;

const root = path.join(__dirname, '..');
const svgDir = path.join(path.dirname(require.resolve('flag-icons/package.json')), 'flags/4x3');
const outDir = path.join(root, 'app/src/main/res/drawable-nodpi');
const source = fs.readFileSync(path.join(root, 'app/src/main/kotlin/com/jjmluz/bandeiras/Countries.kt'), 'utf8');
const countries = [...source.matchAll(/Country\("([^"]+)", R\.drawable\.flag_([a-z]{2})\)/g)]
  .map(([, name, code]) => ({ name, code }));

(async () => {
  const pixels = [];
  for (const { code } of countries) {
    const svg = path.join(svgDir, `${code}.svg`);
    // Rendered slightly oversized and cropped, since some SVGs fringe at their outer edge.
    const rendered = await sharp(svg, { density: 600 })
      .resize(WIDTH + 2 * BLEED, HEIGHT + 2 * BLEED, { fit: 'fill' })
      .flatten({ background: '#ffffff' })
      .extract({ left: BLEED, top: BLEED, width: WIDTH, height: HEIGHT })
      .png().toBuffer();
    await sharp(rendered).webp({ quality: 92, effort: 6 }).toFile(path.join(outDir, `flag_${code}.webp`));
    pixels.push(await sharp(rendered).resize(48, 36, { fit: 'fill' }).removeAlpha().raw().toBuffer());
  }
  console.log(`rendered ${countries.length} flags`);

  const pairs = [];
  for (let a = 0; a < countries.length; a++) {
    for (let b = a + 1; b < countries.length; b++) {
      let sum = 0;
      for (let i = 0; i < pixels[a].length; i++) sum += Math.abs(pixels[a][i] - pixels[b][i]);
      const difference = sum / pixels[a].length;
      if (difference < LOOKALIKE_THRESHOLD) pairs.push([difference, countries[a].name, countries[b].name]);
    }
  }
  pairs.sort((x, y) => x[0] - y[0]);
  for (const [difference, a, b] of pairs) console.log(difference.toFixed(1).padStart(5), a, '/', b);

  await launcherImage(320, 180, 'banner');
  await launcherImage(180, 180, 'icon');
})();

// Four flags in the game's cross layout, at twice the size the launcher shows them.
async function launcherImage(width, height, name) {
  const scale = 2;
  const flagWidth = 120, flagHeight = 90, gap = 12;
  const centerX = width * scale / 2;
  const centerY = height * scale / 2;
  const places = [
    ['pt', 0, -1], ['jp', -1, 0], ['ca', 1, 0], ['br', 0, 1],
  ];
  const flags = await Promise.all(places.map(async ([code, across, down]) => ({
    input: await sharp(path.join(svgDir, `${code}.svg`), { density: 300 }).resize(flagWidth, flagHeight, { fit: 'fill' }).png().toBuffer(),
    left: Math.round(centerX - flagWidth / 2 + across * (flagWidth + gap) * 0.75),
    top: Math.round(centerY - flagHeight / 2 + down * (flagHeight + gap)),
  })));
  const background = Buffer.from(
    `<svg width="${width * scale}" height="${height * scale}"><rect width="100%" height="100%" fill="#0B1220"/></svg>`);
  await sharp(background).composite(flags).webp({ quality: 92 }).toFile(path.join(outDir, `${name}.webp`));
}
