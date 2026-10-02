package com.aitutor.chatbot.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The design's icons, transcribed from its SVG path data rather than approximated with stock
 * Material glyphs — the line weight and corner rounding are a visible part of the look.
 *
 * Everything is drawn in black and recoloured by `Icon(tint = …)` at the call site, so one vector
 * serves every colour it appears in.
 */
object AppIcons {

    /** The brand mark: an open book under a four-point spark. */
    val Logo: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppLogo",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f,
        )
            .addStroke("M6 18c6-3 12-3 18 1v21c-6-4-12-4-18-1z", width = 3.4f)
            .addStroke("M42 18c-6-3-12-3-18 1v21c6-4 12-4 18-1z", width = 3.4f)
            .addFill("M24 2l2.1 5.4 5.4 2.1-5.4 2.1L24 17l-2.1-5.4-5.4-2.1 5.4-2.1z")
            .build()
    }

    /** The AI spark, used wherever an answer is attributed to the tutor. */
    val Spark: ImageVector by lazy {
        filled(
            "Spark",
            "M12 2.5l2.1 6.1 6.1 2.1-6.1 2.1L12 19l-2.1-6.2-6.1-2.1 6.1-2.1z",
            "M19 15.5l.8 2 2 .8-2 .8-.8 2-.8-2-2-.8 2-.8z",
        )
    }

    val ArrowRight: ImageVector by lazy { stroked("ArrowRight", "M5 12h14M13 6l6 6-6 6", width = 2.2f) }
    val Check: ImageVector by lazy { stroked("Check", "M5 12.5l4.5 4.5L19 7.5", width = 3f) }

    val Globe: ImageVector by lazy {
        stroked(
            "Globe",
            "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0",
            "M3 12h18",
            "M12 3a14 14 0 0 1 0 18a14 14 0 0 1 0-18z",
            width = 1.8f,
        )
    }

    val Search: ImageVector by lazy {
        stroked("Search", "M18 11a7 7 0 1 1-14 0a7 7 0 1 1 14 0", "M20 20l-3.5-3.5", width = 2f)
    }

    val ScanFrame: ImageVector by lazy {
        stroked(
            "ScanFrame",
            "M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2",
            "M7 12h10",
            width = 1.9f,
        )
    }

    // ---- Home & navigation ----
    val ChevronRight: ImageVector by lazy { stroked("ChevronRight", "M9 6l6 6-6 6", width = 2f) }
    val ArrowUpRight: ImageVector by lazy { stroked("ArrowUpRight", "M7 17L17 7", "M8 7h9v9", width = 2.2f) }
    val SendUp: ImageVector by lazy { stroked("SendUp", "M12 19V5", "M5 12l7-7 7 7", width = 2.2f) }
    val Play: ImageVector by lazy {
        filled("Play", "M8 5.5v13a1 1 0 0 0 1.5.9l10.2-6.5a1 1 0 0 0 0-1.8L9.5 4.6A1 1 0 0 0 8 5.5z")
    }
    val Mic: ImageVector by lazy {
        stroked(
            "Mic",
            "M12 2a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V5a3 3 0 0 1 3-3z",
            "M19 10v1a7 7 0 0 1-14 0v-1",
            "M12 18v4",
            width = 1.8f,
        )
    }
    val HomeFilled: ImageVector by lazy {
        filled(
            "HomeFilled",
            "M11.3 2.6a1 1 0 0 1 1.4 0l8.5 7.4a1 1 0 0 1 .3.8V20a2 2 0 0 1-2 2h-4v-6.5a1 1 0 0 0-1-1h-5a1 1 0 0 0-1 1V22h-4a2 2 0 0 1-2-2v-9.2a1 1 0 0 1 .3-.8z",
        )
    }
    val HomeOutline: ImageVector by lazy {
        stroked(
            "HomeOutline",
            "M11.3 2.6a1 1 0 0 1 1.4 0l8.5 7.4a1 1 0 0 1 .3.8V20a2 2 0 0 1-2 2h-4v-6.5a1 1 0 0 0-1-1h-5a1 1 0 0 0-1 1V22h-4a2 2 0 0 1-2-2v-9.2a1 1 0 0 1 .3-.8z",
        )
    }
    val Grid: ImageVector by lazy {
        stroked(
            "Grid",
            roundRect(3f, 3f, 7.5f, 7.5f, 2f),
            roundRect(13.5f, 3f, 7.5f, 7.5f, 2f),
            roundRect(3f, 13.5f, 7.5f, 7.5f, 2f),
            roundRect(13.5f, 13.5f, 7.5f, 7.5f, 2f),
        )
    }
    val HistoryClock: ImageVector by lazy {
        stroked("HistoryClock", "M3 12a9 9 0 1 0 3-6.7L3 8", "M3 3v5h5", "M12 7v5l3 2")
    }

    // ---- Tool glyphs ----
    val Paraphrase: ImageVector by lazy {
        stroked(
            "Paraphrase",
            "M17 2l4 4-4 4",
            "M3 11v-1a4 4 0 0 1 4-4h14",
            "M7 22l-4-4 4-4",
            "M21 13v1a4 4 0 0 1-4 4H3",
            width = 1.8f,
        )
    }
    val SummaryLines: ImageVector by lazy {
        stroked("SummaryLines", "M4 6h16M4 12h10M4 18h7", "M18 13l-2 4h4l-2 4", width = 1.8f)
    }

    // ---- Setup flow ----
    val ChevronLeft: ImageVector by lazy { stroked("ChevronLeft", "M15 18l-6-6 6-6", width = 2.1f) }
    val ChevronDown: ImageVector by lazy { stroked("ChevronDown", "M6 9l6 6 6-6", width = 2.2f) }
    val Plus: ImageVector by lazy { stroked("Plus", "M12 5v14M5 12h14", width = 2.2f) }

    val Person: ImageVector by lazy {
        stroked("Person", circle(12f, 8f, 4f), "M4 21a8 8 0 0 1 16 0", width = 1.9f)
    }
    val School: ImageVector by lazy {
        stroked("School", "M3 21h18", "M5 21V10l7-5 7 5v11", "M10 21v-5h4v5", "M12 5V2h3", width = 1.8f)
    }
    val Book: ImageVector by lazy {
        stroked(
            "Book",
            "M4 19.5A2.5 2.5 0 0 1 6.5 17H20",
            "M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z",
            width = 1.9f,
        )
    }
    val College: ImageVector by lazy {
        stroked(
            "College",
            "M4 19.5A2.5 2.5 0 0 1 6.5 17H20",
            "M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z",
            "M9 7h7M9 11h5",
            width = 1.8f,
        )
    }
    val Target: ImageVector by lazy {
        stroked("Target", circle(12f, 12f, 9f), circle(12f, 12f, 5f), circle(12f, 12f, 1f), width = 1.9f)
    }
    val Clock: ImageVector by lazy {
        stroked("Clock", circle(12f, 12f, 9f), "M12 7v5l3 2", width = 1.9f)
    }
    /** The unlimited-questions mark: a lemniscate, drawn as one continuous stroke. */
    val Infinity: ImageVector by lazy {
        stroked(
            "Infinity",
            "M18.2 8.4a3.6 3.6 0 1 1 0 7.2c-3 0-5.4-7.2-9.4-7.2a3.6 3.6 0 1 0 0 7.2c4 0 6.4-7.2 9.4-7.2z",
            width = 1.9f,
        )
    }

    // ---- Chat ----
    val ScanText: ImageVector by lazy {
        stroked(
            "ScanText",
            "M3 7V5a2 2 0 0 1 2-2h2M17 3h2a2 2 0 0 1 2 2v2M21 17v2a2 2 0 0 1-2 2h-2M7 21H5a2 2 0 0 1-2-2v-2",
            "M7 9h10M7 12h10M7 15h6",
            width = 1.9f,
        )
    }
    val DotsVertical: ImageVector by lazy {
        filled("DotsVertical", circle(12f, 5f, 1.7f), circle(12f, 12f, 1.7f), circle(12f, 19f, 1.7f))
    }
    val DotsHorizontal: ImageVector by lazy {
        filled("DotsHorizontal", circle(5f, 12f, 1.7f), circle(12f, 12f, 1.7f), circle(19f, 12f, 1.7f))
    }
    val Copy: ImageVector by lazy {
        stroked(
            "Copy",
            roundRect(9f, 9f, 12f, 12f, 2.5f),
            "M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1",
            width = 1.9f,
        )
    }
    val SpeakerWave: ImageVector by lazy {
        stroked(
            "SpeakerWave",
            "M11 5L6 9H2v6h4l5 4z",
            "M15.5 8.5a5 5 0 0 1 0 7M19 5a10 10 0 0 1 0 14",
            width = 1.9f,
        )
    }
    val ThumbUp: ImageVector by lazy { stroked("ThumbUp", *ThumbPaths, width = 1.9f) }

    /** The same glyph as [ThumbUp], turned about the icon's centre rather than redrawn mirrored. */
    val ThumbDown: ImageVector by lazy {
        var builder = ImageVector.Builder(
            name = "ThumbDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addGroup(rotate = 180f, pivotX = 12f, pivotY = 12f)
        ThumbPaths.forEach { builder = builder.addStroke(it, 1.9f) }
        builder.clearGroup().build()
    }
    val Refresh: ImageVector by lazy {
        stroked("Refresh", "M21 12a9 9 0 1 1-2.6-6.4L21 8", "M21 3v5h-5", width = 1.9f)
    }
    val Close: ImageVector by lazy { stroked("Close", "M6 6l12 12M18 6L6 18", width = 2.2f) }
    val Camera: ImageVector by lazy {
        stroked(
            "Camera",
            "M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z",
            circle(12f, 13.5f, 3.5f),
            width = 1.8f,
        )
    }
    val Gallery: ImageVector by lazy {
        stroked(
            "Gallery",
            roundRect(3f, 3f, 18f, 18f, 3f),
            circle(8.5f, 8.5f, 1.8f),
            "M21 15l-5-5L5 21",
            width = 1.8f,
        )
    }
    val PdfFile: ImageVector by lazy {
        stroked(
            "PdfFile",
            "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z",
            "M14 2v6h6",
            "M8 17v-4h1.5a1.3 1.3 0 0 1 0 2.6H8",
            "M13 13v4h1a2 2 0 0 0 0-4z",
            width = 1.8f,
        )
    }
    val WordFile: ImageVector by lazy {
        stroked(
            "WordFile",
            "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z",
            "M14 2v6h6",
            "M7.5 12l1.5 5 1.5-3.5 1.5 3.5 1.5-5",
            width = 1.8f,
        )
    }
    val Lock: ImageVector by lazy {
        stroked("Lock", roundRect(4f, 11f, 16f, 10f, 2f), "M8 11V8a4 4 0 0 1 8 0v3", width = 2f)
    }
    val Share: ImageVector by lazy {
        stroked(
            "Share",
            circle(18f, 5f, 3f),
            circle(6f, 12f, 3f),
            circle(18f, 19f, 3f),
            "M8.6 13.5l6.8 4M15.4 6.5l-6.8 4",
            width = 2f,
        )
    }
    val TextSize: ImageVector by lazy {
        stroked("TextSize", "M4 20l5-14 5 14", "M6 15h6", "M16 20l2.5-7 2.5 7", width = 2f)
    }
    val Eraser: ImageVector by lazy {
        stroked(
            "Eraser",
            "M20 20H9L4 15a2 2 0 0 1 0-2.8l8.2-8.2a2 2 0 0 1 2.8 0L20 9a2 2 0 0 1 0 2.8L13 19",
            "M9 11l6 6",
            width = 2f,
        )
    }
    val Trash: ImageVector by lazy {
        stroked(
            "Trash",
            "M3 6h18",
            "M8 6V4h8v2",
            "M6 6l1 14a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-14",
            width = 2f,
        )
    }
    val Contract: ImageVector by lazy {
        stroked("Contract", "M4 14h6v6M20 10h-6V4M14 10l7-7M3 21l7-7", width = 2f)
    }
    val Expand: ImageVector by lazy {
        stroked("Expand", "M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7", width = 2f)
    }
    val SelectText: ImageVector by lazy {
        stroked("SelectText", "M4 7V4h16v3M9 20h6M12 4v16", width = 2f)
    }
    val VoiceWave: ImageVector by lazy {
        stroked("VoiceWave", "M3 10v4M7 7v10M11 4v16M15 8v8M19 11v2", width = 2f)
    }
    val Flag: ImageVector by lazy {
        stroked("Flag", "M4 22V4", "M4 4h12l-2 4 2 4H4", width = 2f)
    }
    val Info: ImageVector by lazy {
        stroked("Info", circle(12f, 12f, 9f), "M12 11v5", "M12 8h.01", width = 2f)
    }

    // ---- Settings and account glyphs ----
    val Filter: ImageVector by lazy {
        stroked(
            "Filter",
            "M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12",
            circle(16f, 6f, 2f),
            circle(10f, 12f, 2f),
            circle(18f, 18f, 2f),
        )
    }
    val Settings: ImageVector by lazy {
        stroked(
            "Settings",
            circle(12f, 12f, 3f),
            "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
        )
    }
    val Sun: ImageVector by lazy {
        stroked(
            "Sun",
            circle(12f, 12f, 4f),
            "M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4",
            width = 1.8f,
        )
    }

    /** "Follow the system": a circle half filled, the conventional mark for automatic contrast. */
    val Contrast: ImageVector by lazy {
        ImageVector.Builder(
            name = "Contrast",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
            .addStroke(circle(12f, 12f, 9f), 1.8f)
            .addFill("M12 3a9 9 0 0 1 0 18z")
            .build()
    }

    val Moon: ImageVector by lazy {
        stroked("Moon", "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z", width = 1.8f)
    }
    val Card: ImageVector by lazy {
        stroked("Card", roundRect(2f, 5f, 20f, 14f, 2f), "M2 10h20", width = 1.8f)
    }
    val Shield: ImageVector by lazy {
        stroked("Shield", "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z", width = 1.8f)
    }

    // ---- Sign-in benefits. Transcribed from the SignInC artboard. ----

    val CloudCheck: ImageVector by lazy {
        stroked(
            "CloudCheck",
            "M17.5 19H7a5 5 0 1 1 1.2-9.9A6 6 0 0 1 19.5 11a4 4 0 0 1-2 8z",
            "M9.5 14.5l2 2 3.5-3.5",
            width = 2f,
        )
    }

    /** [Shield] with a tick inside it. Kept separate, since the plain shield heads a Profile row. */
    val ShieldCheck: ImageVector by lazy {
        stroked(
            "ShieldCheck",
            "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z",
            "M9 12l2 2 4-4",
            width = 2f,
        )
    }

    /** A monitor beside a phone. The design draws it as two rounded rectangles and a stand. */
    val Devices: ImageVector by lazy {
        stroked(
            "Devices",
            "M4 4h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
            "M6 18h6",
            "M18.5 8h2a1.5 1.5 0 0 1 1.5 1.5v9a1.5 1.5 0 0 1-1.5 1.5h-2A1.5 1.5 0 0 1 17 18.5v-9A1.5 1.5 0 0 1 18.5 8z",
            width = 2f,
        )
    }
    val Help: ImageVector by lazy {
        stroked("Help", circle(12f, 12f, 9f), "M9.1 9a3 3 0 0 1 5.8 1c0 2-3 3-3 3", "M12 17h.01", width = 1.8f)
    }
    val Star: ImageVector by lazy {
        stroked("Star", "M12 2.5l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.1 1.2-6.5-4.8-4.6 6.6-.9z", width = 1.8f)
    }
    val Logout: ImageVector by lazy {
        stroked("Logout", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4", "M16 17l5-5-5-5", "M21 12H9", width = 2f)
    }
    val Pencil: ImageVector by lazy {
        stroked("Pencil", "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z", width = 2f)
    }

    /** The "upgrade" crown. Solid, so it reads at 22dp inside a filled accent tile. */
    val Crown: ImageVector by lazy { filled("Crown", "M3 7l4.5 4L12 4l4.5 7L21 7l-2 12H5z") }

    val Bell: ImageVector by lazy {
        stroked(
            "Bell",
            "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9",
            "M13.7 21a2 2 0 0 1-3.4 0",
            width = 1.9f,
        )
    }

    // ---- Subjects ----
    val Chemistry: ImageVector by lazy {
        stroked(
            "Chemistry",
            "M9 3h6M10 3v6l-5 9a2 2 0 0 0 1.7 3h10.6a2 2 0 0 0 1.7-3l-5-9V3",
            "M7.5 15h9",
            width = 1.9f,
        )
    }
    val Biology: ImageVector by lazy {
        stroked(
            "Biology",
            "M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.5 19 2c1 2 2 4.2 2 8 0 5.5-4.8 10-10 10z",
            "M2 21c0-3 1.9-5.4 5.2-6",
            width = 1.9f,
        )
    }
    val EnglishSubject: ImageVector by lazy {
        stroked(
            "EnglishSubject",
            "M4 16l4-10 4 10",
            "M5.5 12.5h5",
            "M14 9h4a2 2 0 0 1 0 4h-4V6h3.5a1.5 1.5 0 0 1 0 3",
            "M14 13h4.5a2 2 0 0 1 0 4H14z",
            width = 1.9f,
        )
    }
    val History: ImageVector by lazy {
        stroked("History", "M3 21h18M5 21V10M19 21V10M9 21V10M15 21V10M12 3l9 5H3z", width = 1.9f)
    }
    val Code: ImageVector by lazy {
        stroked("Code", "M8 7l-5 5 5 5M16 7l5 5-5 5", width = 1.9f)
    }
    val Economics: ImageVector by lazy {
        stroked("Economics", "M4 20V10M10 20V4M16 20v-8M22 20H2", width = 1.9f)
    }
    val Languages: ImageVector by lazy {
        stroked(
            "Languages",
            "M4 5h8M8 3v2M6 5c0 4 2 7 6 9M10 5c-1 4-3 7-6 9",
            "M13 21l4-9 4 9M14.5 18h5",
            width = 1.9f,
        )
    }
    val Art: ImageVector by lazy {
        stroked(
            "Art",
            "M12 3a9 9 0 1 0 0 18c1.1 0 2-.9 2-2 0-.5-.2-1-.5-1.3-.3-.4-.5-.8-.5-1.3 0-1.1.9-2 2-2h2.3A4.7 4.7 0 0 0 21 9.8C21 6 17 3 12 3z",
            circle(7.5f, 11f, 1f),
            circle(10.5f, 7.5f, 1f),
            circle(15f, 7.5f, 1f),
            width = 1.9f,
        )
    }

    /** Physics: a nucleus inside three orbits, each the same ellipse turned 60 degrees further. */
    val Physics: ImageVector by lazy {
        var builder = ImageVector.Builder(
            name = "Physics",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        builder = builder.addStroke(circle(12f, 12f, 1.6f), 1.9f)
        listOf(0f, 60f, 120f).forEach { angle ->
            builder = builder
                .addGroup(name = "orbit$angle", rotate = angle, pivotX = 12f, pivotY = 12f)
                .addStroke(ellipse(12f, 12f, 10f, 4f), 1.9f)
                .clearGroup()
        }
        builder.build()
    }

    // ---- Study-tool glyphs, in the order the design lays them out ----
    val Chat: ImageVector by lazy {
        stroked("Chat", "M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z", "M8.5 12h.01M12 12h.01M15.5 12h.01")
    }
    val Quiz: ImageVector by lazy {
        stroked(
            "Quiz",
            "M9 2h6a1 1 0 0 1 1 1v2a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V3a1 1 0 0 1 1-1z",
            "M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2",
            "M9 14l2 2 4-4",
        )
    }
    val Math: ImageVector by lazy {
        stroked("Math", "M7 4v6M4 7h6", "M14 7h6", "M5 15l4 4M9 15l-4 4", "M14 15.5h6M14 18.5h6")
    }
    val Document: ImageVector by lazy {
        stroked("Document", "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z", "M14 2v6h6", "M8 13h8M8 17h5")
    }
    val Grammar: ImageVector by lazy {
        stroked("Grammar", "M4 16l4-10 4 10", "M5.5 12.5h5", "M13 17l3 3 5-6")
    }
    val Write: ImageVector by lazy {
        stroked("Write", "M12 20h9", "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z")
    }
    val Study: ImageVector by lazy {
        stroked("Study", "M22 10L12 5 2 10l10 5 10-5z", "M6 12v5c3 2 9 2 12 0v-5", "M22 10v6")
    }
    val Idea: ImageVector by lazy {
        stroked(
            "Idea",
            "M9 18h6",
            "M10 22h4",
            "M15.1 14c.2-1 .7-1.7 1.4-2.5A5.9 5.9 0 0 0 18 8 6 6 0 0 0 6 8c0 1.4.5 2.6 1.5 3.5.7.8 1.2 1.5 1.4 2.5",
        )
    }
}

/** The thumb outline, shared by [AppIcons.ThumbUp] and its rotated twin. */
private val ThumbPaths = arrayOf(
    "M7 10v11H4a1 1 0 0 1-1-1v-9a1 1 0 0 1 1-1z",
    "M7 10l4-8a3 3 0 0 1 3 3v4h5.5a2 2 0 0 1 2 2.3l-1.4 8A2 2 0 0 1 18 21H7",
)

// ---- builders ----

private fun stroked(name: String, vararg paths: String, width: Float = 1.7f): ImageVector {
    var builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    paths.forEach { builder = builder.addStroke(it, width) }
    return builder.build()
}

private fun filled(name: String, vararg paths: String): ImageVector {
    var builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    paths.forEach { builder = builder.addFill(it) }
    return builder.build()
}

private fun ImageVector.Builder.addStroke(pathData: String, width: Float): ImageVector.Builder =
    addPath(
        pathData = addPathNodes(pathData),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )

private fun ImageVector.Builder.addFill(pathData: String): ImageVector.Builder =
    addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))

/** SVG `<circle>` has no path equivalent, so it is expressed as two half-arcs. */
private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx + r} ${cy}a$r $r 0 1 1 ${-2 * r} 0a$r $r 0 1 1 ${2 * r} 0"

/** SVG `<rect rx>`, which likewise has no path form. */
private fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float): String =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}" +
        "a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float): String =
    "M${cx + rx} ${cy}a$rx $ry 0 1 1 ${-2 * rx} 0a$rx $ry 0 1 1 ${2 * rx} 0"
