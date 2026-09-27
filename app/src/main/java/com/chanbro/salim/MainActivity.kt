package com.chanbro.salim

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.SalimBottomBar
import com.chanbro.salim.ui.common.SalimTab
import com.chanbro.salim.ui.connect.CodeInputScreen
import com.chanbro.salim.ui.connect.ConnectDoneScreen
import com.chanbro.salim.ui.connect.ConnectScreen
import com.chanbro.salim.ui.connect.InviteCodeScreen
import com.chanbro.salim.ui.dday.DDayInputScreen
import com.chanbro.salim.ui.dday.DDayScreen
import com.chanbro.salim.ui.expense.ExpenseInputScreen
import com.chanbro.salim.ui.expense.ExpenseScreen
import com.chanbro.salim.ui.todo.TodoScreen
import com.chanbro.salim.ui.wedding.VendorDetailScreen
import com.chanbro.salim.ui.wedding.VendorInputScreen
import com.chanbro.salim.ui.wedding.WeddingExpenseInputScreen
import com.chanbro.salim.ui.wedding.WeddingScreen
import com.chanbro.salim.ui.auth.LoginScreen
import com.chanbro.salim.ui.home.HomeScreen
import com.chanbro.salim.ui.onboarding.OnboardingScreen
import com.chanbro.salim.ui.settings.CategoryEditScreen
import com.chanbro.salim.ui.settings.ProfileEditScreen
import com.chanbro.salim.ui.settings.SettingsScreen
import com.chanbro.salim.ui.schedule.ScheduleInputScreen
import com.chanbro.salim.ui.schedule.ScheduleScreen
import com.chanbro.salim.ui.schedule.todayUtc

private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_LOGIN = "login"
private const val ROUTE_EXPENSE_INPUT = "expense_input"
private const val ROUTE_EXPENSE_EDIT = "expense_edit/{expenseId}"
private const val ROUTE_DDAY_MANAGE = "dday_manage"
private const val ROUTE_DDAY_INPUT = "dday_input"

/**
 * 홈 화면 위젯 진입점 (PRD 10). 위젯이 MainActivity를 명시적으로 지정해 여므로
 * 매니페스트 intent-filter는 두지 않는다 — 외부 앱이 쓸 이유가 없는 진입점이다.
 */
internal const val DEEP_LINK_DDAY_MANAGE = "salim://dday"
internal const val DEEP_LINK_HOME = "salim://home"
private const val ROUTE_DDAY_EDIT = "dday_edit/{ddayId}"
private const val ROUTE_SCHEDULE_INPUT = "schedule_input/{dateMillis}"
private const val ROUTE_SCHEDULE_EDIT = "schedule_edit/{scheduleId}"
private const val ROUTE_PROFILE_EDIT = "profile_edit"
private const val ROUTE_CATEGORY_EDIT = "category_edit"

// 상대방 연결 (wireframe/connect.md 9-1~9-5)
private const val ROUTE_CONNECT = "connect"
private const val ROUTE_CONNECT_INVITE = "connect_invite"
private const val ROUTE_CONNECT_CODE = "connect_code?code={code}"
private const val ROUTE_CONNECT_DONE = "connect_done"

// 결혼 준비 (wedding.md 12-1~12-8)
private const val ROUTE_WEDDING = "wedding"
private const val ROUTE_WEDDING_EXPENSE = "wedding_expense?expenseId={expenseId}&vendorId={vendorId}"
private const val ROUTE_WEDDING_VENDOR = "wedding_vendor/{vendorId}"
private const val ROUTE_WEDDING_VENDOR_INPUT = "wedding_vendor_input?vendorId={vendorId}"

private fun weddingExpenseRoute(expenseId: String? = null, vendorId: String? = null): String =
    "wedding_expense?expenseId=${Uri.encode(expenseId.orEmpty())}&vendorId=${Uri.encode(vendorId.orEmpty())}"

private fun weddingVendorRoute(vendorId: String): String = "wedding_vendor/${Uri.encode(vendorId)}"

