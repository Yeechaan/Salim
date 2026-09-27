package com.chanbro.salim.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.chanbro.salim.MainActivity
import com.chanbro.salim.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

// ---------------------------------------------------------------------------
// 위젯 공통 (widget.md "공통")
// ---------------------------------------------------------------------------

private const val LOAD_TIMEOUT_MILLIS = 10_000L

/**
 * 위젯 데이터를 읽는다. Firestore 오프라인 캐시에서 읽으므로 보통 즉시 끝난다.
 * 시간 안에 못 읽거나 실패하면 null — 호출부는 "없음"으로 오인하지 않도록 "불러오기 실패"를 그린다.
 */
internal suspend fun <T : Any> loadForWidget(tag: String, load: suspend () -> T): T? = try {
    withTimeoutOrNull(LOAD_TIMEOUT_MILLIS) { load() }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.w(tag, "위젯 데이터 로드 실패", e)
    null
}

/** 앱을 열고 [deepLink] 화면으로 바로 들어가는 인텐트 (MainActivity의 딥링크 라우트). */
internal fun openAppIntent(context: Context, deepLink: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), context, MainActivity::class.java)

// 크기별 레이아웃 (widget.md "공통 — 가로형 2×1").
// Responsive는 놓인 크기에 들어가는 가장 큰 후보로 그린다. 폰에서 한 칸 높이는 약 90~100dp라
// 세로 한 칸이면 COMPACT, 두 칸 이상이면 REGULAR가 고른다.
private val COMPACT = DpSize(110.dp, 40.dp)
private val REGULAR = DpSize(110.dp, 130.dp)
internal val WIDGET_SIZE_MODE = SizeMode.Responsive(setOf(COMPACT, REGULAR))

/** 지금 그리는 크기가 가로형(2×1)인지. */
@Composable
internal fun isCompact(): Boolean = LocalSize.current.height < REGULAR.height

/** 흰(또는 강조) 카드 한 장. 위젯 어디를 눌러도 [onClickIntent]로 간다. 본문은 카드 아래쪽에 붙인다. */
@Composable
internal fun WidgetCard(
    @DrawableRes background: Int,
    onClickIntent: Intent,
    header: @Composable () -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(background))
            .padding(16.dp)
            .clickable(actionStartActivity(onClickIntent)),
    ) {
        header()
        Spacer(GlanceModifier.defaultWeight())
        body()
    }
}

/** 가로형(2×1) 카드. 내용을 세로 가운데에 둔다. */
@Composable
internal fun WidgetCompactCard(
    @DrawableRes background: Int,
    onClickIntent: Intent,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(background))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable(actionStartActivity(onClickIntent)),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** 라벨 아이콘. 가로형은 제목 옆에 따로 둔다. */
@Composable
internal fun WidgetIcon(@DrawableRes icon: Int, color: Color) {
    Image(
        provider = ImageProvider(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(ColorProvider(color)),
        modifier = GlanceModifier.size(16.dp),
    )
}

/** 상단 라벨 — 아이콘 + 위젯 이름. */
@Composable
internal fun WidgetLabel(@DrawableRes icon: Int, text: String, iconColor: Color, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WidgetIcon(icon, iconColor)
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = text,
            style = TextStyle(color = ColorProvider(textColor), fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
    }
}

/** 빈 상태 / 불러오기 실패 문구. */
@Composable
internal fun WidgetMessage(text: String, color: Color) {
    Text(text = text, style = TextStyle(color = ColorProvider(color), fontSize = 13.sp))
}

/** 상태 문구가 없는 위젯용 — 불러오기 실패. */
internal fun Context.widgetUnavailableText(): String = getString(R.string.widget_unavailable)
