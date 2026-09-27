package com.chanbro.salim

import android.app.Application
import com.chanbro.salim.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SalimApplication : Application() {

    @Inject lateinit var widgetUpdater: WidgetUpdater

    override fun onCreate() {
        super.onCreate()
        // 홈 화면 위젯이 놓여 있으면 데이터 변화를 위젯에 실시간 반영 (PRD 10)
        widgetUpdater.start()
    }
}
