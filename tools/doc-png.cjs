const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { chromium } = require(process.env.ZONELY_PLAYWRIGHT_MODULE || 'playwright');
(async () => {
  const browser = await chromium.launch({ headless: true });
  try {
    const directory = path.resolve(__dirname, '../docs/assets');
    const files = fs.readdirSync(directory).filter(name => name.endsWith('.svg'));
    for (const file of files) {
      const page = await browser.newPage({ viewport: { width: 1200, height: 500 }, deviceScaleFactor: 1 });
      await page.goto(pathToFileURL(path.join(directory, file)).href);
      await page.evaluate(() => document.fonts.ready);
      const bounds = await page.locator('svg').boundingBox();
      const overflow = await page.locator('text').evaluateAll(nodes => nodes.filter(node => { const r = node.getBoundingClientRect(); return r.x < 0 || r.right > 1200; }).map(node => node.textContent));
      if (overflow.length) throw Error(`${file}: metin taşması: ${overflow}`);
      await page.screenshot({ path: path.join(directory, file.replace('.svg', '.png')), clip: { x: 0, y: 0, width: 1200, height: bounds.height } });
      await page.close();
    }
    console.log(`${files.length} PNG tanıtım görseli render edildi; yatay metin taşması yok.`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
