package cn.edu.ubaa.ui

import cn.edu.ubaa.model.dto.CgyyOrderDto
import cn.edu.ubaa.ui.screens.cgyy.shouldShowCancelAction
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CgyyScreenLogicTest {
  @Test
  fun `cancel action hidden for cancelled order`() {
    assertFalse(shouldShowCancelAction(CgyyOrderDto(id = 1, orderStatus = 2)))
  }

  @Test
  fun `cancel action hidden for rejected order`() {
    assertFalse(shouldShowCancelAction(CgyyOrderDto(id = 1, orderStatus = 1, checkStatus = -2)))
  }

  @Test
  fun `cancel action kept for pending order`() {
    assertTrue(shouldShowCancelAction(CgyyOrderDto(id = 1, orderStatus = 1, checkStatus = 2)))
  }
}
