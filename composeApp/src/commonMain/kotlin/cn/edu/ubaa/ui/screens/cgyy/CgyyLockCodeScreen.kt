package cn.edu.ubaa.ui.screens.cgyy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CgyyLockCodeScreen(viewModel: CgyyViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  val pullRefreshState =
      rememberPullRefreshState(
          refreshing = uiState.isLockCodeLoading,
          onRefresh = viewModel::loadLockCode,
      )

  Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      CgyySectionCard(title = "查看密码") {
        Button(
            onClick = viewModel::loadLockCode,
            enabled = !uiState.isLockCodeLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
          Text(if (uiState.isLockCodeLoading) "加载中..." else "获取门锁密码")
        }
      }

      uiState.lockCodeError?.let { CgyyMessageBanner(it) }

      if (uiState.lockCode != null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Text(
              text = uiState.lockCode?.rawData.toRawDisplayText(),
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              fontFamily = FontFamily.Monospace,
              style = MaterialTheme.typography.bodyMedium,
          )
        }
      }

      Text(
          text = "若能看到密码请截图联系开发者。",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
      )
    }
    PullRefreshIndicator(
        refreshing = uiState.isLockCodeLoading,
        state = pullRefreshState,
        modifier = Modifier.align(Alignment.TopCenter),
    )
  }
}
