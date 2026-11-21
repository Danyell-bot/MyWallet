package com.example.mywallet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private class Ic(val paths: List<String>, val dots: List<Triple<Float, Float, Float>> = emptyList(), val rects: List<FloatArray> = emptyList())

/** Thin rounded line icons on a 24 x 24 grid (same family and weight, so they look like one set). */
private val ICONS = mapOf(
    "pie" to Ic(listOf("M21.21 15.89A10 10 0 1 1 8 2.83", "M22 12A10 10 0 0 0 12 2v10z")),
    "trend" to Ic(listOf("M22 7 13.5 15.5 8.5 10.5 2 17", "M16 7h6v6")),
    "sparkles" to Ic(listOf(
        "M9.937 15.5A2 2 0 0 0 8.5 14.063l-6.135-1.582a.5.5 0 0 1 0-.962L8.5 9.936A2 2 0 0 0 9.937 8.5l1.582-6.135a.5.5 0 0 1 .963 0L14.063 8.5A2 2 0 0 0 15.5 9.937l6.135 1.581a.5.5 0 0 1 0 .964L15.5 14.063a2 2 0 0 0-1.437 1.437l-1.582 6.135a.5.5 0 0 1-.963 0z",
        "M20 3v4", "M22 5h-4", "M4 17v2", "M5 18H3")),
    "search" to Ic(listOf("M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16z", "M21 21l-4.3-4.3")),
    "repeat" to Ic(listOf("m17 2 4 4-4 4", "M3 11v-1a4 4 0 0 1 4-4h14", "m7 22-4-4 4-4", "M21 13v1a4 4 0 0 1-4 4H3")),
    "tag" to Ic(listOf("M12.586 2.586A2 2 0 0 0 11.172 2H4a2 2 0 0 0-2 2v7.172a2 2 0 0 0 .586 1.414l8.704 8.704a2.426 2.426 0 0 0 3.42 0l6.58-6.58a2.426 2.426 0 0 0 0-3.42z"), dots = listOf(Triple(7.5f, 7.5f, 1f))),
    "zap" to Ic(listOf("M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z")),
    "receipt" to Ic(listOf("M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1z", "M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8", "M12 17.5v-11")),
    "lock" to Ic(listOf("M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z", "M7 11V7a5 5 0 0 1 10 0v4")),
    "bell" to Ic(listOf("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0")),
    "cloud" to Ic(listOf("M12 13v8", "M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242", "m8 17 4-4 4 4")),
    "download" to Ic(listOf("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "M7 10l5 5 5-5", "M12 15V3")),
    "eye" to Ic(listOf("M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0", "M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z")),
    "eyeoff" to Ic(listOf("M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49", "M14.084 14.158a3 3 0 0 1-4.242-4.242", "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143", "m2 2 20 20")),
    "grid" to Ic(emptyList(), rects = listOf(floatArrayOf(3f, 3f, 7f, 7f), floatArrayOf(14f, 3f, 7f, 7f), floatArrayOf(14f, 14f, 7f, 7f), floatArrayOf(3f, 14f, 7f, 7f))),
    "help" to Ic(listOf("M22 12A10 10 0 1 1 2 12a10 10 0 0 1 20 0z", "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"), dots = listOf(Triple(12f, 17f, 0.9f))),
    "info" to Ic(listOf("M22 12A10 10 0 1 1 2 12a10 10 0 0 1 20 0z", "M12 16v-4"), dots = listOf(Triple(12f, 8f, 0.9f))),
    "back" to Ic(listOf("m15 18-6-6 6-6")),
    "close" to Ic(listOf("M18 6 6 18", "m6 6 12 12")),
    "paperclip" to Ic(listOf("m21.44 11.05-9.19 9.19a6 6 0 0 1-8.49-8.49l8.57-8.57A4 4 0 1 1 18 8.84l-8.59 8.57a2 2 0 0 1-2.83-2.83l8.49-8.48")),
    "wallet" to Ic(listOf("M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1", "M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4")),
    "landmark" to Ic(listOf("M10 18v-7", "M11.12 2.198a2 2 0 0 1 1.76.006l7.866 3.847c.476.233.31.949-.22.949H3.474c-.53 0-.695-.716-.22-.949z", "M14 18v-7", "M18 18v-7", "M3 22h18", "M6 18v-7")),
    "banknote" to Ic(listOf("M4 6h16a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z", "M14 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0z", "M6 12h.01", "M18 12h.01")),
    "card" to Ic(listOf("M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", "M2 10h20")),
    "utensils" to Ic(listOf("M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2", "M7 2v20", "M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3zm0 0v7")),
    "coffee" to Ic(listOf("M10 2v2", "M14 2v2", "M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1", "M6 2v2")),
    "bus" to Ic(listOf("M8 6v6", "M15 6v6", "M2 12h19.6", "M18 18h3s.5-1.7.8-2.8c.1-.4.2-.8.2-1.2 0-.4-.1-.8-.2-1.2l-1.4-5C20.1 6.8 19.1 6 18 6H4a2 2 0 0 0-2 2v10h3", "M9 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0z", "M18 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0z", "M9 18h5")),
    "car" to Ic(listOf("M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2", "M9 17a2 2 0 1 1-4 0 2 2 0 0 1 4 0z", "M19 17a2 2 0 1 1-4 0 2 2 0 0 1 4 0z", "M9 17h6")),
    "plane" to Ic(listOf("M17.8 19.2 16 11l3.5-3.5C21 6 21.5 4 21 3c-1-.5-3 0-4.5 1.5L13 8 4.8 6.2c-.5-.1-.9.1-1.1.5l-.3.5c-.2.5-.1 1 .3 1.3L9 12l-2 3H4l-1 1 3 2 2 3 1-1v-3l3-2 3.5 5.3c.3.4.8.5 1.3.3l.5-.2c.4-.3.6-.7.5-1.2z")),
    "bag" to Ic(listOf("M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z", "M3 6h18", "M16 10a4 4 0 0 1-8 0")),
    "cart" to Ic(listOf("M10 21a1 1 0 1 1-2 0 1 1 0 0 1 2 0z", "M20 21a1 1 0 1 1-2 0 1 1 0 0 1 2 0z", "M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12")),
    "book" to Ic(listOf("M12 7v14", "M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z")),
    "pill" to Ic(listOf("m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z", "m8.5 8.5 7 7")),
    "heart" to Ic(listOf("M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7z")),
    "phone" to Ic(listOf("M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z", "M12 18h.01")),
    "gamepad" to Ic(listOf("M6 12h4", "M8 10v4", "M15 13h.01", "M18 11h.01", "M17.32 5H6.68a4 4 0 0 0-3.978 3.59c-.006.052-.01.101-.017.152C2.604 9.416 2 14.456 2 16a3 3 0 0 0 3 3c1 0 1.5-.5 2-1l1.414-1.414A2 2 0 0 1 9.828 16h4.344a2 2 0 0 1 1.414.586L17 18c.5.5 1 1 2 1a3 3 0 0 0 3-3c0-1.545-.604-6.584-.685-7.258-.007-.05-.011-.1-.017-.151A4 4 0 0 0 17.32 5z")),
    "more" to Ic(listOf("M6 12h.01", "M12 12h.01", "M18 12h.01", "M22 12A10 10 0 1 1 2 12a10 10 0 0 1 20 0z")),
    "briefcase" to Ic(listOf("M16 20V4a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16", "M4 6h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z")),
    "gift" to Ic(listOf("M20 12v10H4V12", "M2 7h20v5H2z", "M12 22V7", "M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z", "M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z")),
    "coin" to Ic(listOf("M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z", "M12 7v10", "M15 9.5c-.6-.9-1.7-1.5-3-1.5-1.7 0-3 .9-3 2s1.3 2 3 2 3 .9 3 2-1.3 2-3 2c-1.3 0-2.4-.6-3-1.5")),
    "calendar" to Ic(listOf("M8 2v4", "M16 2v4", "M3 8h18", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z")),
    "swap" to Ic(listOf("M8 3 4 7l4 4", "M4 7h16", "m16 21 4-4-4-4", "M20 17H4")),
    "target" to Ic(listOf("M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0z", "M18 12a6 6 0 1 1-12 0 6 6 0 0 1 12 0z", "M14 12a2 2 0 1 1-4 0 2 2 0 0 1 4 0z")),
    "plus" to Ic(listOf("M5 12h14", "M12 5v14")),
    "minus" to Ic(listOf("M5 12h14")),
    "camera" to Ic(listOf("M13.997 4a2 2 0 0 1 1.76 1.05l.486.9A2 2 0 0 0 18.003 7H20a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h1.997a2 2 0 0 0 1.759-1.048l.489-.904A2 2 0 0 1 10.004 4z", "M16 13a4 4 0 1 1-8 0 4 4 0 0 1 8 0z")),
    "image" to Ic(listOf("M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M10 9a2 2 0 1 1-4 0 2 2 0 0 1 4 0z", "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21")),
    "edit" to Ic(listOf("M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z", "m15 5 4 4")),
    "copy" to Ic(listOf("M20 9h-9a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h9a2 2 0 0 0 2-2v-9a2 2 0 0 0-2-2z", "M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1")),
    "volume" to Ic(listOf("M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z", "M16 9a5 5 0 0 1 0 6", "M19.364 18.364a9 9 0 0 0 0-12.728")),
    "volumeoff" to Ic(listOf("M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z", "m22 9-6 6", "m16 9 6 6")),
    "trash" to Ic(listOf("M3 6h18", "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6", "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2", "M10 11v6", "M14 11v6")),
    "check" to Ic(listOf("M21.801 10A10 10 0 1 1 17 3.335", "m9 11 3 3L22 4")),
    "flame" to Ic(listOf("M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z")),
    "chart" to Ic(listOf("M3 3v18h18", "M18 17V9", "M13 17V5", "M8 17v-3")),
    "home" to Ic(listOf("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8", "M3 10a2 2 0 0 1 .709-1.528l7-5.999a2 2 0 0 1 2.582 0l7 5.999A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")),
    "award" to Ic(listOf("m15.477 12.89 1.515 8.526a.5.5 0 0 1-.81.47l-3.58-2.687a1 1 0 0 0-1.197 0l-3.586 2.686a.5.5 0 0 1-.81-.469l1.514-8.526", "M18 8A6 6 0 1 1 6 8a6 6 0 0 1 12 0z")),
    "trophy" to Ic(listOf("M6 9H4.5a2.5 2.5 0 0 1 0-5H6", "M18 9h1.5a2.5 2.5 0 0 0 0-5H18", "M4 22h16", "M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22", "M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22", "M18 2H6v7a6 6 0 0 0 12 0V2z")),
    "code" to Ic(listOf("m16 18 6-6-6-6", "m8 6-6 6 6 6")),
    "file" to Ic(listOf("M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z", "M14 2v4a2 2 0 0 0 2 2h4", "M8 13h8", "M8 17h6")),
    "users" to Ic(listOf("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2", "M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", "M22 21v-2a4 4 0 0 0-3-3.87", "M16 3.13a4 4 0 0 1 0 7.75")),
    "bellring" to Ic(listOf("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0", "M2 8c0-2.2.7-4.3 2-6", "M22 8a10 10 0 0 0-2-6")),
    "upload" to Ic(listOf("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "M17 8l-5-5-5 5", "M12 3v12")),
    "share" to Ic(listOf("M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8", "M16 6l-4-4-4 4", "M12 2v13")),
    "gear" to Ic(listOf("M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z")),
    "paw" to Ic(listOf("M12 12.5c-2.6 0-5 2.6-5 5 0 1.7 1.3 2.5 2.8 2.5.9 0 1.4-.3 2.2-.3s1.3.3 2.2.3c1.5 0 2.8-.8 2.8-2.5 0-2.4-2.4-5-5-5z"), dots = listOf(Triple(5.4f, 11f, 1.9f), Triple(9f, 6.6f, 1.9f), Triple(15f, 6.6f, 1.9f), Triple(18.6f, 11f, 1.9f))),
    "cat" to Ic(listOf("M5 8 4 3.5 9 6c1.5-.4 4.5-.4 6 0l5-2.5L19 8c1.2 1.5 2 3.3 2 5.5 0 4.5-4 7.5-9 7.5s-9-3-9-7.5c0-2.2.8-4 2-5.5z", "M10.6 16.2c.6.5 2.2.5 2.8 0", "M2.5 14.5 6.5 14", "M21.5 14.5 17.5 14"), dots = listOf(Triple(8.8f, 12.3f, 1.1f), Triple(15.2f, 12.3f, 1.1f))),
    "fish" to Ic(listOf("M3 12c3-5 8-6 12-3l5-3v12l-5-3c-4 3-9 2-12-3z"), dots = listOf(Triple(7.6f, 11.4f, 0.9f))),
)

/** a different picture for some icons in some themes (the Cozy cat theme uses cats, paws and fish) */
private fun aliasFor(kind: String): String = if (MwStyle.cur.id != "cat") kind else when (kind) {
    "home" -> "cat"; "wallet" -> "cat"; "heart" -> "paw"; "utensils" -> "fish"; "gift" -> "paw"; "target" -> "paw"; else -> kind
}

/** the pixel theme draws every icon on a tiny 20 x 20 grid and blows it up with hard edges */
private fun pixelIcon(ic: Ic): androidx.compose.ui.graphics.ImageBitmap {
    val n = 20
    val bmp = androidx.compose.ui.graphics.ImageBitmap(n, n)
    val c = androidx.compose.ui.graphics.Canvas(bmp)
    val k = n / 24f
    val line = androidx.compose.ui.graphics.Paint().apply {
        isAntiAlias = false; style = androidx.compose.ui.graphics.PaintingStyle.Stroke; strokeWidth = 2.3f
        strokeCap = StrokeCap.Square; strokeJoin = StrokeJoin.Miter; color = Color.White
    }
    val fill = androidx.compose.ui.graphics.Paint().apply { isAntiAlias = false; style = androidx.compose.ui.graphics.PaintingStyle.Fill; color = Color.White }
    c.save(); c.scale(k, k)
    ic.paths.forEach { c.drawPath(PathParser().parsePathString(it).toPath(), line) }
    ic.rects.forEach { c.drawRect(it[0], it[1], it[0] + it[2], it[1] + it[3], line) }
    ic.dots.forEach { c.drawCircle(Offset(it.first, it.second), maxOf(it.third, 1.1f), fill) }
    c.restore()
    return bmp
}

@Composable
fun MwIcon(kind: String, tint: Color, size: Dp = 22.dp, modifier: Modifier = Modifier, weight: Float = 1.7f) {
    val id = MwStyle.cur.id
    val key = aliasFor(kind)
    val ic = ICONS[key] ?: return
    if (id == "pixel") {
        val bmp = remember(key) { pixelIcon(ic) }
        Canvas(modifier.size(size)) {
            drawImage(bmp, dstSize = androidx.compose.ui.unit.IntSize(this.size.width.toInt(), this.size.height.toInt()),
                filterQuality = androidx.compose.ui.graphics.FilterQuality.None, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint))
        }
        return
    }
    val paths = remember(key) { ic.paths.map { PathParser().parsePathString(it).toPath() } }
    // each theme has its own line style: sharp for Phantom and Terminal, chunky for Cozy cat, hairline for Minimal
    val w = when (id) { "phantom" -> weight * 1.3f; "cat" -> weight * 1.2f; "terminal" -> weight * 0.95f; "minimal" -> weight * 0.85f; else -> weight }
    val sharp = id == "phantom" || id == "terminal"
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24f
        scale(u, u, pivot = Offset.Zero) {
            val st = if (sharp) Stroke(w, cap = StrokeCap.Butt, join = StrokeJoin.Miter, miter = 6f) else Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round)
            paths.forEach { drawPath(it, tint, style = st) }
            ic.rects.forEach { drawRoundRect(tint, Offset(it[0], it[1]), Size(it[2], it[3]), CornerRadius(if (sharp) 0.4f else 1.6f), style = st) }
            ic.dots.forEach { drawCircle(tint, it.third, Offset(it.first, it.second)) }
        }
    }
}

private val GLYPHS: Map<String, String> = buildMap {
    fun m(key: String, vararg e: String) = e.forEach { put(it, key) }
    m("banknote", "\uD83D\uDCB5", "\uD83D\uDCB4", "\uD83D\uDCB6", "\uD83D\uDCB7", "\uD83D\uDCB0", "\uD83E\uDD11")
    m("landmark", "\uD83C\uDFE6", "\uD83C\uDFDB")
    m("wallet", "\uD83D\uDC5B", "\uD83D\uDC5D", "\uD83E\uDDE7")
    m("card", "\uD83D\uDCB3")
    m("utensils", "\uD83C\uDF5C", "\uD83C\uDF55", "\uD83C\uDF54", "\uD83C\uDF7D", "\uD83C\uDF74", "\uD83E\uDD57", "\uD83C\uDF71", "\uD83C\uDF5A", "\uD83C\uDF5D", "\uD83C\uDF2E")
    m("coffee", "\u2615", "\uD83E\uDDCB")
    m("bus", "\uD83D\uDE8C", "\uD83D\uDE8E", "\uD83D\uDE8D", "\uD83D\uDE95", "\uD83D\uDE87")
    m("car", "\uD83D\uDE97", "\u26FD")
    m("plane", "\u2708", "\uD83D\uDEEB")
    m("receipt", "\uD83E\uDDFE")
    m("bag", "\uD83D\uDECD")
    m("cart", "\uD83D\uDED2")
    m("book", "\uD83D\uDCDA", "\uD83D\uDCD6", "\uD83C\uDF93", "\uD83D\uDCDD")
    m("pill", "\uD83D\uDC8A", "\uD83C\uDFE5", "\uD83E\uDE7A")
    m("heart", "\u2764", "\uD83D\uDC96", "\uD83D\uDC9C")
    m("phone", "\uD83D\uDCF1", "\uD83D\uDCDE", "\uD83D\uDCF6")
    m("gamepad", "\uD83C\uDFAE")
    m("more", "\uD83E\uDDE9")
    m("briefcase", "\uD83D\uDCBC")
    m("gift", "\uD83C\uDF81")
    m("coin", "\uD83E\uDE99")
    m("calendar", "\uD83D\uDDD3", "\uD83D\uDCC5")
    m("repeat", "\uD83D\uDD01", "\uD83D\uDD04")
    m("swap", "\u21C4")
    m("target", "\uD83C\uDFAF")
    m("zap", "\u26A1")
    m("home", "\uD83C\uDFE0", "\uD83C\uDFE1")
    m("paperclip", "\uD83D\uDCCE")
    m("check", "\u2696")
    m("plus", "+")
    m("minus", "\u2212", "-", "\u2014")
}

/** A category / account / goal symbol. Known emoji become the matching line icon; anything you picked yourself stays as it is. */
@Composable
fun GlyphIcon(glyph: String, tint: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val key = GLYPHS[glyph.replace("\uFE0F", "").trim()]
    if (key != null) MwIcon(key, tint, size, modifier)
    else androidx.compose.material3.Text(glyph.ifBlank { "\u2022" }, modifier, fontSize = (size.value * 0.82f).sp)
}

/** every line icon name (developer gallery) */
fun allIconNames(): List<String> = ICONS.keys.toList()
