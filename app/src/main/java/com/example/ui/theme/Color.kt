package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Mat koyu palet.
 *
 * ## Neon yasağı — tasarım kuralı
 *
 * Neon görünümü üç şeyin birleşimidir: **yüksek doygunluk + yüksek parlaklık +
 * ışıma (glow/bloom)**. Bu palet üçünü de reddeder:
 *
 * 1. Gövde renkleri nötr slate ailesinden gelir (doygunluk ~%20'nin altında).
 *    Arayüzün büyük yüzeyleri renkli değil, *değer* farkıyla ayrışır.
 * 2. Vurgu renkleri yalnızca durum bildirmek için ve yalnızca küçük alanlarda
 *    kullanılır — metin, ikon, 1 dp kenarlık. Geniş dolgular için `...Muted`
 *    (koyulaştırılmış, düşük ışıklı) karşılıkları vardır.
 * 3. Hiçbir yerde ışıma/halo/bloom yok. Derinlik gölgeden değil, [Elevation]
 *    yüzey katmanlarından ve saç teli (hairline) kenarlıktan gelir.
 *
 * Projeden çıkarılan neon renkler ve yerine geleni:
 * `#06B6D4` (neon camgöbeği) → [AccentSteel] • `#00E676` (neon yeşil) → [StatusSafe]
 * `#FF5252` / `#FF3333` / `Color.Red` → [StatusDanger].
 *
 * ## Katman merdiveni
 *
 * Slate ailesinde, adım başına ~%4 parlaklık artışıyla ilerleyen tek bir merdiven.
 * Bir bileşen ne kadar "yukarıdaysa" o kadar açık bir zemin alır; gölge kullanılmaz.
 */

// ------------------------------------------------------------------ koyu merdiven

/** Her şeyin arkasındaki en derin ton. Tam siyah değil; siyah, OLED'de bant yapar. */
val SlateVoid = Color(0xFF060911)

/** Uygulama arka planı. */
val SlateBackground = Color(0xFF0B0F17)

/** Arka planın hemen üstü: bölüm zeminleri, girinti alanları. */
val SlateSurfaceLow = Color(0xFF131A26)

/** Standart kart / panel zemini. */
val SlateSurface = Color(0xFF1E293B)

/** Kart içindeki ikinci katman: satır vurgusu, seçili durum. */
val SlateSurfaceHigh = Color(0xFF273449)

/** Diyalog, alt sayfa (bottom sheet), açılır menü. */
val SlateSurfaceHighest = Color(0xFF313F57)

// ------------------------------------------------------------------ çizgiler

/** Saç teli kenarlık — bileşenleri gölge yerine bununla ayırıyoruz. */
val SlateBorder = Color(0xFF334155)

/** Odaklanmış / seçili kenarlık. */
val SlateBorderStrong = Color(0xFF475569)

// ------------------------------------------------------------------ metin

val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

/** Üçüncül metin: zaman damgası, birim, yardım metni. */
val TextTertiary = Color(0xFF64748B)

/** Devre dışı bileşen metni. */
val TextDisabled = Color(0xFF475569)

// ------------------------------------------------------------------ aydınlık merdiven

val LightBackground = Color(0xFFF1F5F9)
val LightSurfaceLow = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceHigh = Color(0xFFF1F5F9)
val LightSurfaceHighest = Color(0xFFE2E8F0)

val LightBorder = Color(0xFFCBD5E1)
val LightBorderStrong = Color(0xFF94A3B8)

val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextTertiary = Color(0xFF64748B)
val LightTextDisabled = Color(0xFF94A3B8)

// ------------------------------------------------------------------ durum renkleri

/** Mat zümrüt. Yalnızca "güvenli/tamamlandı" durumunu bildirir. */
val StatusSafe = Color(0xFF10B981)

/** Zümrütün geniş dolgu karşılığı — ışımasın diye değeri düşürülmüş. */
val StatusSafeMuted = Color(0xFF12503F)

/** Kehribar. "Dikkat / beklemede". */
val StatusWarning = Color(0xFFF59E0B)
val StatusWarningMuted = Color(0xFF5C3D08)

/** Mat yakut. "Tehdit / hata". */
val StatusDanger = Color(0xFFEF4444)
val StatusDangerMuted = Color(0xFF5E2020)

/**
 * Doygunluğu düşürülmüş çelik mavisi. Projede neon camgöbeği (`#06B6D4`) nerede
 * kullanıldıysa yerine bu geçer: bilgi taşır, ışımaz.
 */
val AccentSteel = Color(0xFF7C97B8)
val AccentSteelMuted = Color(0xFF27374B)

// ------------------------------------------------------------------ aydınlık durum karşılıkları

val LightStatusSafe = Color(0xFF047857)
val LightStatusSafeMuted = Color(0xFFD1FAE5)
val LightStatusWarning = Color(0xFFB45309)
val LightStatusWarningMuted = Color(0xFFFEF3C7)
val LightStatusDanger = Color(0xFFB91C1C)
val LightStatusDangerMuted = Color(0xFFFEE2E2)
val LightAccentSteel = Color(0xFF44618A)
val LightAccentSteelMuted = Color(0xFFDBE3EF)
