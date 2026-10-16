package com.askylix.app

import com.askylix.app.data.remote.ApiError
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.ui.chat.refusesFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which refusals from the server are about the attached file itself, and so mean it must not be offered again. */
class FileRefusalTest {

    @Test
    fun `a file the server will not take is dropped, not put back to fail again`() {
        assertTrue(ApiError(ApiErrorCode.FileUnreadable, 422).refusesFile())
        assertTrue(ApiError(ApiErrorCode.UnsupportedFileType, 415).refusesFile())
        assertTrue(ApiError(ApiErrorCode.FileTooLarge, 413).refusesFile())
    }

    @Test
    fun `a dropped connection or a used-up day says nothing about the file`() {
        assertFalse(ApiError(ApiErrorCode.NoConnection, 0).refusesFile())
        assertFalse(ApiError(ApiErrorCode.Timeout, 0).refusesFile())
        assertFalse(ApiError(ApiErrorCode.QuotaExceeded, 429).refusesFile())
        assertFalse(ApiError(ApiErrorCode.Unauthenticated, 401).refusesFile())
    }
}
