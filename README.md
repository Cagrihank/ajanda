# Ajanda

Kalem destekli tabletler için sade, çevrimdışı bir ajanda / yapılacaklar / not defteri / hesap makinesi uygulaması (Android).

🇬🇧 [English below](#english)

## Özellikler

- **Ana sayfa** – bugünün görevleri, ajanda ve son notlar tek bakışta
- **Görevler** – tarih ve öncelik; Gecikmiş / Bugün / Yarın / Sonra grupları
- **Ajanda** – haftalık görünüm, saatli veya tüm gün etkinlikler
- **Not defteri** (kalemle)
  - Basınca duyarlı kalem, fosforlu kalem, silgi (kalemin silgi ucu / yan tuşu da çalışır)
  - Avuç içi reddi: varsayılan olarak yalnızca kalem çizer
  - Kement ile seç, taşı ve döndür; metin ekleme
  - Şekiller: çizgi, ok, dikdörtgen, daire, elips, üçgen
  - Renk paleti, kalınlık ayarı, düz / çizgili / kareli kâğıt
  - Parmakla kaydırma ve iki parmakla yakınlaştırma, çok sayfalı notlar
  - **Vektör PDF** olarak dışa aktarma
- **Takip listeleri** – kendi durumlarınızı tanımladığınız listeler (ör. RFI, sipariş)
- **Hesap makinesi** – standart, bilimsel ve **eğim** hesabı (eğim %, açı, 1:n, ‰; yatay uzunluk veya yükseklik bulma)

Tüm veriler yalnızca cihazda saklanır; internet izni yoktur, hiçbir veri dışarı gönderilmez.

## Kurulum

**Hazır APK:** [Releases](../../releases) bölümünden `Ajanda.apk` dosyasını indirip tablette açın. Android "bilinmeyen kaynaklardan yükleme" izni isteyebilir.

**Kaynaktan derleme:** Projeyi Android Studio ile açın ve ▶ Run'a basın.

- En düşük Android sürümü: 10 (API 29)
- Kotlin + Jetpack Compose; ek kütüphane gerektirmez

## Lisans

MIT – ayrıntılar için [LICENSE](LICENSE) dosyasına bakın.

---

## English

A simple, offline agenda, to-do, handwriting notebook and calculator app for Android tablets with stylus support. *(The app's interface is in Turkish.)*

### Features

- **Home** – today's tasks, agenda and recent notes at a glance
- **Tasks** – due dates and priorities, grouped as Overdue / Today / Tomorrow / Later
- **Agenda** – weekly view with timed or all-day events
- **Notebook** (stylus)
  - Pressure-sensitive pen, highlighter, eraser (works with the pen's eraser tip / side button)
  - Palm rejection: only the stylus draws by default
  - Lasso to select, move and rotate; text boxes
  - Shapes: line, arrow, rectangle, circle, ellipse, triangle
  - Color palette, adjustable stroke width, blank / lined / grid paper
  - Finger pan and two-finger pinch zoom, multi-page notes
  - Export as **vector PDF**
- **Tracking lists** – lists with your own custom statuses (e.g. RFIs, orders)
- **Calculator** – standard, scientific and **slope** modes (slope %, angle, 1:n, ‰; solve for horizontal length or height)

All data stays on the device. The app requests no internet permission and sends nothing anywhere.

### Installation

**Ready-made APK:** download `Ajanda.apk` from [Releases](../../releases) and open it on your tablet. Android may ask you to allow installing from unknown sources.

**Build from source:** open the project in Android Studio and press ▶ Run.

- Minimum Android version: 10 (API 29)
- Kotlin + Jetpack Compose; no extra libraries required

### License

MIT – see [LICENSE](LICENSE).
