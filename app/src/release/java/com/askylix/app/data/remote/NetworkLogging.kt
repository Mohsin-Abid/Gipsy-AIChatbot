package com.askylix.app.data.remote

import okhttp3.OkHttpClient

/** Release builds log nothing about the network. The debug twin of this file adds a header-only logger. */
@Suppress("UnusedReceiverParameter")
internal fun OkHttpClient.Builder.addDebugLogging() = Unit
