package cn.edu.ubaa.cgyy

import cn.edu.ubaa.auth.ErrorDetails
import cn.edu.ubaa.auth.ErrorResponse
import cn.edu.ubaa.auth.JwtAuth.jwtUsername
import cn.edu.ubaa.model.dto.CgyyReservationSubmitRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.cgyyRouting() {
  val cgyyService = GlobalCgyyService.instance

  route("/api/v1/cgyy") {
    get("/sites") {
      val username = call.jwtUsername!!
      try {
        call.respond(HttpStatusCode.OK, cgyyService.getVenueSites(username))
      } catch (e: CgyyException) {
        call.respondCgyyError(e)
      }
    }

    get("/purpose-types") {
      val username = call.jwtUsername!!
      try {
        call.respond(HttpStatusCode.OK, cgyyService.getPurposeTypes(username))
      } catch (e: CgyyException) {
        call.respondCgyyError(e)
      }
    }

    get("/day-info") {
      val username = call.jwtUsername!!
      val venueSiteId =
          call.request.queryParameters["venueSiteId"]?.toIntOrNull()
              ?: return@get call.respond(
                  HttpStatusCode.BadRequest,
                  ErrorResponse(ErrorDetails("invalid_request", "venueSiteId is required")),
              )
      val date =
          call.request.queryParameters["date"]
              ?: return@get call.respond(
                  HttpStatusCode.BadRequest,
                  ErrorResponse(ErrorDetails("invalid_request", "date is required")),
              )
      try {
        call.respond(HttpStatusCode.OK, cgyyService.getDayInfo(username, venueSiteId, date))
      } catch (e: CgyyException) {
        call.respondCgyyError(e)
      }
    }

    post("/reservations") {
      val username = call.jwtUsername!!
      val request =
          try {
            call.receive<CgyyReservationSubmitRequest>()
          } catch (_: Exception) {
            return@post call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(ErrorDetails("invalid_request", "Invalid request body")),
            )
          }
      try {
        call.respond(HttpStatusCode.OK, cgyyService.submitReservation(username, request))
      } catch (e: CgyyException) {
        call.respondCgyyError(e)
      }
    }

    route("/orders") {
      get("/lock-code") {
        val username = call.jwtUsername!!
        try {
          call.respond(HttpStatusCode.OK, cgyyService.getLockCode(username))
        } catch (e: CgyyException) {
          call.respondCgyyError(e)
        }
      }

      get {
        val username = call.jwtUsername!!
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
        val size = call.request.queryParameters["size"]?.toIntOrNull() ?: 20
        try {
          call.respond(HttpStatusCode.OK, cgyyService.getOrders(username, page, size))
        } catch (e: CgyyException) {
          call.respondCgyyError(e)
        }
      }

      get("/{orderId}") {
        val username = call.jwtUsername!!
        val orderId =
            call.parameters["orderId"]?.toIntOrNull()
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(ErrorDetails("invalid_request", "orderId is required")),
                )
        try {
          call.respond(HttpStatusCode.OK, cgyyService.getOrderDetail(username, orderId))
        } catch (e: CgyyException) {
          call.respondCgyyError(e)
        }
      }

      post("/{orderId}/cancel") {
        val username = call.jwtUsername!!
        val orderId =
            call.parameters["orderId"]?.toIntOrNull()
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(ErrorDetails("invalid_request", "orderId is required")),
                )
        try {
          call.respond(HttpStatusCode.OK, cgyyService.cancelOrder(username, orderId))
        } catch (e: CgyyException) {
          call.respondCgyyError(e)
        }
      }
    }
  }
}

private suspend fun ApplicationCall.respondCgyyError(e: CgyyException) {
  val status =
      when (e.code) {
        "invalid_request", "reservation_invalid", "reservation_token_missing" ->
            HttpStatusCode.BadRequest
        "unauthenticated" -> HttpStatusCode.Unauthorized
        "captcha_error" -> HttpStatusCode.BadGateway
        else -> HttpStatusCode.BadGateway
      }
  respond(status, ErrorResponse(ErrorDetails(e.code, e.message ?: "研讨室请求失败")))
}
