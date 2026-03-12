package com.callapp.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Типографика CallApp на основе design-reference.
 *
 * Tailwind → Material 3 маппинг:
 *
 *  text-4xl  36sp  font-bold      → displaySmall    (имя на экране входящего звонка)
 *  text-3xl  30sp  font-semibold  → headlineLarge   (имя на экране звонка, крупные заголовки)
 *  text-2xl  24sp  font-semibold  → headlineMedium  (заголовки страниц)
 *  text-xl   20sp  font-medium    → headlineSmall   (подзаголовки)
 *  text-lg   18sp  font-medium    → titleLarge      (таймер звонка, секции)
 *  text-base 16sp  font-medium    → titleMedium     (названия карточек, лейблы)
 *  text-base 16sp  font-normal    → bodyLarge       (основной текст, поля ввода)
 *  text-sm   14sp  font-medium    → bodyMedium      (описания, вторичный текст)
 *  text-sm   14sp  font-normal    → bodySmall       (вторичный текст)
 *  text-xs   12sp  font-medium    → labelMedium     (бейджи, индикаторы)
 *  text-xs   12sp  font-normal    → labelSmall      (подсказки, таймстемпы)
 */

val Typography = Typography(

    // ── Display ──────────────────────────────────────────────────────────────
    // text-4xl font-bold — имя контакта на экране входящего звонка
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 54.sp,      // 1.5
    ),

    // ── Headline ─────────────────────────────────────────────────────────────
    // text-3xl font-semibold — имя контакта на экране звонка
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 45.sp,
    ),

    // text-2xl font-semibold — заголовки страниц ("Мой профиль", "Настройки")
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 36.sp,
    ),

    // text-xl font-medium — подзаголовки, название сервера на экране звонка
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 30.sp,
    ),

    // ── Title ────────────────────────────────────────────────────────────────
    // text-lg font-medium — таймер звонка, секционные заголовки
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 27.sp,
    ),

    // text-base font-medium — названия в карточках, лейблы форм
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),

    // text-sm font-semibold — мелкие заголовки секций
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),

    // ── Body ─────────────────────────────────────────────────────────────────
    // text-base font-normal — основной текст, поля ввода
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),

    // text-sm font-medium — описания, вторичный текст с акцентом
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),

    // text-sm font-normal — описания, вторичный текст
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),

    // ── Label ────────────────────────────────────────────────────────────────
    // text-base font-medium — кнопки
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),

    // text-xs font-medium — бейджи, счётчики, индикаторы
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),

    // text-xs font-normal — подсказки, таймстемпы, хелпер-текст
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
)