private fun weddingVendorInputRoute(vendorId: String? = null): String =
    "wedding_vendor_input?vendorId=${Uri.encode(vendorId.orEmpty())}"

/** 선택 인자. 빈 문자열로 넘긴 것은 없음으로 본다. */
private fun optionalStringArg(name: String) = navArgument(name) {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

private fun androidx.navigation.NavBackStackEntry.optionalArg(name: String): String? =
    arguments?.getString(name)?.takeIf { it.isNotEmpty() }

private fun connectCodeRoute(code: String? = null): String =
    if (code == null) "connect_code" else "connect_code?code=${Uri.encode(code)}"

private fun scheduleInputRoute(dateMillis: Long): String = "schedule_input/$dateMillis"

private fun expenseEditRoute(expenseId: String): String = "expense_edit/${Uri.encode(expenseId)}"
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
private fun SalimApp(appViewModel: AppViewModel = hiltViewModel()) {
    val appState by appViewModel.uiState.collectAsStateWithLifecycle()

    // 로그인 여부가 확정되기 전에는 NavHost를 만들지 않는다 — 시작 목적지가 한 번만 정해져
    // 온보딩/로그인/홈이 번갈아 깜빡이는 것을 막는다.
    if (appState is AppUiState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SalimTokens.Background),
        )
    } else {
        SalimNavGraph(
            appState = appState,
            onOnboardingFinished = appViewModel::onOnboardingFinished,
        )
    }
}

