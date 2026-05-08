# 🛒 ApeironMarket - Gelişmiş GUI Tabanlı Market Sistemi

ApeironMarket, Minecraft sunucunuz için sıfırdan tasarlanmış, tamamen özelleştirilebilir, modern ve yüksek performanslı bir market sistemidir. Hem standart Vault ekonomisini hem de özel Kristal birimini destekleyerek sunucunuza derinlik katar.

## 🔥 Temel Özellikler

*   **Çoklu Para Birimi:** Aynı market içerisinde hem normal para (Vault) hem de Kristal (özel birim) ile satış yapabilme imkanı.
*   **Dinamik Kategoriler:** Sınırsız sayıda kategori oluşturun (End, Nether, Yemek, Kristal vb.) ve her birini ayrı dosyalarda yönetin.
*   **Akıllı Miktar Seçimi:**
    *   **Tekli Alım:** Standart tıklama.
    *   **Maksimum Alım (Shift+Sol Tık):** Envanter kapasitenize göre alabileceğiniz en yüksek miktarı tek seferde alır.
    *   **Hepsini Sat (Shift+Sağ Tık):** Envanterinizdeki o türden tüm eşyaları tek seferde satar.
*   **Stack Menüsü:** Büyük alımlar için (1 stack, 7 stack vb.) özel görsel seçim menüsü.
*   **Gelişmiş Görsellik:** Base64 kafa (Player Head) dokuları, modern semboller ve şık GUI tasarımı.
*   **Kolay Yönetim:** Tüm market yapısı `menus/` klasörü altındaki YAML dosyalarından yönetilir.

## ⚙️ Kristal Entegrasyonu

Bu plugin, harici bir Kristal sistemiyle (örn: `Kristal` plugini) tam uyumlu çalışır. PlaceholderAPI üzerinden bakiye okur ve satın alımlarda komutlar aracılığıyla işlem yapar.

## 🛠️ Komutlar ve Yetkiler

| Komut | Açıklama | Yetki |
| :--- | :--- | :--- |
| `/market` | Market ana menüsünü açar. | - |
| `/market reload` | Tüm konfigürasyon ve menüleri yeniler. | `apeironmarket.reload` |

## 📦 Kurulum

1. `ApeironMarket.jar` dosyasını `plugins` klasörüne atın.
2. Sunucunuzu başlatın.
3. `plugins/Market/menus/` klasörü altındaki örnek menüleri (end.yml, nether.yml vb.) kendi ihtiyaçlarınıza göre düzenleyin.
4. Eşyaların alış ve satış fiyatlarını, para birimi türlerini (MONEY/KRISTAL) belirleyin.

---
*ApeironMarket ile sunucunuzun ticaret sistemini profesyonelleştirin.*
