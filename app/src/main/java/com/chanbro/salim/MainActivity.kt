package com.chanbro.salim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chanbro.salim.core.ui.theme.SalimTheme
import com.chanbro.salim.core.ui.theme.SalimTokens
import com.chanbro.salim.ui.common.SalimBottomBar
import com.chanbro.salim.ui.common.SalimTab
import com.chanbro.salim.ui.common.SalimType
import com.chanbro.salim.ui.expense.ExpenseScreen
import com.chanbro.salim.ui.home.HomeScreen

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
    var tab by rememberSaveable { mutableStateOf(SalimTab.Home) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SalimTokens.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { SalimBottomBar(selected = tab, onSelect = { tab = it }) },
        floatingActionButton = {
            // FAB 노출 규칙(main-shell.md): 가계부/일정/디데이. 지금은 가계부만 실제 화면.
            if (tab == SalimTab.Expense) {
                FloatingActionButton(
                    onClick = { /* TODO: 지출 입력 화면 연결 (4-2) */ },
                    containerColor = SalimTokens.Accent,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "지출 추가")
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (tab) {
                SalimTab.Home -> HomeScreen()
                SalimTab.Expense -> ExpenseScreen()
                else -> PlaceholderScreen(tab.label)
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$label 화면 준비 중", style = SalimType.bodyLg, color = SalimTokens.TextMuted)
    }
}