/** 진입 상태가 확정된 뒤의 본 화면 그래프. */
@Composable
private fun SalimNavGraph(
    appState: AppUiState,
    onOnboardingFinished: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // 일정 FAB가 캘린더에서 선택 중인 날짜를 기본값으로 넘기기 위해 끌어올린 상태.
    var selectedScheduleDate by rememberSaveable { mutableLongStateOf(todayUtc()) }
    // 할 일 FAB는 화면 안의 시트를 연다 — 요청만 끌어올리고 시트 상태는 화면이 가진다.
    var todoAddRequested by rememberSaveable { mutableStateOf(false) }

    val tabRoutes = SalimTab.entries.map { it.route }.toSet()
    val onTabRoute = currentRoute in tabRoutes
    val selectedTab = SalimTab.entries.firstOrNull { it.route == currentRoute } ?: SalimTab.Home

    // 시작 목적지는 최초 확정 값으로 한 번만 정하고, 이후 상태 변화(로그인 성공/로그아웃)는
    // 아래 LaunchedEffect가 이동으로 반영한다.
    val startDestination = remember { appState.route() }
    var lastHandledState by remember { mutableStateOf(appState) }
    LaunchedEffect(appState) {
        if (appState != lastHandledState) {
            lastHandledState = appState
            navController.navigate(appState.route()) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

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
            // FAB 노출 규칙(main-shell.md): 가계부/일정/할 일. 디데이 관리는 하위 화면이라 자기 FAB를 그린다.
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
                SalimTab.Todo.route -> {
                    FloatingActionButton(
                        onClick = { todoAddRequested = true },
                        containerColor = SalimTokens.Accent,
                        contentColor = Color.White,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.todo_add))
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ROUTE_ONBOARDING) { OnboardingScreen(onFinish = onOnboardingFinished) }
            composable(ROUTE_LOGIN) { LoginScreen() }
            composable(
                SalimTab.Home.route,
                // 예산 위젯 (widget.md 10-2)
                deepLinks = listOf(navDeepLink { uriPattern = DEEP_LINK_HOME }),
            ) {
                RequireMain(appState, navController) {
                    HomeScreen(
                        onConnectClick = { navController.navigate(ROUTE_CONNECT) },
                        onDDayClick = { navController.navigate(ROUTE_DDAY_MANAGE) },
                        onWeddingClick = { navController.navigate(ROUTE_WEDDING) },
                    )
                }
            }
            composable(SalimTab.Expense.route) {
                ExpenseScreen(onItemClick = { row -> navController.navigate(expenseEditRoute(row.id)) })
            }
            composable(SalimTab.Schedule.route) {
                ScheduleScreen(
                    onItemClick = { row -> navController.navigate(scheduleEditRoute(row.id)) },
                    onSelectedDateChange = { selectedScheduleDate = it },
                )
            }
            composable(SalimTab.Todo.route) {
                TodoScreen(
                    addRequested = todoAddRequested,
                    onAddHandled = { todoAddRequested = false },
                )
            }
            // 설정 > 디데이 관리 / 홈 디데이 카드 / 홈 화면 위젯 (dday.md 6-1)
            composable(
                ROUTE_DDAY_MANAGE,
                deepLinks = listOf(navDeepLink { uriPattern = DEEP_LINK_DDAY_MANAGE }),
            ) {
                RequireMain(appState, navController) {
                    DDayScreen(
                        onBack = { navController.popBackStack() },
                        onAddClick = { navController.navigate(ROUTE_DDAY_INPUT) },
                        onItemClick = { row ->
                            // 자동 반영 항목(생일/기념일)은 설정 > 프로필에서만 수정 (PRD 6.)
                            // TODO: 자동 항목 탭 시 안내/프로필 이동 흐름 확정 필요 (dday.md 6-1)
                            if (!row.isAuto) navController.navigate(ddayEditRoute(row.id))
                        },
                    )
                }
            }
            composable(SalimTab.Settings.route) {
                SettingsScreen(
                    onProfileClick = { navController.navigate(ROUTE_PROFILE_EDIT) },
                    onCategoryClick = { navController.navigate(ROUTE_CATEGORY_EDIT) },
                    onDDayClick = { navController.navigate(ROUTE_DDAY_MANAGE) },
                    onConnectClick = { navController.navigate(ROUTE_CONNECT) },
                )
            }
            composable(ROUTE_CONNECT) {
                ConnectScreen(
                    onClose = { navController.popBackStack() },
                    onCreateCode = { navController.navigate(ROUTE_CONNECT_INVITE) },
                    onEnterCode = { navController.navigate(connectCodeRoute()) },
                )
            }
            composable(ROUTE_CONNECT_INVITE) {
                InviteCodeScreen(
                    onClose = { navController.popBackStack() },
                    onConnected = { navController.navigateToConnectDone() },
                )
            }
            composable(
                ROUTE_CONNECT_CODE,
                arguments = listOf(
                    navArgument("code") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
                // 초대 QR / 공유 링크로 바로 들어오는 경로 (connect.md 9-3)
                deepLinks = listOf(navDeepLink { uriPattern = "salim://invite/{code}" }),
            ) { entry ->
                // 로그인 전이면 코드는 로그인 후 다시 입력하게 한다.
                RequireMain(appState, navController) {
                    CodeInputScreen(
                        onClose = { navController.popBackStack() },
                        onConnected = { navController.navigateToConnectDone() },
                        prefillCode = entry.arguments?.getString("code"),
                    )
                }
            }
            composable(ROUTE_CONNECT_DONE) {
                ConnectDoneScreen(
                    onHome = {
                        navController.navigate(SalimTab.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable(ROUTE_PROFILE_EDIT) {
                ProfileEditScreen(
                    onClose = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }
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
                    onDone = { navController.popBackStack() },
                    onEditCategories = { navController.navigate(ROUTE_CATEGORY_EDIT) },
                )
            }
            composable(
                ROUTE_EXPENSE_EDIT,
                arguments = listOf(navArgument("expenseId") { type = NavType.StringType }),
            ) { entry ->
                ExpenseInputScreen(
                    onClose = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                    onEditCategories = { navController.navigate(ROUTE_CATEGORY_EDIT) },
                    expenseId = entry.arguments?.getString("expenseId"),
                )
            }
            composable(ROUTE_CATEGORY_EDIT) {
                CategoryEditScreen(onClose = { navController.popBackStack() })
            }
            // 결혼 준비 — 홈 결혼 준비 카드에서만 들어온다 (PRD 12, 하단 탭 없음)
            composable(ROUTE_WEDDING) {
                WeddingScreen(
                    onBack = { navController.popBackStack() },
                    onAddExpense = { navController.navigate(weddingExpenseRoute()) },
                    onExpenseClick = { navController.navigate(weddingExpenseRoute(expenseId = it)) },
                    onAddVendor = { navController.navigate(weddingVendorInputRoute()) },
                    onVendorClick = { navController.navigate(weddingVendorRoute(it)) },
                )
            }
            composable(
                ROUTE_WEDDING_EXPENSE,
                arguments = listOf(optionalStringArg("expenseId"), optionalStringArg("vendorId")),
            ) { entry ->
                WeddingExpenseInputScreen(
                    onClose = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                    expenseId = entry.optionalArg("expenseId"),
                    vendorId = entry.optionalArg("vendorId"),
                )
            }
            composable(
                ROUTE_WEDDING_VENDOR,
                arguments = listOf(navArgument("vendorId") { type = NavType.StringType }),
            ) { entry ->
                val vendorId = entry.arguments?.getString("vendorId").orEmpty()
                VendorDetailScreen(
                    vendorId = vendorId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(weddingVendorInputRoute(vendorId)) },
                    onRecordExpense = { navController.navigate(weddingExpenseRoute(vendorId = vendorId)) },
                    onExpenseClick = { navController.navigate(weddingExpenseRoute(expenseId = it)) },
                )
            }
            composable(
                ROUTE_WEDDING_VENDOR_INPUT,
                arguments = listOf(optionalStringArg("vendorId")),
            ) { entry ->
                VendorInputScreen(
                    onClose = { navController.popBackStack() },
                    // 추가 → 업체 목록, 수정 → 업체 상세. 둘 다 한 단계 뒤다.
                    onSaved = { navController.popBackStack() },
                    // 삭제하면 상세를 건너뛰고 업체 목록으로 (wedding.md 12-8)
                    onDeleted = { navController.popBackStack(ROUTE_WEDDING, inclusive = false) },
                    vendorId = entry.optionalArg("vendorId"),
                )
            }
        }
    }
}

/**
 * 딥링크(초대 QR·위젯)로 들어오는 화면의 로그인 게이트. 딥링크는 로그인 전에도 들어올 수 있어서,
 * 그때는 [content] 대신 진입 상태가 정한 화면(온보딩/로그인)으로 돌려보낸다.
 */
@Composable
private fun RequireMain(
    appState: AppUiState,
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    if (appState == AppUiState.Main) {
        content()
    } else {
        LaunchedEffect(Unit) {
            navController.navigate(appState.route()) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}

/**
 * 탭 전환. 그래프 시작 목적지가 아니라 홈 탭을 기준으로 되감는다 —
 * 미로그인 진입 시 시작 목적지가 로그인 화면이라, 그래프 시작점을 쓰면
 * 로그인 후 탭을 옮길 때마다 백스택이 쌓인다.
 */
private fun androidx.navigation.NavHostController.navigateToTab(tab: SalimTab) {
    navigate(tab.route) {
        popUpTo(SalimTab.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * 연결 완료로 이동. 연결 흐름 화면들(9-1~9-3)은 뒤로가기에 남기지 않는다 —
 * 이미 연결된 상태에서 "초대 코드" 화면으로 되돌아가면 앞뒤가 맞지 않는다.
 */
private fun androidx.navigation.NavHostController.navigateToConnectDone() {
    navigate(ROUTE_CONNECT_DONE) {
        popUpTo(ROUTE_CONNECT) { inclusive = true }
        launchSingleTop = true
    }
}

/** 진입 상태에 대응하는 시작 라우트. Loading은 NavHost를 만들기 전에 걸러진다. */
private fun AppUiState.route(): String = when (this) {
    AppUiState.Onboarding -> ROUTE_ONBOARDING
    AppUiState.Login -> ROUTE_LOGIN
    AppUiState.Main, AppUiState.Loading -> SalimTab.Home.route
}
