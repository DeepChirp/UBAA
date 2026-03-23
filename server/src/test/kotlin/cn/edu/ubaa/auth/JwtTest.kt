package cn.edu.ubaa.auth

import cn.edu.ubaa.model.dto.UserData
import cn.edu.ubaa.utils.JwtUtil
import java.time.Duration
import kotlin.test.*
import kotlinx.coroutines.runBlocking

class JwtUtilTest {

  @Test
  fun testJwtGenerationAndValidation() {
    val username = "testuser"
    val ttl = Duration.ofMinutes(30)

    val token = JwtUtil.generateToken(username, ttl)
    assertNotNull(token)
    assertTrue(token.isNotBlank())

    val extractedUsername = JwtUtil.validateTokenAndGetUsername(token)
    assertEquals(username, extractedUsername)
  }

  @Test
  fun testJwtExpiration() {
    val username = "testuser"
    val shortTtl = Duration.ofMillis(1)

    val token = JwtUtil.generateToken(username, shortTtl)
    Thread.sleep(10)

    val extractedUsername = JwtUtil.validateTokenAndGetUsername(token)
    assertNull(extractedUsername)
    assertTrue(JwtUtil.isTokenExpired(token))
  }

  @Test
  fun testInvalidToken() {
    val invalidToken = "invalid.jwt.token"
    val extractedUsername = JwtUtil.validateTokenAndGetUsername(invalidToken)
    assertNull(extractedUsername)
    assertTrue(JwtUtil.isTokenExpired(invalidToken))
  }
}

class SessionManagerJwtTest {
  private fun createSessionManager(
      sessionTtl: Duration = Duration.ofMinutes(30),
      activityPersistInterval: Duration = Duration.ofSeconds(60),
  ): SessionManager {
    return SessionManager(
        sessionTtl = sessionTtl,
        activityPersistInterval = activityPersistInterval,
        sessionStore = InMemorySessionStore(),
        cookieStorageFactory = InMemoryCookieStorageFactory(),
    )
  }

  @Test
  fun testSessionWithTokenCommit() = runBlocking {
    val sessionManager = createSessionManager()
    val username = "testuser"
    val userData = UserData("Test User", "123456")

    val candidate = sessionManager.prepareSession(username)
    val sessionWithToken = sessionManager.commitSessionWithToken(candidate, userData)

    assertNotNull(sessionWithToken.jwtToken)
    assertEquals(userData, sessionWithToken.session.userData)
    assertEquals(username, sessionWithToken.session.username)

    val retrievedSession = sessionManager.getSessionByToken(sessionWithToken.jwtToken)
    assertNotNull(retrievedSession)
    assertEquals(username, retrievedSession.username)
  }

  @Test
  fun testGetSessionByInvalidToken() = runBlocking {
    val sessionManager = createSessionManager()
    val session = sessionManager.getSessionByToken("invalid.token")
    assertNull(session)
  }

  @Test
  fun testReadOnlySessionAccessDoesNotTouchLastActivity() = runBlocking {
    val sessionManager = createSessionManager(activityPersistInterval = Duration.ofMillis(1))
    val username = "readonly-user"
    val userData = UserData("Read Only", "10001")

    val candidate = sessionManager.prepareSession(username)
    val sessionWithToken = sessionManager.commitSessionWithToken(candidate, userData)
    val before = sessionWithToken.session.lastActivity()

    Thread.sleep(10)

    val readOnlySession =
        sessionManager.getSession(username, SessionManager.SessionAccess.READ_ONLY)
    assertNotNull(readOnlySession)
    assertEquals(before, readOnlySession.lastActivity())
  }

  @Test
  fun testInvalidateSessionClearsCookieStorageBeforeClosingIt() = runBlocking {
    val trackingCookieStorageFactory = TrackingCookieStorageFactory()
    val sessionManager =
        SessionManager(
            sessionStore = InMemorySessionStore(),
            cookieStorageFactory = trackingCookieStorageFactory,
        )
    val username = "logout-user"
    val userData = UserData("Logout User", "10003")

    val candidate = sessionManager.prepareSession(username)
    sessionManager.commitSessionWithToken(candidate, userData)
    val baselineEventCount = trackingCookieStorageFactory.events.size

    sessionManager.invalidateSession(username)

    val invalidateEvents = trackingCookieStorageFactory.events.drop(baselineEventCount)
    assertTrue(invalidateEvents.contains("clear"))
    assertTrue(invalidateEvents.contains("close"))
    assertTrue(invalidateEvents.indexOf("clear") < invalidateEvents.indexOf("close"))
  }
}
