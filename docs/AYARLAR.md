# ZonelyWebSender ayarları

Dosya: `plugins/ZonelyWebSender/config.yml`

## Portsuz bağlantı

```yaml
connection-mode: portless
relay-url: 'HTTPS_BAGLANTI_ADRESIN'
socketPassword: 'SUNUCU_SIFREN'
poll-ticks: 20
```

Bağlantı adresini ve şifreni gir. Sunucunu yeniden başlat.

## Portlu bağlantı

```yaml
connection-mode: port
socketPort: 9876
socketPassword: 'SUNUCU_SIFREN'
```

Ayarları kaydettikten sonra sunucuyu yeniden başlat.
