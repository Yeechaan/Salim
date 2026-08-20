package com.chanbro.salim.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chanbro.salim.domain.model.Schedule
import com.chanbro.salim.domain.model.ScheduleType
import com.chanbro.salim.domain.usecase.DeleteScheduleUseCase
import com.chanbro.salim.domain.usecase.GetScheduleUseCase
import com.chanbro.salim.domain.usecase.SaveScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** 수정 화면 프리필 값. 등록 모드에서는 null. */
data class ScheduleInitial(
    val title: String,
    val dateMillis: Long,
    val minuteOfDay: Int?,
    val type: ScheduleType,
)

@HiltViewModel
class ScheduleInputViewModel @Inject constructor(
    private val getSchedule: GetScheduleUseCase,
    private val saveSchedule: SaveScheduleUseCase,
    private val deleteSchedule: DeleteScheduleUseCase,
) : ViewModel() {

    private val _initial = MutableStateFlow<ScheduleInitial?>(null)
    val initial: StateFlow<ScheduleInitial?> = _initial.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var editingId: String? = null
    private var createdAtMillis: Long = 0L

    /** 수정 모드 진입 시 1회 호출. scheduleId가 null이면 등록 모드. */
    fun load(scheduleId: String?) {
        if (scheduleId == null || editingId == scheduleId) return
        editingId = scheduleId
        _loading.value = true
        viewModelScope.launch {
            getSchedule(scheduleId)?.let { schedule ->
                createdAtMillis = schedule.createdAtMillis
                _initial.value = ScheduleInitial(
                    title = schedule.title,
                    dateMillis = schedule.dateMillis,
                    minuteOfDay = schedule.minuteOfDay,
                    type = schedule.type,
                )
            }
            _loading.value = false
        }
    }

    fun save(
        title: String,
        dateMillis: Long,
        minuteOfDay: Int?,
        type: ScheduleType,
        onDone: () -> Unit,
    ) {
        val id = editingId
        val schedule = Schedule(
            id = id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            dateMillis = dateMillis,
            minuteOfDay = minuteOfDay,
            type = type,
            createdAtMillis = if (id != null) createdAtMillis else System.currentTimeMillis(),
        )
        viewModelScope.launch {
            saveSchedule(schedule)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = editingId ?: return
        viewModelScope.launch {
            deleteSchedule(id)
            onDone()
        }
    }
}
