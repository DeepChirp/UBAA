package cn.edu.ubaa.api

import cn.edu.ubaa.model.dto.CgyyDayInfoResponse
import cn.edu.ubaa.model.dto.CgyyLockCodeResponse
import cn.edu.ubaa.model.dto.CgyyOrderDto
import cn.edu.ubaa.model.dto.CgyyOrdersPageResponse
import cn.edu.ubaa.model.dto.CgyyPurposeTypeDto
import cn.edu.ubaa.model.dto.CgyyReservationSubmitRequest
import cn.edu.ubaa.model.dto.CgyyReservationSubmitResponse
import cn.edu.ubaa.model.dto.CgyyVenueSiteDto
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

open class CgyyApi(private val apiClient: ApiClient = ApiClientProvider.shared) {
  open suspend fun getVenueSites(): Result<List<CgyyVenueSiteDto>> {
    return safeApiCall { apiClient.getClient().get("api/v1/cgyy/sites") }
  }

  open suspend fun getPurposeTypes(): Result<List<CgyyPurposeTypeDto>> {
    return safeApiCall { apiClient.getClient().get("api/v1/cgyy/purpose-types") }
  }

  open suspend fun getDayInfo(venueSiteId: Int, date: String): Result<CgyyDayInfoResponse> {
    return safeApiCall {
      apiClient.getClient().get("api/v1/cgyy/day-info") {
        parameter("venueSiteId", venueSiteId)
        parameter("date", date)
      }
    }
  }

  open suspend fun submitReservation(
      request: CgyyReservationSubmitRequest
  ): Result<CgyyReservationSubmitResponse> {
    return safeApiCall {
      apiClient.getClient().post("api/v1/cgyy/reservations") {
        contentType(ContentType.Application.Json)
        setBody(request)
      }
    }
  }

  open suspend fun getMyOrders(page: Int = 0, size: Int = 20): Result<CgyyOrdersPageResponse> {
    return safeApiCall {
      apiClient.getClient().get("api/v1/cgyy/orders") {
        parameter("page", page)
        parameter("size", size)
      }
    }
  }

  open suspend fun getOrderDetail(orderId: Int): Result<CgyyOrderDto> {
    return safeApiCall { apiClient.getClient().get("api/v1/cgyy/orders/$orderId") }
  }

  open suspend fun cancelOrder(orderId: Int): Result<CgyyReservationSubmitResponse> {
    return safeApiCall { apiClient.getClient().post("api/v1/cgyy/orders/$orderId/cancel") }
  }

  open suspend fun getLockCode(): Result<CgyyLockCodeResponse> {
    return safeApiCall { apiClient.getClient().get("api/v1/cgyy/orders/lock-code") }
  }
}
