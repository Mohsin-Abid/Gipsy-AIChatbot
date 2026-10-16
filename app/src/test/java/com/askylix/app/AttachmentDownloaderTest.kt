package com.askylix.app

import com.askylix.app.data.attachment.AttachmentDownloader
import com.askylix.app.data.remote.ApiClient
import com.askylix.app.data.remote.ApiError
import com.askylix.app.data.remote.ApiErrorCode
import com.askylix.app.data.remote.TokenProvider
import com.askylix.app.domain.model.Attachment
import com.askylix.app.domain.model.ScanSource
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Fetching a file from the link the server gave: when it downloads, when it does not, and when the link has lapsed. */
class AttachmentDownloaderTest {

    private lateinit var server: MockWebServer
    private lateinit var folder: File
    private lateinit var downloader: AttachmentDownloader

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        folder = Files.createTempDirectory("downloads").toFile()
        val client = ApiClient(server.url("/v1/").toString(), TokenProvider { "SECRET-TOKEN" })
        downloader = AttachmentDownloader(client.files, downloadsDirectory = { folder })
    }

    @After
    fun stop() {
        runCatching { server.shutdown() }
        folder.deleteRecursively()
    }

    private fun link(name: String) = server.url("/v1/files/$name?expires=1&signature=abc").toString()
    private fun attachment(url: String? = link("1"), localPath: String? = null, name: String = "tiny.jpg") =
        Attachment(ScanSource.Gallery, name, "image/jpeg", 10, localPath = localPath, url = url)

    @Test
    fun `a file is downloaded to the cache without the sign-in token`() = runBlocking {
        server.enqueue(MockResponse().setBody("picture-bytes"))

        val file = downloader.fetch("msg_1", attachment(), refreshLink = { null }).getOrThrow()

        assertEquals("picture-bytes", file.readText())
        assertTrue(file.absolutePath.startsWith(folder.absolutePath))
        assertNull("the token stays off a signed link", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `a file already downloaded is not fetched again`() = runBlocking {
        server.enqueue(MockResponse().setBody("picture-bytes"))

        downloader.fetch("msg_1", attachment(), refreshLink = { null }).getOrThrow()
        downloader.fetch("msg_1", attachment(), refreshLink = { null }).getOrThrow()

        assertEquals(1, server.requestCount)
    }

    @Test
    fun `this phone's own copy of a file it sent is used without any download`() = runBlocking {
        val own = File(folder, "mine.jpg").apply { writeText("original") }

        val file = downloader.fetch("msg_1", attachment(url = null, localPath = own.absolutePath), refreshLink = { null }).getOrThrow()

        assertEquals(own, file)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `an expired link is replaced once and the download tried again`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse().setBody("fresh-bytes"))
        var refreshed = 0

        val file = downloader.fetch("msg_1", attachment(link("old")), refreshLink = { refreshed++; link("new") }).getOrThrow()

        assertEquals("fresh-bytes", file.readText())
        assertEquals(1, refreshed)
        assertTrue(server.takeRequest().path!!.contains("/files/old"))
        assertTrue(server.takeRequest().path!!.contains("/files/new"))
    }

    @Test
    fun `a link that keeps failing is given up on after one replacement`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse().setResponseCode(403))

        val result = downloader.fetch("msg_1", attachment(), refreshLink = { link("new") })

        assertEquals(2, server.requestCount)
        assertTrue(result.isFailure)
    }

    @Test
    fun `a file with no link at all is fetched by asking for one`() = runBlocking {
        server.enqueue(MockResponse().setBody("bytes"))

        val file = downloader.fetch("msg_1", attachment(url = null), refreshLink = { link("asked") }).getOrThrow()

        assertEquals("bytes", file.readText())
    }

    @Test
    fun `no link and none to be had is a not-found`() = runBlocking {
        val error = downloader.fetch("msg_1", attachment(url = null), refreshLink = { null }).exceptionOrNull() as ApiError

        assertEquals(ApiErrorCode.NotFound, error.code)
    }

    @Test
    fun `a server error leaves no half-written file behind`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = downloader.fetch("msg_1", attachment(), refreshLink = { null })

        assertTrue(result.isFailure)
        assertTrue(folder.walkTopDown().none { it.isFile })
    }

    @Test
    fun `a file larger than the limit is refused before it is written`() = runBlocking {
        server.enqueue(MockResponse().setBody("x").setHeader("Content-Length", (21L * 1024 * 1024).toString()))

        val result = downloader.fetch("msg_1", attachment(), refreshLink = { null })

        assertTrue(result.exceptionOrNull() is ApiError)
        assertTrue(folder.walkTopDown().none { it.isFile })
    }

    @Test
    fun `progress is reported up to a hundred`() = runBlocking {
        server.enqueue(MockResponse().setBody("0123456789"))
        val seen = mutableListOf<Int>()

        downloader.fetch("msg_1", attachment(), refreshLink = { null }, onProgress = { seen += it }).getOrThrow()

        assertTrue(seen.isNotEmpty())
        assertEquals(100, seen.last())
    }

    @Test
    fun `an awkward file name cannot escape the folder`() = runBlocking {
        server.enqueue(MockResponse().setBody("bytes"))

        val file = downloader.fetch("msg/../1", attachment(name = "../../etc/passwd"), refreshLink = { null }).getOrThrow()

        assertTrue(file.canonicalPath.startsWith(folder.canonicalPath))
        assertFalse(file.name.contains("/"))
    }
}
