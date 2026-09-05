package app.vela.ui.nav

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.KeyboardArrowUp
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vela.R
import app.vela.core.model.Lane
import app.vela.core.model.ManeuverType
import app.vela.ui.SheetPalette
import app.vela.ui.formatArrivalClock
import kotlinx.coroutines.launch
import app.vela.ui.formatDistance
import app.vela.ui.formatDuration
import app.vela.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import app.vela.ui.dpadFieldEscape
import app.vela.ui.dpadHighlight

/**
 * Top turn banner - DISABLED/HIDDEN
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManeuverBanner(
    text: String,
    distanceMeters: Double,
    type: ManeuverType = ManeuverType.STRAIGHT,
    roundabout: app.vela.core.model.RoundaboutGeometry? = null,
    ref: String? = null,
    laneHint: String? = null,
    lanes: List<Lane> = emptyList(),
    nextText: String? = null,
    nextType: ManeuverType? = null,
    nextRoundabout: app.vela.core.model.RoundaboutGeometry? = null,
    nextRef: String? = null,
    currentRef: String? = null,
    nextDistanceMeters: Double? = null,
    destName: String? = null,
    destAddress: String? = null,
    laneShowM: Double = LANE_SHOW_M,
    previewing: Boolean = false,
    offRoute: Boolean = false,
    onPreviewNext: () -> Unit = {},
    onPreviewPrev: () -> Unit = {},
    onExitPreview: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Top banner rendering completely disabled to remove the top instruction box
}

private const val LANE_SHOW_M = 800.0
private const val NAV_BAR_LIFT_COMMIT_DP = 56
private const val NAV_BAR_LIFT_MAX_DP = 120
private const val NAV_BAR_FLING_PX_S = 900f
private const val COMPOUND_M = 500.0

internal fun isCompoundNext(nextDistanceMeters: Double?): Boolean =
    nextDistanceMeters != null && nextDistanceMeters <= COMPOUND_M

private val EXIT_RE = Regex("""\bexit\s+(\w[\w-]*)""", RegexOption.IGNORE_CASE)
private val ROUTE_RE = Regex("""\b(?:(?:I|US|CA|SR|US-?Hwy|Hwy)[-\s]?\d+|(?-i:[A-Z]{2}[-\s]\d+))(?:\s?[NSEW]\b)?""", RegexOption.IGNORE_CASE)

internal data class Sign(val isExit: Boolean, val label: String)

internal fun roadSigns(text: String, explicitRef: String? = null): List<Sign> {
    val seen = HashSet<String>()
    val out = ArrayList<Sign>()
    EXIT_RE.find(text)?.let {
        val label = "Exit ${it.groupValues[1]}"
        if (seen.add(label.lowercase())) out.add(Sign(isExit = true, label = label))
    }
    explicitRef?.trim()?.replace(Regex("\\s+"), " ")?.uppercase()?.takeIf { it.isNotBlank() }?.let {
        if (seen.add(it.lowercase())) out.add(Sign(isExit = false, label = it))
    }
    ROUTE_RE.findAll(text).forEach { m ->
        val label = m.value.trim().replace(Regex("\\s+"), " ").uppercase()
        if (seen.add(label.lowercase())) out.add(Sign(isExit = false, label = label))
    }
    return out.take(3)
}

@Composable
internal fun SignChip(sign: Sign) {
    if (sign.isExit) {
        Surface(color = Color(0xFF1E7E34), shape = RoundedCornerShape(4.dp)) {
            Text(
                sign.label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    } else {
        RouteShield(
            sign.label,
            ink = MaterialTheme.colorScheme.onPrimaryContainer,
            dim = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun FitText(text: String, style: androidx.compose.ui.text.TextStyle, color: Color, modifier: Modifier = Modifier) {
    val scaleState = remember(text) { androidx.compose.runtime.mutableStateOf(1f) }
    val scale = scaleState.value
    Text(
        text,
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        fontSize = style.fontSize * scale,
        onTextLayout = { if (it.hasVisualOverflow && scaleState.value > 0.55f) scaleState.value *= 0.92f },
        modifier = modifier,
    )
}

@Composable
private fun LaneGuide(hint: String, type: ManeuverType) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f),
            shape = RoundedCornerShape(6.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(laneArrowCount(hint)) {
                    Icon(maneuverIcon(type), contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
        Text(hint, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun laneArrowCount(hint: String): Int {
    val n = Regex("\\d+").find(hint)?.value?.toIntOrNull()
    return (n ?: if (hint.contains("any", ignoreCase = true)) 2 else 1).coerceIn(1, 3)
}

@Composable
internal fun LaneDiagram(
    lanes: List<Lane>,
    maneuver: ManeuverType = ManeuverType.STRAIGHT,
    on: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier,
) {
    val bright = on
    val dim = Color(0xFF80868B)
    val target = maneuverBucket(maneuver)
    Surface(color = on.copy(alpha = 0.10f), shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            lanes.take(8).forEach { lane ->
                val inds = lane.indications.ifEmpty { listOf("straight") }.distinct()
                val active = if (lane.valid && target != null) inds.minByOrNull { kotlin.math.abs(laneBucket(it) - target) } else null
                fun lit(ind: String) = lane.valid && (target == null || ind == active)
                Canvas(Modifier.size(width = 30.dp, height = 26.dp)) {
                    val cw = size.width; val ch = size.height
                    val baseX = cw / 2f; val bendY = ch * 0.44f; val stroke = cw * 0.11f
                    drawLine(if (lane.valid) bright else dim, Offset(baseX, ch * 0.92f), Offset(baseX, bendY), stroke, cap = StrokeCap.Round)
                    inds.sortedBy { if (lit(it)) 1 else 0 }.forEach { ind ->
                        laneHead(ind, if (lit(ind)) bright else dim, baseX, bendY, cw, ch, stroke)
                    }
                }
            }
        }
    }
}

private fun laneBucket(indication: String): Int = when (indication.trim().lowercase().replace('_', ' ')) {
    "uturn" -> -4
    "sharp left" -> -3
    "left" -> -2
    "slight left", "merge to left" -> -1
    "slight right", "merge to right" -> 1
    "right" -> 2
    "sharp right" -> 3
    else -> 0
}

private fun maneuverBucket(type: ManeuverType): Int? = when (type) {
    ManeuverType.UTURN -> -4
    ManeuverType.SHARP_LEFT -> -3
    ManeuverType.TURN_LEFT -> -2
    ManeuverType.SLIGHT_LEFT, ManeuverType.FORK_LEFT, ManeuverType.KEEP_LEFT, ManeuverType.RAMP_LEFT -> -1
    ManeuverType.SLIGHT_RIGHT, ManeuverType.FORK_RIGHT, ManeuverType.KEEP_RIGHT, ManeuverType.RAMP_RIGHT -> 1
    ManeuverType.TURN_RIGHT -> 2
    ManeuverType.SHARP_RIGHT -> 3
    ManeuverType.STRAIGHT, ManeuverType.CONTINUE, ManeuverType.DEPART -> 0
    else -> null
}

private fun DrawScope.laneHead(indication: String, color: Color, baseX: Float, bendY: Float, w: Float, h: Float, stroke: Float) {
    val deg = when (indication.trim().lowercase().replace('_', ' ')) {
        "straight", "none", "" -> 0f
        "slight right" -> 32f
        "slight left" -> -32f
        "right" -> 66f
        "left" -> -66f
        "sharp right" -> 108f
        "sharp left" -> -108f
        "uturn" -> 155f
        "merge to left" -> -32f
        "merge to right" -> 32f
        else -> 0f
    }
    val a = Math.toRadians(deg.toDouble())
    val headLen = h * 0.40f
    val tip = Offset(
        baseX + (kotlin.math.sin(a) * headLen).toFloat(),
        bendY - (kotlin.math.cos(a) * headLen).toFloat(),
    )
    drawLine(color, Offset(baseX, bendY), tip, stroke, cap = StrokeCap.Round)
    val barb = w * 0.22f
    listOf(150.0, -150.0).forEach { d ->
        val ba = a + Math.toRadians(d)
        drawLine(
            color, tip,
            Offset(
                tip.x + (kotlin.math.sin(ba) * barb).toFloat(),
                tip.y - (kotlin.math.cos(ba) * barb).toFloat(),
            ),
            stroke, cap = StrokeCap.Round,
        )
    }
}

@Composable
fun NavSearchChips(
    query: String,
    onQueryChange: (String) -> Unit,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isAppInDarkTheme()
    Card(
        modifier,
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = SheetPalette.bg(dark),
            contentColor = SheetPalette.ink(dark),
        ),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = SheetPalette.dim(dark), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = SheetPalette.ink(dark)),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onPick(query.trim()) }),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                stringResource(R.string.place_search_along_route),
                                style = MaterialTheme.typography.bodyLarge,
                                color = SheetPalette.dim(dark),
                            )
                        }
                        inner()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .dpadFieldEscape(),
                )
            }
            Row(
                Modifier.padding(horizontal = 12.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    Triple(R.string.cat_gas, "Gas", Icons.Default.LocalGasStation),
                    Triple(R.string.cat_food, "Food", Icons.Default.Restaurant),
                    Triple(R.string.cat_coffee, "Coffee", Icons.Default.LocalCafe),
                    Triple(R.string.cat_groceries, "Groceries", Icons.Default.LocalGroceryStore),
                ).forEach { (labelRes, query, icon) ->
                    FilterChip(
                        selected = false,
                        onClick = { onPick(query) },
                        border = null,
                        shape = androidx.compose.foundation.shape.CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (dark) Color(0xFF333539) else Color(0xFFF1F3F4),
                            labelColor = SheetPalette.ink(dark),
                        ),
                        label = { Text(stringResource(labelRes)) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = SheetPalette.dim(dark))
                        },
                        modifier = Modifier.dpadHighlight(androidx.compose.foundation.shape.CircleShape),
                    )
                }
            }
        }
    }
}

/**
 * Navigation bottom bar shifted to the TOP of the screen.
 */
