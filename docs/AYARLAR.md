# ZonelyWebSender ayarları

Dosya: `plugins/ZonelyWebSender/config.yml`

## Portsuz bağlantı

```yaml
connection-mode: portless
relay-url: 'HTTPS_BAGLANTI_ADRESIN'
server-id: 1
socketPassword: 'SUNUCU_SIFREN'
poll-ticks: 20
```

`relay-url`, bağlantı sağlayıcından aldığın HTTPS adresidir. `server-id` ve şifre aynı sunucuya ait olmalıdır. Sunucu dışarı doğru HTTPS bağlantısı kurar.

## Portlu bağlantı

```yaml
connection-mode: port
socketPort: 9876
socketPassword: 'SUNUCU_SIFREN'
```

Ayarları kaydettikten sonra sunucuyu yeniden başlat.
