package cn.edu.ubaa.auth

import cn.edu.ubaa.utils.VpnCipher
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

/** 专门负责BYXT的会话初始化和验证。 */
object ByxtService {
  private val log = LoggerFactory.getLogger(ByxtService::class.java)
  private val BYXT_INDEX_URL =
      VpnCipher.toVpnUrl("https://byxt.buaa.edu.cn/jwapp/sys/homeapp/api/home/currentUser.do")

  /** 初始化 BYXT 会话。 */
  suspend fun initializeSession(client: HttpClient): Boolean {
    log.debug("Initializing BYXT session...")
    return try {
      val indexResponse = client.get(BYXT_INDEX_URL)
      val body = indexResponse.bodyAsText()
      val ready = isSessionReady(indexResponse.status, indexResponse.request.url.toString(), body)
      log.debug(
          "BYXT index page accessed. Status: {}, Final URL: {}, Ready: {}",
          indexResponse.status,
          indexResponse.request.url,
          ready,
      )
      ready
    } catch (e: Exception) {
      log.error("Failed to initialize BYXT session", e)
      false
    }
  }

  fun isSessionReady(status: HttpStatusCode, finalUrl: String, body: String): Boolean {
    if (status != HttpStatusCode.OK) return false
    if (finalUrl.contains("sso.buaa.edu.cn", ignoreCase = true)) return false
    val trimmed = body.trimStart()
    if (trimmed.startsWith("<!DOCTYPE html", ignoreCase = true) || trimmed.startsWith("<html", ignoreCase = true)) {
      return false
    }
    if (body.contains("input name=\"execution\"") || body.contains("统一身份认证", ignoreCase = true)) {
      return false
    }
    return trimmed.startsWith("{") || trimmed.startsWith("[")
  }
}