@Composable
fun NavControls(
    remainingDistanceMeters: Double,
    remainingSeconds: Double,
    offRoute: Boolean,
    onStop: () -> Unit,
    onSteps: () -> Unit,
    trafficRatio: Double? = null,
    showListButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val dark = isAppInDarkTheme()
    val lift = remember { Animatable(0f) }
    val liftScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val latestSteps by rememberUpdatedState(onSteps)

    val etaColor = when {
        trafficRatio == null -> SheetPalette.ink(dark)
        trafficRatio > 1.4 -> SheetPalette.TrafficRed
        trafficRatio > 1.15 -> SheetPalette.TrafficAmber
        else -> SheetPalette.TrafficGreen
    }
    Card(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()                  // Shifts control panel below top camera cutouts/status bar
            .padding(top = 8.dp)                  // Top margin spacing
            .offset { IntOffset(0, lift.value.roundToInt().coerceAtMost(0)) }
            .pointerInput(Unit) {
                val commitPx = with(density) { NAV_BAR_LIFT_COMMIT_DP.dp.toPx() }
                val maxLiftPx = with(density) { NAV_BAR_LIFT_MAX_DP.dp.toPx() }
                val tracker = androidx.compose.ui.input.pointer.util.VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = { tracker.resetTracking() },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        liftScope.launch { lift.snapTo((lift.value + dy).coerceIn(-maxLiftPx, 0f)) }
                    },
                    onDragEnd = {
                        val vy = tracker.calculateVelocity().y
                        val commit = -lift.value > commitPx || vy < -NAV_BAR_FLING_PX_S
                        liftScope.launch {
                            if (commit) {
                                latestSteps()
                                lift.snapTo(0f)
                            } else {
                                lift.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = { liftScope.launch { lift.animateTo(0f) } },
                )
            },
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = SheetPalette.bg(dark),
            contentColor = SheetPalette.ink(dark),
        ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(20.dp)
                .dpadHighlight(RoundedCornerShape(10.dp))
                .clickable(onClick = onSteps),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(R.string.nav_steps_handle_cd),
                tint = SheetPalette.dim(dark),
                modifier = Modifier.size(22.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = onStop,
                modifier = Modifier.size(54.dp),
                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.nav_end), modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FitText(
                    formatDuration(remainingSeconds),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = etaColor,
                )
                FitText(
                    formatDistance(remainingDistanceMeters) +
                            " · " + formatArrivalClock(remainingSeconds) +
                            if (offRoute) " · " + stringResource(R.string.nav_rerouting) else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SheetPalette.dim(dark),
                )
            }
            Spacer(Modifier.width(8.dp))
            if (showListButton) {
                FilledTonalIconButton(onClick = onSteps, modifier = Modifier.size(54.dp)) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.nav_steps), modifier = Modifier.size(26.dp))
                }
            } else {
                Spacer(Modifier.size(54.dp))
            }
        }
    }
}

/**
 * Arrival summary display
 */
@Composable
fun ArrivalSummary(
    destinationLabel: String,
    destinationAddress: String = "",
    tripSeconds: Double,
    tripDistanceMeters: Double,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text(stringResource(R.string.nav_arrived), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (destinationLabel.isNotBlank()) {
                        Text(destinationLabel, style = MaterialTheme.typography.bodyLarge)
                    }
                    if (destinationAddress.isNotBlank() && !destinationAddress.equals(destinationLabel, ignoreCase = true)) {
                        Text(destinationAddress, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column {
                    Text(stringResource(R.string.nav_trip_time), style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatDuration(tripSeconds),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column {
                    Text(stringResource(R.string.nav_distance), style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatDistance(tripDistanceMeters),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.nav_done))
            }
        }
    }
}