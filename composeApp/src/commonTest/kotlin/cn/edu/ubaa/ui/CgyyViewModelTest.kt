package cn.edu.ubaa.ui

import cn.edu.ubaa.api.CgyyApi
import cn.edu.ubaa.model.dto.CgyyDayInfoResponse
import cn.edu.ubaa.model.dto.CgyyLockCodeResponse
import cn.edu.ubaa.model.dto.CgyyOrderDto
import cn.edu.ubaa.model.dto.CgyyOrdersPageResponse
import cn.edu.ubaa.model.dto.CgyyPurposeTypeDto
import cn.edu.ubaa.model.dto.CgyyReservationSelectionDto
import cn.edu.ubaa.model.dto.CgyyReservationSubmitRequest
import cn.edu.ubaa.model.dto.CgyyReservationSubmitResponse
import cn.edu.ubaa.model.dto.CgyySlotStatusDto
import cn.edu.ubaa.model.dto.CgyySpaceAvailabilityDto
import cn.edu.ubaa.model.dto.CgyyTimeSlotDto
import cn.edu.ubaa.model.dto.CgyyVenueSiteDto
import cn.edu.ubaa.ui.screens.cgyy.CgyyViewModel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalCoroutinesApi::class)
class CgyyViewModelTest {
  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init loads sites and day info`() = runTest {
    setMainDispatcher(testScheduler)
    val api = FakeCgyyApi()
    val viewModel = CgyyViewModel(api, currentDateProvider = { "2026-03-29" })

    advanceUntilIdle()

    assertEquals(1, viewModel.uiState.value.sites.size)
    assertEquals(4, viewModel.uiState.value.selectedSiteId)
    assertEquals("2026-03-29", viewModel.uiState.value.selectedDate)
    assertEquals(1, api.dayInfoCalls)
    assertEquals(0, api.ordersCalls)
  }

  @Test
  fun `toggle slot keeps only one selection and builds summary`() = runTest {
    setMainDispatcher(testScheduler)
    val viewModel = CgyyViewModel(FakeCgyyApi(), currentDateProvider = { "2026-03-29" })

    advanceUntilIdle()
    viewModel.toggleSlot(6, 242, null)
    viewModel.toggleSlot(7, 242, null)

    assertEquals(1, viewModel.uiState.value.selections.size)
    assertEquals(7, viewModel.uiState.value.selections.first().spaceId)
    assertNotNull(viewModel.uiState.value.reservationSummary)
    assertEquals("主205", viewModel.uiState.value.reservationSummary?.spaceName)
  }

  @Test
  fun `tapping same slot twice clears selection`() = runTest {
    setMainDispatcher(testScheduler)
    val viewModel = CgyyViewModel(FakeCgyyApi(), currentDateProvider = { "2026-03-29" })

    advanceUntilIdle()
    viewModel.toggleSlot(6, 242, null)
    viewModel.toggleSlot(6, 242, null)

    assertTrue(viewModel.uiState.value.selections.isEmpty())
    assertEquals(null, viewModel.uiState.value.reservationSummary)
  }

  @Test
  fun `submit reservation refreshes orders`() = runTest {
    setMainDispatcher(testScheduler)
    val api = FakeCgyyApi()
    val viewModel = CgyyViewModel(api, currentDateProvider = { "2026-03-29" })

    advanceUntilIdle()
    viewModel.updatePhone("18800000000")
    viewModel.updateTheme("讨论")
    viewModel.updateJoinerNum("3")
    viewModel.updateActivityContent("活动内容")
    viewModel.updateJoiners("张三等三人")
    viewModel.toggleSlot(6, 242, null)
    viewModel.submitReservation()
    advanceUntilIdle()

    assertEquals(1, api.submitCalls)
    assertEquals(1, api.ordersCalls)
    assertEquals("预约成功", viewModel.uiState.value.actionMessage)
    assertTrue(viewModel.uiState.value.selections.isEmpty())
    assertEquals("", viewModel.uiState.value.theme)
  }

  @Test
  fun `ensure orders loaded fetches orders lazily`() = runTest {
    setMainDispatcher(testScheduler)
    val api = FakeCgyyApi()
    val viewModel = CgyyViewModel(api, currentDateProvider = { "2026-03-29" })

    advanceUntilIdle()
    viewModel.ensureOrdersLoaded()
    advanceUntilIdle()

    assertEquals(1, api.ordersCalls)
    assertEquals(1, viewModel.uiState.value.orders.content.size)
  }

  @Test
  fun `load lock code stores raw value`() = runTest {
    setMainDispatcher(testScheduler)
    val viewModel = CgyyViewModel(FakeCgyyApi(), currentDateProvider = { "2026-03-29" })

    advanceUntilIdle()
    viewModel.loadLockCode()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.lockCode?.rawData.toString().contains("123456"))
  }

  private fun setMainDispatcher(testScheduler: TestCoroutineScheduler) {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
  }

  private class FakeCgyyApi : CgyyApi() {
    var dayInfoCalls = 0
    var submitCalls = 0
    var ordersCalls = 0

    override suspend fun getVenueSites(): Result<List<CgyyVenueSiteDto>> {
      return Result.success(
          listOf(CgyyVenueSiteDto(id = 4, siteName = "二层", venueName = "老主楼研讨室", campusName = "学院路校区"))
      )
    }

    override suspend fun getPurposeTypes(): Result<List<CgyyPurposeTypeDto>> {
      return Result.success(listOf(CgyyPurposeTypeDto(3, "学术研讨类")))
    }

    override suspend fun getDayInfo(venueSiteId: Int, date: String): Result<CgyyDayInfoResponse> {
      dayInfoCalls++
      return Result.success(
          CgyyDayInfoResponse(
              venueSiteId = venueSiteId,
              reservationDate = date,
              availableDates = listOf(date),
              timeSlots = listOf(CgyyTimeSlotDto(242, "14:00", "15:35", "14:00-15:35")),
              spaces =
                  listOf(
                      CgyySpaceAvailabilityDto(
                          spaceId = 6,
                          spaceName = "主204",
                          venueSiteId = venueSiteId,
                          slots =
                              listOf(
                                  CgyySlotStatusDto(
                                      timeId = 242,
                                      reservationStatus = 1,
                                      isReservable = true,
                                  )
                              ),
                      ),
                      CgyySpaceAvailabilityDto(
                          spaceId = 7,
                          spaceName = "主205",
                          venueSiteId = venueSiteId,
                          slots =
                              listOf(
                                  CgyySlotStatusDto(
                                      timeId = 242,
                                      reservationStatus = 1,
                                      isReservable = true,
                                  )
                              ),
                      ),
                  ),
          )
      )
    }

    override suspend fun submitReservation(
        request: CgyyReservationSubmitRequest
    ): Result<CgyyReservationSubmitResponse> {
      submitCalls++
      return Result.success(
          CgyyReservationSubmitResponse(
              success = true,
              message = "预约成功",
              order = CgyyOrderDto(id = 1, theme = request.theme),
          )
      )
    }

    override suspend fun getMyOrders(page: Int, size: Int): Result<CgyyOrdersPageResponse> {
      ordersCalls++
      return Result.success(
          CgyyOrdersPageResponse(
              content = listOf(CgyyOrderDto(id = 1, theme = "讨论")),
              totalElements = 1,
              totalPages = 1,
              size = size,
              number = page,
          )
      )
    }

    override suspend fun getLockCode(): Result<CgyyLockCodeResponse> {
      return Result.success(
          CgyyLockCodeResponse(buildJsonObject { put("password", "123456") })
      )
    }
  }
}
