package com.chanbro.salim

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.SalimBottomBar
import com.chanbro.salim.ui.common.SalimTab
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.dday.DDayInputScreen
import com.chanbro.salim.ui.dday.DDayScreen
import com.chanbro.salim.ui.expense.ExpenseInputScreen
import com.chanbro.salim.ui.expense.ExpenseScreen
import com.chanbro.salim.ui.home.HomeScreen
import com.chanbro.salim.ui.schedule.ScheduleInputScreen
import com.chanbro.salim.ui.schedule.ScheduleScreen
import com.chanbro.salim.ui.schedule.todayUtc

private const val ROUTE_EXPENSE_INPUT = "expense_input"
private const val ROUTE_DDAY_INPUT = "dday_input"
private const val ROUTE_DDAY_EDIT = "dday_edit/{ddayId}"
private const val ROUTE_SCHEDULE_INPUT = "schedule_input/{dateMillis}"
private const val ROUTE_SCHEDULE_EDIT = "schedule_edit/{scheduleId}"

private fun scheduleInputRoute(dateMillis: Long): String = "schedule_input/$dateMillis"
private fun scheduleEditRoute(scheduleId: String): String =
    "schedule_edit/${Uri.encode(scheduleId)}"

private fun ddayEditRoute(ddayId: String): String = "dday_edit/${Uri.encode(ddayId)}"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SalimTheme {
                SalimApp()
            }
        }
    }
}

@Composable
private fun SalimApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // 일정 FAB가 캘린더에서 선택 중인 날짜를 기본값으로 넘기기 위해 끌어올린 상태.
    var selectedScheduleDate by rememberSaveable { mutableLongStateOf(todayUtc()) }

    val tabRoutes = SalimTab.entries.map { it.route }.toSet()
    val onTabRoute = currentRoute in tabRoutes
    val selectedTab = SalimTab.entries.firstOrNull { it.route == currentRoute } ?: SalimTab.Home

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SalimTokens.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = onTabRoute,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                SalimBottomBar(
                    selected = selectedTab,
                    onSelect = { tab -> navController.navigateToTab(tab) },
                )
            }
        },
        floatingActionButton = {
            // FAB 노출 규칙(main-shell.md): 가계부/일정/디데이
            when (currentRoute) {
                SalimTab.Expense.route -> {
                    FloatingActionButton(
                        onClick = { navController.navigate(ROUTE_EXPENSE_INPUT) },
                        containerColor = SalimTokens.Accent,
                        contentColor = Color.White,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "지출 추가")
                    }
                }
                SalimTab.Schedule.route -> {
                    FloatingActionButton(
                        onClick = { navController.navigate(scheduleInputRoute(selectedScheduleDate)) },
                        containerColor = SalimTokens.Accent,
                        contentColor = Color.White,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "일정 등록")
                    }
                }
                SalimTab.DDay.route -> {
                    FloatingActionButton(
                        onClick = { navController.navigate(ROUTE_DDAY_INPUT) },
                        containerColor = SalimTokens.Accent,
                        contentColor = Color.White,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "디데이 추가")
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SalimTab.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(SalimTab.Home.route) { HomeScreen() }
            composable(SalimTab.Expense.route) {
                ExpenseScreen(onItemClick = { navController.navigate(ROUTE_EXPENSE_INPUT) })
            }
            composable(SalimTab.Schedule.route) {
                ScheduleScreen(
                    onItemClick = { row -> navController.navigate(scheduleEditRoute(row.id)) },
                    onSelectedDateChange = { selectedScheduleDate = it },
                )
            }
            composable(SalimTab.DDay.route) {
                DDayScreen(
                    onItemClick = { row ->
                        // 자동 반영 항목(생일/기념일)은 설정 > 프로필에서만 수정 (PRD 6.)
                        // TODO: 자동 항목 탭 시 안내/프로필 이동 흐름 확정 필요 (dday.md 6-1)
                        if (!row.isAuto) navController.navigate(ddayEditRoute(row.id))
                    },
                )
            }
            composable(SalimTab.Settings.route) { PlaceholderScreen(SalimTab.Settings.label) }
            composable(ROUTE_DDAY_INPUT) {
                DDayInputScreen(
                    onClose = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                ROUTE_DDAY_EDIT,
                arguments = listOf(navArgument("ddayId") { type = NavType.StringType }),
            ) { entry ->
                DDayInputScreen(
                    onClose = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                    ddayId = entry.arguments?.getString("ddayId"),
                )
            }
            composable(
                ROUTE_SCHEDULE_INPUT,
                arguments = listOf(navArgument("dateMillis") { type = NavType.LongType }),
            ) { entry ->
                ScheduleInputScreen(
                    onClose = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                    defaultDateMillis = entry.arguments?.getLong("dateMillis") ?: todayUtc(),
                )
            }
            composable(
                ROUTE_SCHEDULE_EDIT,
                arguments = listOf(navArgument("scheduleId") { type = NavType.StringType }),
            ) { entry ->
                ScheduleInputScreen(
                    onClose = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                    scheduleId = entry.arguments?.getString("scheduleId"),
                )
            }
            composable(ROUTE_EXPENSE_INPUT) {
                ExpenseInputScreen(
                    onClose = { navController.popBackStack() },
                    onSave = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun androidx.navigation.NavHostController.navigateToTab(tab: SalimTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$label 화면 준비 중", style = SalimType.bodyLg, color = SalimTokens.TextMuted)
    }
}
