const fs = require('node:fs');
const path = require('node:path');
const target = path.resolve(__dirname, '../docs/assets');
fs.mkdirSync(target, { recursive: true });
// Feather icons: https://github.com/feathericons/feather (MIT; see FEATHER-LICENSE.txt).
const icons = {
  server: '<rect x="2" y="2" width="20" height="8" rx="2"/><rect x="2" y="14" width="20" height="8" rx="2"/><path d="M6 6h.01M6 18h.01"/>',
  shield: '<path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11z"/>',
  terminal: '<path d="m4 17 6-5-6-5M12 19h8"/>',
  link: '<path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/>',
  monitor: '<rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/>',
  check: '<path d="m20 6-11 11-5-5"/>',
};
const icon = (name, x, y, size = 24, color = '#60a5fa') => `<g data-feather="${name}" transform="translate(${x} ${y}) scale(${size / 24})" stroke="${color}" fill="none" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">${icons[name]}</g>`;
const text = (value, x, y, size = 20, color = '#dbeafe', weight = 400) => `<text x="${x}" y="${y}" fill="${color}" font-size="${size}" font-weight="${weight}">${value}</text>`;
const svg = (name, height, body, desc) => `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="${height}" viewBox="0 0 1200 ${height}" role="img" aria-labelledby="title desc"><title id="title">${name}</title><desc id="desc">${desc}</desc><rect width="1200" height="${height}" rx="20" fill="#0b1220"/><g font-family="Segoe UI,Arial,sans-serif">${body}</g></svg>`;

const cover = svg('ZonelyWebSender', 360,
  '<rect x="0" y="0" width="8" height="360" rx="4" fill="#3b82f6"/>' +
  text('ZONELY', 48, 57, 16, '#93c5fd', 600) +
  text('ZonelyWebSender', 44, 163, 82, '#ffffff', 750) +
  text('Minecraft sunucuna uzaktan komut gönder.', 48, 215, 27, '#cbd5e1') +
  icon('link', 50, 266, 26) + text('Uzaktan komut', 92, 287, 21, '#eff6ff', 600) +
  icon('shield', 320, 266, 26) + text('Güvenli bağlantı', 362, 287, 21, '#eff6ff', 600) +
  icon('terminal', 596, 266, 26) + text('Kolay kurulum', 638, 287, 21, '#eff6ff', 600) +
  icon('server', 997, 97, 120, '#60a5fa') +
  text('1.0.0-beta.4', 1000, 274, 18, '#94a3b8'),
  'ZonelyWebSender Minecraft eklentisi.');

let features = text('Bağlantı senin seçimin.', 48, 61, 34, '#fff', 700);
const cards = [
  ['shield', 'Portsuz kullanım', 'Sunucuna kolayca bağlan.', 'Ek bir gelen bağlantı portu açman gerekmez.'],
  ['terminal', 'Uzaktan komut', 'Sunucuna komut ve mesaj gönder.', 'İşlemler sunucu tarafından yürütülür.'],
  ['server', 'Güvenli bağlantı', 'Sunucunla güvenli iletişim kur.', 'Bağlantını şifrenle doğrula.'],
  ['link', 'Kolay kurulum', 'JAR dosyasını plugins klasörüne ekle.', 'Ayarlarını düzenle ve sunucunu başlat.'],
];
cards.forEach(([glyph, title, line1, line2], index) => {
  const x = 32 + (index % 2) * 572, y = 91 + Math.floor(index / 2) * 151;
  features += `<rect x="${x}" y="${y}" width="560" height="139" rx="14" fill="#111e31"/>` +
    icon(glyph, x + 22, y + 23, 28) + text(title, x + 67, y + 47, 23, '#f8fafc', 650) +
    text(line1, x + 22, y + 86, 19, '#cbd5e1') + text(line2, x + 22, y + 114, 19, '#94a3b8');
});

const flow = svg('ZonelyWebSender kurulumu', 315,
  text('Sunucuna ekle, ayarla, bağlantını kur.', 48, 61, 34, '#fff', 700) +
  text('Birkaç adımda kullanıma hazır.', 48, 99, 21, '#94a3b8') +
  [['server', 'JAR dosyasını ekle', 'plugins klasörüne yükle'], ['link', 'Ayarlarını düzenle', 'Bağlantı bilgilerini gir'], ['terminal', 'Sunucunu başlat', 'Bağlantını kontrol et']].map(([glyph, title, label], index) => {
    const x = 48 + index * 388;
    return `<rect x="${x}" y="133" width="328" height="127" rx="14" fill="#111e31"/>` + icon(glyph, x + 22, 153, 28) + text(title, x + 67, 178, 23, '#fff', 650) + text(label, x + 22, 229, 20, '#94a3b8') +
      (index < 2 ? `<path d="M${x + 344} 196h28m-8-8 8 8-8 8" stroke="#60a5fa" stroke-width="2" fill="none"/>` : '');
  }).join(''),
  'JAR dosyasını ekle, bağlantı ayarlarını düzenle ve sunucunu başlat.');

fs.writeFileSync(path.join(target, 'zonelywebsender-cover-v4.svg'), cover);
fs.writeFileSync(path.join(target, 'zonelywebsender-features-v4.svg'), svg('ZonelyWebSender özellikleri', 405, features, 'Portsuz kullanım, uzaktan komut, güvenli bağlantı ve kolay kurulum.'));
fs.writeFileSync(path.join(target, 'zonelywebsender-setup-v4.svg'), flow);
console.log('3 SVG tanıtım görseli üretildi.');
