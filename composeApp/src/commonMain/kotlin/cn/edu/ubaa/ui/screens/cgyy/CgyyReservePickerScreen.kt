package cn.edu.ubaa.ui.screens.cgyy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.ubaa.model.dto.CgyyReservationSelectionDto
import cn.edu.ubaa.model.dto.CgyySpaceAvailabilityDto
import cn.edu.ubaa.model.dto.CgyyTimeSlotDto
import cn.edu.ubaa.model.dto.CgyyVenueSiteDto

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CgyyReservePickerScreen(
    viewModel: CgyyViewModel,
    onNext: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()
  var showDateDialog by remember { mutableStateOf(false) }

  if (uiState.isInitialLoading && uiState.sites.isEmpty()) {
    CgyyLoadingState("正在加载场地与可预约信息...")
    return
  }

  if (uiState.initialError != null && uiState.sites.isEmpty()) {
    CgyyErrorState(uiState.initialError, onRetry = viewModel::loadInitialData)
    return
  }

  val campusOptions =
      listOf(CgyyViewModel.ALL_CAMPUSES) +
          uiState.sites.map { it.campusName }.distinct().sortedBy { campusSortKey(it) }
  val selectedCampus = uiState.selectedCampus.ifBlank { CgyyViewModel.ALL_CAMPUSES }
  val campusSites =
      if (selectedCampus == CgyyViewModel.ALL_CAMPUSES) {
        uiState.sites
      } else {
        uiState.sites.filter { it.campusName == selectedCampus }
      }
  val visibleSites =
      campusSites.filter { site ->
        val query = uiState.reserveSearchQuery.trim()
        query.isBlank() ||
            site.venueName.contains(query, ignoreCase = true) ||
            site.siteName.contains(query, ignoreCase = true)
      }
  val visibleSpaces =
      uiState.dayInfo?.spaces.orEmpty().filter { space ->
        val query = uiState.reserveSearchQuery.trim()
        query.isBlank() ||
            space.spaceName.contains(query, ignoreCase = true) ||
            selectedSiteLabel(uiState.sites, uiState.selectedSiteId).contains(query, ignoreCase = true)
      }
  val pullRefreshState =
      rememberPullRefreshState(
          refreshing = uiState.isInitialLoading || uiState.isDayInfoLoading,
          onRefresh = viewModel::refreshReserveData,
      )

  if (showDateDialog) {
    CgyyDateDialog(
        dates = uiState.dayInfo?.availableDates.orEmpty(),
        selectedDate = uiState.selectedDate,
        onDismiss = { showDateDialog = false },
        onSelect = {
          viewModel.selectDate(it)
          showDateDialog = false
        },
    )
  }

  Scaffold(
      bottomBar = {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Button(
              onClick = onNext,
              enabled = viewModel.canAdvanceToReservationForm(),
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp),
          ) {
            Text("下一步")
          }
        }
      }
  ) { innerPadding ->
    Box(
        modifier =
            Modifier.fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .pullRefresh(pullRefreshState)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          campusOptions.forEach { campus ->
            FilterChip(
                selected = selectedCampus == campus,
                onClick = { viewModel.setReserveCampus(campus) },
                label = { Text(campusDisplayName(campus)) },
            )
          }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedCard(
              onClick = { showDateDialog = true },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp),
          ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(uiState.selectedDate.ifBlank { "选择日期" })
              androidx.compose.material3.Icon(
                  imageVector = Icons.Default.DateRange,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
          OutlinedTextField(
              value = uiState.reserveSearchQuery,
              onValueChange = viewModel::updateReserveSearchQuery,
              modifier = Modifier.weight(1.5f),
              singleLine = true,
              placeholder = { Text("搜索楼栋/教室") },
              leadingIcon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
              },
          )
        }

        if (visibleSites.isNotEmpty()) {
          LazyRow(
              modifier = Modifier.fillMaxWidth(),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(visibleSites) { site ->
              FilterChip(
                  selected = uiState.selectedSiteId == site.id,
                  onClick = { viewModel.selectSite(site.id) },
                  label = { Text(siteSelectionLabel(site)) },
              )
            }
          }
        }

        when {
          uiState.isDayInfoLoading ->
              CgyyLoadingState("正在刷新可预约时段...", modifier = Modifier.fillMaxSize())
          uiState.dayInfoError != null ->
              CgyyErrorState(uiState.dayInfoError, onRetry = viewModel::refreshReserveData)
          uiState.selectedSiteId == null ->
              CgyyEmptyState("请选择楼栋/层", "先从上方横向列表中选择一个研讨室位置。")
          visibleSpaces.isEmpty() ->
              CgyyEmptyState("暂无可预约教室", "当前楼栋或搜索条件下没有匹配教室。")
          else ->
              CgyyReservationTable(
                  spaces = visibleSpaces,
                  timeSlots = uiState.dayInfo?.timeSlots.orEmpty(),
                  selectedSelections = uiState.selections,
                  onSlotToggle = viewModel::toggleSlot,
              )
        }
      }
      PullRefreshIndicator(
          refreshing = uiState.isInitialLoading || uiState.isDayInfoLoading,
          state = pullRefreshState,
          modifier = Modifier.align(Alignment.TopCenter),
      )
    }
  }
}

