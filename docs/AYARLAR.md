# ZonelyWebSender ayarları

Dosya: `plugins/ZonelyWebSender/config.yml`

[Yayınlar sayfasından JAR indir](https://github.com/zonely-gen-tr/ZonelyWebSender/releases), `plugins` klasörüne ekle ve ayar dosyasının oluşması için sunucuyu bir kez başlat.

İlk kurulumda varsayılan bağlantı türü **Portlu** (`connection-mode: port`). Portsuz kullanmak için bu değeri `portless` olarak değiştir. Güncelleme mevcut bağlantı seçimini korur.

Beta.6 ve üzeri, ayarların üzerindeki `#` açıklamalarını korur. Önceki sürümde açıklamalar silindiyse JAR'ı güncelleyip sunucuyu yeniden başlatman yeterli; mevcut adres, şifre ve port değerleri korunarak açıklamalar geri eklenir. Config dosyasını silmen gerekmez.

## Portsuz bağlantı

```yaml
connection-mode: portless
relay-url: 'https://siteadresin.com'
socketPassword: 'KENDI_UZUN_SIFRENI_YAZ'
poll-ticks: 20
```

`relay-url` alanına yalnızca Zonely sitenin adresini, `socketPassword` alanına panelde aynı sunucu için belirlediğin şifreyi yaz. Bağlantı yolu otomatik tamamlanır. `https://siteadresin.com` ve `https://siteadresin.com/` aynı şekilde çalışır. `siteadresin.com` yazarsan HTTPS otomatik eklenir. Her sunucuya farklı, en az 12 karakterli şifre belirle. Panelde de portsuz bağlantıyı seç ve sunucuyu yeniden başlat. Oyun sunucusunda ek port açman gerekmez.

## Portlu bağlantı

```yaml
connection-mode: port
socketPort: 9876
socketPassword: 'KENDI_UZUN_SIFRENI_YAZ'
```

`socketPort` değerine kullanacağın WebSender portunu yaz. Panelde portlu bağlantıyı seç; adres, port ve şifreyi eklentiyle aynı gir. Bu portun sunucu güvenlik duvarından erişilebilir olmasını sağla ve sunucuyu yeniden başlat.

Şifrende `#`, `:`, `!` gibi özel karakterler kullanabilirsin. YAML'da şifreyi tek tırnak içinde yaz; şifrenin içindeki tek tırnağı iki kez yaz (`'ornek''sifre!123'`).

Bağlantıyı denemek için panelinden `say ZonelyWebSender baglantisi hazir` komutunu gönder. Mesaj oyun sohbetinde görünüyorsa komut sunucuya ulaşmıştır.
