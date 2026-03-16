package cn.edu.ubaa.auth

import cn.edu.ubaa.model.dto.UserData
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.HttpStatusCode
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class AuthServiceResourceTest {

  @Test
  fun withNoRedirectClientClosesDerivedHandleOnSuccess() = runBlocking {
    val authService = AuthService()
    val baseClient = mockClient()
    val derivedClient = mockClient()
    var closeCount = 0

    authService.derivedClientFactory =
      AuthService.DerivedClientFactory { _ ->
        object : AuthService.DerivedClientHandle {
          override val client: HttpClient = derivedClient

          override fun close() {
            closeCount++
            client.close()
          }
        }
      }

    try {
      val result = authService.withNoRedirectClient(baseClient) { "ok" }
      assertEquals("ok", result)
      assertEquals(1, closeCount)
    } finally {
      baseClient.close()
    }
  }

  @Test
  fun withNoRedirectClientClosesDerivedHandleOnFailure() = runBlocking {
    val authService = AuthService()
    val baseClient = mockClient()
    val derivedClient = mockClient()
    var closeCount = 0

    authService.derivedClientFactory =
      AuthService.DerivedClientFactory { _ ->
        object : AuthService.DerivedClientHandle {
          override val client: HttpClient = derivedClient

          override fun close() {
            closeCount++
            client.close()
          }
        }
      }

    try {
      assertFailsWith<IllegalStateException> {
        authService.withNoRedirectClient(baseClient) { throw IllegalStateException("boom") }
      }
      assertEquals(1, closeCount)
    } finally {
      baseClient.close()
    }
  }

  @Test
  fun disposeSessionCandidateClearsCookieStorageBeforeClosingIt() = runBlocking {
    val trackingCookieStorageFactory = TrackingCookieStorageFactory()
    val sessionManager =
      SessionManager(
        sessionStore = InMemorySessionStore(),
        cookieStorageFactory = trackingCookieStorageFactory,
        clientFactory = { mockClient() },
      )

    val candidate = sessionManager.prepareSession("candidate-user")
    val baselineEventCount = trackingCookieStorageFactory.events.size

    try {
      sessionManager.disposeSessionCandidate(candidate)
      val disposeEvents = trackingCookieStorageFactory.events.drop(baselineEventCount)
      assertEquals(listOf("clear", "close"), disposeEvents)
    } finally {
      sessionManager.close()
    }
  }

  @Test
  fun restoreSessionBuildsSingleClientUnderConcurrentLoad() = runBlocking {
    val record =
      SessionPersistence.SessionRecord(
        userData = UserData("Restored User", "10010"),
        authenticatedAt = Instant.now(),
        lastActivity = Instant.now(),
      )
    val sessionStore = DelayedSessionStore(record)
    val clientBuildCount = AtomicInteger(0)
    val sessionManager =
      SessionManager(
        sessionStore = sessionStore,
        cookieStorageFactory = InMemoryCookieStorageFactory(),
        clientFactory = { _: CookiesStorage ->
          clientBuildCount.incrementAndGet()
          mockClient()
        },
      )

    try {
      coroutineScope {
        val first = async { sessionManager.getSession("restored", SessionManager.SessionAccess.READ_ONLY) }
        val second = async { sessionManager.getSession("restored", SessionManager.SessionAccess.READ_ONLY) }

        val firstSession = first.await()
        val secondSession = second.await()

        assertNotNull(firstSession)
        assertNotNull(secondSession)
        assertSame(firstSession, secondSession)
      }

      assertEquals(1, clientBuildCount.get())
      assertEquals(1, sessionStore.loadCount.get())
    } finally {
      sessionManager.close()
    }
  }

  private fun mockClient(): HttpClient {
    return HttpClient(MockEngine) {
      engine {
        addHandler { respond(content = "", status = HttpStatusCode.OK) }
      }
    }
  }

  private class DelayedSessionStore(private val record: SessionPersistence.SessionRecord) :
    SessionPersistence {
    val loadCount = AtomicInteger(0)

    override suspend fun saveSession(
      username: String,
      userData: UserData,
      authenticatedAt: Instant,
      lastActivity: Instant,
    ) {}

    override suspend fun updateLastActivity(username: String, lastActivity: Instant) {}

    override suspend fun loadSession(username: String): SessionPersistence.SessionRecord? {
      loadCount.incrementAndGet()
      delay(50)
      return record
    }

    override suspend fun deleteSession(username: String) {}

    override fun close() {}
  }
}
