package be.zvz.semy

import io.ktor.client.request.*
import io.ktor.server.testing.*
import kotlin.test.*
import be.zvz.semy.plugins.configureHTTP
import be.zvz.semy.plugins.configureSockets

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureHTTP()
            configureSockets()
        }
        client.post("/inference").apply {

        }
    }
}
