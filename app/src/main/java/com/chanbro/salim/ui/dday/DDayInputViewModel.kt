package com.chanbro.salim.ui.dday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.DDay
import com.chanbro.salim.domain.model.DDaySource
import com.chanbro.salim.domain.usecase.DeleteDDayUseCase
import com.chanbro.salim.domain.usecase.GetDDayUseCase
import com.chanbro.salim.domain.usecase.SaveDDayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** 수정 화면 프리필 값. 추가 모드에서는 null. */
data class DDayInitial(
    val title: String,
    val dateMillis: Long,
    val repeatYearly: Boolean,
)

@HiltViewModel
class DDayInputViewModel @Inject constructor(
    private val getDDay: GetDDayUseCase,
    private val saveDDay: SaveDDayUseCase,
    private val deleteDDay: DeleteDDayUseCase,
) : ViewModel() {

    private val _initial = MutableStateFlow<DDayInitial?>(null)
    val initial: StateFlow<DDayInitial?> = _initial.asStateFlow()

    /** 프리필 로딩 전에는 입력 화면을 그리지 않는다(빈 값이 잠깐 보이는 것 방지). */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var editingId: String? = null
    private var createdAtMillis: Long = 0L

    /** 수정 모드 진입 시 1회 호출. ddayId가 null이면 추가 모드. */
    fun load(ddayId: String?) {
        if (ddayId == null || editingId == ddayId) return
        editingId = ddayId
        _loading.value = true
        viewModelScope.launch {
            getDDay(ddayId)?.let { dDay ->
                createdAtMillis = dDay.createdAtMillis
                _initial.value = DDayInitial(dDay.title, dDay.dateMillis, dDay.repeatYearly)
            }
            _loading.value = false
        }
    }

    fun save(title: String, dateMillis: Long, repeatYearly: Boolean, onDone: () -> Unit) {
        val id = editingId
        val dDay = DDay(
            id = id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            dateMillis = dateMillis,
            repeatYearly = repeatYearly,
            // 자동 항목은 이 화면에 진입하지 않으므로 항상 직접 추가 (PRD 6.)
            source = DDaySource.MANUAL,
            createdAtMillis = if (id != null) createdAtMillis else System.currentTimeMillis(),
        )
        viewModelScope.launch {
            saveDDay(dDay)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = editingId ?: return
        viewModelScope.launch {
            deleteDDay(id)
            onDone()
        }
    }
}