@Composable
private fun CgyyDateDialog(
    dates: List<String>,
    selectedDate: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("选择日期") },
      text = {
        if (dates.isEmpty()) {
          Text("当前楼栋暂无可预约日期")
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dates.forEach { date ->
              FilterChip(
                  selected = selectedDate == date,
                  onClick = { onSelect(date) },
                  label = { Text(date) },
              )
            }
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
  )
}

@Composable
private fun CgyyReservationTable(
    spaces: List<CgyySpaceAvailabilityDto>,
    timeSlots: List<CgyyTimeSlotDto>,
    selectedSelections: List<CgyyReservationSelectionDto>,
    onSlotToggle: (Int, Int, Int?) -> Unit,
) {
  val horizontalScroll = rememberScrollState()
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    item {
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .horizontalScroll(horizontalScroll)
                  .background(MaterialTheme.colorScheme.surface)
                  .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        CgyyTableCell("教室", 2.2f, true)
        timeSlots.forEach { slot ->
          CgyyTableCell(slotHeaderLabel(slot), 1.4f, true)
        }
      }
    }
    items(spaces) { space ->
      Row(
          modifier =
              Modifier.fillMaxWidth()
                  .height(44.dp)
                  .horizontalScroll(horizontalScroll)
                  .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Box(
            modifier =
                Modifier.weight(2.2f)
                    .fillMaxHeight()
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            contentAlignment = Alignment.Center,
        ) {
          Text(
              text = space.spaceName,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center,
              fontSize = 13.sp,
          )
        }
        timeSlots.forEach { slot ->
          val slotState = space.slots.firstOrNull { it.timeId == slot.id }
          val selected =
              selectedSelections.any { it.spaceId == space.spaceId && it.timeId == slot.id }
          val isReservable = slotState?.isReservable == true
          val background =
              when {
                selected -> MaterialTheme.colorScheme.primary
                isReservable -> Color(0xFF98FB98)
                else -> Color.Transparent
              }
          Box(
              modifier =
                  Modifier.weight(1.4f)
                      .fillMaxHeight()
                      .background(background)
                      .border(0.3.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                      .padding(2.dp),
              contentAlignment = Alignment.Center,
          ) {
            TextButton(
                onClick = {
                  if (slotState != null && (isReservable || selected)) {
                    onSlotToggle(space.spaceId, slot.id, space.venueSpaceGroupId)
                  }
                },
                enabled = slotState != null && (isReservable || selected),
                contentPadding = PaddingValues(0.dp),
            ) {
              Text(
                  text =
                      when {
                        selected -> "已选"
                        isReservable -> ""
                        else -> ""
                      },
                  color =
                      when {
                        selected -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                      },
                  fontSize = 10.sp,
              )
            }
          }
        }
      }
    }
    item { Spacer(modifier = Modifier.height(16.dp)) }
  }
}

@Composable
private fun RowScope.CgyyTableCell(text: String, weight: Float, header: Boolean) {
  Box(
      modifier =
          Modifier.weight(weight)
              .height(40.dp)
              .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
      contentAlignment = Alignment.Center,
  ) {
    Text(
        text = text,
        style =
            if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        fontSize = if (header) 11.sp else 10.sp,
        lineHeight = if (header) 12.sp else 11.sp,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
  }
}

private fun slotHeaderLabel(slot: CgyyTimeSlotDto): String = "${slot.beginTime}\n-${slot.endTime}"

private fun selectedSiteLabel(sites: List<CgyyVenueSiteDto>, selectedSiteId: Int?): String =
    sites.firstOrNull { it.id == selectedSiteId }?.let(::siteSelectionLabel).orEmpty()

private fun siteSelectionLabel(site: CgyyVenueSiteDto): String =
    buildString {
      append(shortVenueName(site.venueName))
      if (site.siteName.isNotBlank()) {
        append(" / ")
        append(site.siteName)
      }
    }

private fun shortVenueName(name: String): String =
    name.removeSuffix("研讨室").ifBlank { name }

private fun campusDisplayName(name: String): String =
    when {
      name.contains("学院路") -> "学院路"
      name.contains("沙河") -> "沙河"
      else -> name
    }

private fun campusSortKey(name: String): Int =
    when {
      name.contains("学院路") -> 0
      name.contains("沙河") -> 1
      else -> 99
    }
