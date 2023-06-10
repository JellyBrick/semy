package be.zvz

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.http.*
import be.zvz.plugins.*

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
