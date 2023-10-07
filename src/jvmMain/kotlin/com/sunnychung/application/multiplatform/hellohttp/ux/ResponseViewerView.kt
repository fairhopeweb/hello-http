package com.sunnychung.application.multiplatform.hellohttp.ux

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalColor
import com.sunnychung.application.multiplatform.hellohttp.ux.local.LocalFont
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KFixedTimeUnit
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant

@Composable
fun ResponseViewerView(response: UserResponse) {
    val colors = LocalColor.current

    var selectedTab by remember { mutableStateOf(ResponseTab.values().first()) }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            StatusLabel(response = response)
            DurationLabel(response = response)
            ResponseSizeLabel(response = response)
        }

        TabsView(
            modifier = Modifier.fillMaxWidth().background(color = colors.backgroundLight),
            onSelectTab = { selectedTab = ResponseTab.values()[it] },
            contents = ResponseTab.values().map {
                { AppText(text = it.name, modifier = Modifier.padding(8.dp)) }
            }
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                ResponseTab.Body -> if (response.body != null) {
                    CodeEditorView(
                        isReadOnly = true,
                        text = response.body!!.decodeToString(),
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                    )
                } else {
                    ResponseEmptyView(type = "body", modifier = Modifier.fillMaxSize().padding(8.dp))
                }

                ResponseTab.Header -> if (response.headers != null) {
                    KeyValueTableView(keyValues = response.headers!!, modifier = Modifier.fillMaxSize().padding(8.dp))
                } else {
                    ResponseEmptyView(type = "header", modifier = Modifier.fillMaxSize().padding(8.dp))
                }

                ResponseTab.Raw ->
                    TransportTimelineView(exchange = response.rawExchange, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

private enum class ResponseTab {
    Body, Header, Raw
}

//@Composable
//@Preview
//fun ResponseViewerViewPreview() {
//    ResponseViewerView(
//        UserResponse(
////        KInstant.now() - KDuration.of(2346, KFixedTimeUnit.MilliSecond), // TODO
//            startAt = KInstant.now() + KDuration.of(-2346, KFixedTimeUnit.MilliSecond),
//            endAt = KInstant.now(),
//            isCommunicating = false,
//            statusCode = 200,
//            statusText = "OK",
//            responseSizeInBytes = 1234,
//            body = "{\"OK\"}",
//            headers = listOf("Content-Type" to "application/json", "Date" to KZonedInstant.nowAtLocalZoneOffset().format(KDateTimeFormat.ISO8601_DATETIME.pattern)),
//            rawExchange = RawExchange(listOf(RawExchange.Exchange(KInstant.now() + KDuration.of(-1, KFixedTimeUnit.MilliSecond), RawExchange.Direction.Outgoing, "Start"), RawExchange.Exchange(KInstant.now(), RawExchange.Direction.Incoming, "End")))
//        )
//    )
//}

//@Composable
//@Preview
//fun ResponseViewerViewPreview_EmptyBody() {
//    ResponseViewerView(
//        UserResponse(
////        KInstant.now() - KDuration.of(2346, KFixedTimeUnit.MilliSecond), // TODO
//            startAt = KInstant.now() + KDuration.of(-2346, KFixedTimeUnit.MilliSecond),
//            endAt = KInstant.now(),
//            isCommunicating = false,
//            statusCode = 200,
//            statusText = "OK",
//            responseSizeInBytes = 1234,
//            body = null,
//            headers = listOf("Content-Type" to "application/json"),
//            rawExchange = RawExchange(listOf())
//        )
//    )
//}

@Composable
fun DataLabel(
    modifier: Modifier = Modifier,
    text: String,
    backgroundColor: Color = LocalColor.current.backgroundLight,
    textColor: Color = LocalColor.current.text,
) {
    AppText(
        text = text,
        color = textColor,
        modifier = modifier.background(color = backgroundColor).padding(8.dp)
    )
}

@Composable
fun StatusLabel(modifier: Modifier = Modifier, response: UserResponse) {
    val colors = LocalColor.current
    val (text, backgroundColor) = if (response.isCommunicating) {
        Pair("Communicating", colors.pendingResponseBackground)
    } else when (response.statusCode) {
        null -> return
        in 100..399 -> Pair("${response.statusCode} ${response.statusText}", colors.successfulResponseBackground)
        else -> Pair("${response.statusCode} ${response.statusText}", colors.errorResponseBackground)
    }
    DataLabel(modifier = modifier, text = text, backgroundColor = backgroundColor, textColor = colors.bright)
}

@Composable
fun DurationLabel(modifier: Modifier = Modifier, response: UserResponse) {
    val startAt = response.startAt ?: return
    val endAt = response.endAt ?: return
    val duration = endAt - startAt
//    val text = if (duration >= KDuration.of(10, KFixedTimeUnit.Second)) // TODO
    val text = if (duration.toTimeUnitValue(KFixedTimeUnit.Second) >= 10) {
        "${duration.toMilliseconds() / 1000.0} s"
    } else {
        "${duration.toMilliseconds()} ms"
    }
    DataLabel(modifier = modifier, text = text)
}

@Composable
fun ResponseSizeLabel(modifier: Modifier = Modifier, response: UserResponse) {
    val size = response.responseSizeInBytes ?: return
    val text = if (size >= 1024L * 1024L) {
        "${"%.3f".format(size / 1024.0 / 1024.0)} MB"
    } else if (size >= 1024L) {
        "${"%.3f".format(size / 1024.0)} KB"
    } else {
        "${size} B"
    }
    DataLabel(modifier = modifier, text = text)
}

@Composable
fun ResponseEmptyView(modifier: Modifier = Modifier, type: String) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AppText(text = "No response $type available", fontSize = LocalFont.current.largeInfoSize, textAlign = TextAlign.Center)
    }
}
