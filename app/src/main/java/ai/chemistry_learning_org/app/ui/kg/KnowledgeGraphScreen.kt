package ai.chemistry_learning_org.app.ui.kg

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.chemistry_learning_org.R
import ai.chemistry_learning_org.app.kg.Graph3DLayout
import ai.chemistry_learning_org.app.kg.Graph3DProjector
import ai.chemistry_learning_org.app.kg.KgCategories
import ai.chemistry_learning_org.app.kg.KgGraphData
import ai.chemistry_learning_org.app.kg.KnowledgeGraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlin.math.min
import kotlin.math.sqrt

private data class Camera(val yaw: Double, val pitch: Double, val zoom: Float) {
    companion object {
        val THREE_D = Camera(yaw = 0.7, pitch = 0.35, zoom = 1f)
        val TWO_D = Camera(yaw = 0.0, pitch = 0.0, zoom = 1f)
    }
}

private sealed interface KgUiState {
    data object Loading : KgUiState

    data class Picker(val graph: KgGraphData.Graph, val fromCache: Boolean) : KgUiState

    data class Ready(
        val ego: KgGraphData.Graph,
        val positions: List<Graph3DLayout.Vec3>,
        val centerName: String,
        val is3D: Boolean,
        val fromCache: Boolean,
    ) : KgUiState

    data object Unavailable : KgUiState
}

/**
 * Native knowledge-graph (Wissensnetz) screen: ego-graph exploration with
 * per-term neighborhoods, 2D/3D toggle via planar layout config.
 * Drag rotates (3D) or does nothing (2D), pinch zooms both, tap selects.
 * Picker lets the user choose a starting term from the full kg dataset;
 * Ready shows an ego graph (center plus 1-hop) with interactive canvas.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KnowledgeGraphScreen(
    onBack: () -> Unit,
    onOpenArticle: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<KgUiState>(KgUiState.Loading) }
    var fullGraph by remember { mutableStateOf<KgGraphData.Graph?>(null) }
    var fromCache by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var camera by remember { mutableStateOf(Camera.THREE_D) }
    var selectJob by remember { mutableStateOf<Job?>(null) }

    val repo = remember {
        KnowledgeGraphRepository(
            fetchJson = { url -> withContext(Dispatchers.IO) { URL(url).readText() } },
            cacheFile = File(context.filesDir, "kg-cache.json"),
        )
    }

    suspend fun load() {
        state = KgUiState.Loading
        selectedIndex = null
        fromCache = false
        selectJob?.cancel()
        selectJob = null
        when (val result = repo.load()) {
            is KnowledgeGraphRepository.LoadResult.Success -> {
                val graph = KgGraphData.build(result.snapshot)
                fullGraph = graph
                fromCache = false
                state = KgUiState.Picker(graph, fromCache = false)
            }
            is KnowledgeGraphRepository.LoadResult.Offline -> {
                val graph = KgGraphData.build(result.snapshot)
                fullGraph = graph
                fromCache = true
                state = KgUiState.Picker(graph, fromCache = true)
            }
            KnowledgeGraphRepository.LoadResult.Unavailable -> {
                state = KgUiState.Unavailable
            }
        }
    }

    fun selectCenter(name: String, threeD: Boolean) {
        val full = fullGraph ?: return
        val ego = KgGraphData.buildEgo(full, name) ?: return
        selectJob?.cancel()
        selectJob = null
        selectedIndex = null
        camera = if (threeD) Camera.THREE_D else Camera.TWO_D
        selectJob = scope.launch {
            val positions = withContext(Dispatchers.Default) {
                Graph3DLayout.compute(
                    Graph3DLayout.Input(ego.nodes, ego.edges),
                    Graph3DLayout.Config(planar = !threeD),
                )
            }
            state = KgUiState.Ready(ego, positions, ego.nodes[0].name, threeD, fromCache)
        }
    }

    fun goBack() {
        val s = state
        when {
            selectedIndex != null -> selectedIndex = null
            s is KgUiState.Ready -> {
                selectJob?.cancel()
                selectJob = null
                state = KgUiState.Picker(fullGraph ?: s.ego, fromCache)
            }
            searchOpen -> {
                searchOpen = false
                query = ""
            }
            else -> onBack()
        }
    }

    LaunchedEffect(Unit) { load() }

    // Auto-rotate while nothing is selected and the 3D view is showing.
    // ~1e-10 rad/ns = 0.1 rad/s ≈ 63 s per revolution — slow enough to read labels.
    // Restarts when is3D changes (via key) so rotation continues with new orientation.
    // 2D is a static top-down view: rotating yaw there spins the whole planar disc.
    LaunchedEffect((state as? KgUiState.Ready)?.is3D) {
        // 2D is a static top-down view: skip the frame loop entirely.
        // The key restarts the effect on a 3D toggle, so rotation resumes on switch.
        if ((state as? KgUiState.Ready)?.is3D != true) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L && selectedIndex == null) {
                    camera = camera.copy(yaw = camera.yaw + (now - last) * 1e-10)
                }
                last = now
            }
        }
    }

    // Dismiss detail card on system back
    BackHandler(enabled = true) { goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.kg_title), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    val s = state
                    when (s) {
                        KgUiState.Loading, KgUiState.Unavailable -> Unit
                        is KgUiState.Picker -> {
                            IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) query = "" }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.kg_search_hint))
                            }
                        }
                        is KgUiState.Ready -> {
                            val toggleDescription = stringResource(R.string.kg_toggle_projection)
                            TextButton(
                                onClick = { selectCenter(s.centerName, !s.is3D) },
                                modifier = Modifier.clearAndSetSemantics {
                                    contentDescription = toggleDescription
                                },
                            ) {
                                Text(if (s.is3D) "3D" else "2D")
                            }
                            IconButton(onClick = {
                                camera = if (s.is3D) Camera.THREE_D else Camera.TWO_D
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.kg_reset_view))
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val s = state) {
                KgUiState.Loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                is KgUiState.Picker -> {
                    if (s.fromCache) {
                        Text(
                            text = stringResource(R.string.kg_offline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.kg_pick_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    if (searchOpen) {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(R.string.kg_search_hint)) },
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 8.dp),
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null },
                                        label = { Text(stringResource(R.string.kg_all)) },
                                    )
                                }
                                items(KgCategories.chipOrder) { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = {
                                            selectedCategory =
                                                if (selectedCategory == cat) null else cat
                                        },
                                        label = { Text(stringResource(KgCategories.labelRes(cat))) },
                                        leadingIcon = {
                                            Box(
                                                Modifier
                                                    .size(10.dp)
                                                    .background(
                                                        Color(KgCategories.colorHex(cat)),
                                                        CircleShape,
                                                    ),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategory == null,
                                    onClick = { selectedCategory = null },
                                    label = { Text(stringResource(R.string.kg_all)) },
                                )
                            }
                            items(KgCategories.chipOrder) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = {
                                        selectedCategory = if (selectedCategory == cat) null else cat
                                    },
                                    label = { Text(stringResource(KgCategories.labelRes(cat))) },
                                    leadingIcon = {
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .background(
                                                    Color(KgCategories.colorHex(cat)),
                                                    CircleShape,
                                                ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                    val filtered = remember(s.graph, query, selectedCategory) {
                        val q = query.trim()
                        s.graph.nodes
                            .asSequence()
                            .filter { selectedCategory == null || it.category.equals(selectedCategory, ignoreCase = true) }
                            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
                            .sortedByDescending { it.relationCount }
                            .take(200)
                            .toList()
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.kg_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f)) {
                            items(filtered.size) { i ->
                                val entity = filtered[i]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectCenter(entity.name, threeD = true) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier
                                            .size(12.dp)
                                            .background(
                                                Color(KgCategories.colorHex(entity.category)),
                                                CircleShape,
                                            ),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = entity.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = stringResource(KgCategories.labelRes(entity.category)) +
                                                " \u00B7 " +
                                                stringResource(R.string.kg_relations, entity.relationCount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is KgUiState.Ready -> {
                    if (s.fromCache) {
                        Text(
                            text = stringResource(R.string.kg_offline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        GraphCanvas(
                            graph = s.ego,
                            positions = s.positions,
                            is3D = s.is3D,
                            camera = camera,
                            onCamera = { camera = it },
                            selectedIndex = selectedIndex,
                            onSelect = { selectedIndex = it },
                        )
                        selectedIndex?.let { idx ->
                            if (idx in s.ego.nodes.indices) {
                                NodeDetailCard(
                                    graph = s.ego,
                                    nodeIndex = idx,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    onClose = { selectedIndex = null },
                                    onFocus = { name -> selectCenter(name, s.is3D) },
                                    onOpenArticle = onOpenArticle,
                                )
                            }
                        }
                    }
                }

                KgUiState.Unavailable ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stringResource(R.string.kg_error),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { scope.launch { load() } }) {
                                Text(stringResource(R.string.kg_retry))
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun GraphCanvas(
    graph: KgGraphData.Graph,
    positions: List<Graph3DLayout.Vec3>,
    is3D: Boolean,
    camera: Camera,
    onCamera: (Camera) -> Unit,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
) {
    val currentCamera by rememberUpdatedState(camera)
    val currentIs3D by rememberUpdatedState(is3D)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer(cacheSize = 128)
    val graphDesc = stringResource(R.string.kg_title)

    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = onSurface,
        textAlign = TextAlign.Center,
        shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(1f, 1f), blurRadius = 2f),
    )
    val centerLabelStyle = labelStyle.copy(fontWeight = FontWeight.Bold)

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clearAndSetSemantics { contentDescription = graphDesc }
            .pointerInput(graph, positions, is3D) {
                detectTapGestures { offset ->
                    val cam = currentCamera
                    val threeD = currentIs3D
                    val size = size
                    val o = viewOrientation(cam, threeD, size.width.toFloat(), size.height.toFloat(), positions)
                    var best = -1
                    var bestDist = Double.MAX_VALUE
                    for (i in positions.indices) {
                        val sp = Graph3DProjector.project(positions[i], o, size.width.toFloat(), size.height.toFloat())
                        val dx = (sp.x - offset.x).toDouble()
                        val dy = (sp.y - offset.y).toDouble()
                        val d2 = dx * dx + dy * dy
                        if (d2 < bestDist) {
                            bestDist = d2
                            best = i
                        }
                    }
                    val threshold = 40.dp.toPx().toDouble()
                    onSelect(if (best >= 0 && bestDist <= threshold * threshold) best else null)
                }
            }
            .pointerInput(is3D) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val cam = currentCamera
                    val threeD = currentIs3D
                    onCamera(
                        if (threeD) cam.copy(
                            yaw = cam.yaw + pan.x * 0.006,
                            pitch = (cam.pitch + pan.y * 0.006).coerceIn(-1.5, 1.5),
                            zoom = (cam.zoom * zoom).coerceIn(0.35f, 5f),
                        ) else cam.copy(
                            zoom = (cam.zoom * zoom).coerceIn(0.35f, 5f),
                        ),
                    )
                }
            },
    ) {
        val n = positions.size
        if (n == 0) return@Canvas

        val w = size.width
        val h = size.height
        val cam = currentCamera
        val o = viewOrientation(cam, is3D, w, h, positions)

        val px = FloatArray(n)
        val py = FloatArray(n)
        val pd = FloatArray(n)
        val ps = FloatArray(n)
        val pr = FloatArray(n)
        for (i in 0 until n) {
            val sp = Graph3DProjector.project(positions[i], o, w, h)
            px[i] = sp.x
            py[i] = sp.y
            pd[i] = sp.depth
            ps[i] = sp.scale
            val node = graph.nodes[i]
            val weight = sqrt(node.relationCount.coerceAtLeast(1).toDouble()).toFloat()
            pr[i] = (3.dp.toPx() * (1f + weight * 0.20f) * ps[i] * (0.55f + 0.65f * pd[i]) * o.zoom.toFloat()).coerceAtMost(28.dp.toPx())
        }

        val sel = selectedIndex?.takeIf { it in 0 until n }

        // Draw edges: painter's order (far to near only matters in 3D; in 2D all depth=1)
        val strokeUnit = 1.dp.toPx()
        for ((a, b) in graph.edges) {
            val depth = (pd[a] + pd[b]) / 2f
            val highlighted = sel != null && (a == sel || b == sel)
            if (sel != null && !highlighted) continue
            val color = if (highlighted) {
                val other = if (a == sel) b else a
                Color(KgCategories.colorHex(graph.nodes[other].category))
            } else {
                onSurfaceVariant
            }
            val alpha = if (highlighted) 0.85f else if (is3D) 0.04f + 0.20f * depth else 0.30f
            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(px[a], py[a]),
                end = Offset(px[b], py[b]),
                strokeWidth = strokeUnit * (0.6f + 0.7f * depth),
                cap = StrokeCap.Round,
            )
        }

        // Draw nodes
        val order = (0 until n).sortedBy { pd[it] }
        for (i in order) {
            val node = graph.nodes[i]
            val center = Offset(px[i], py[i])
            val color = Color(KgCategories.colorHex(node.category))
            val alpha = when {
                i == sel -> 1f
                else -> 0.45f + 0.55f * pd[i]
            }
            drawCircle(color = color.copy(alpha = alpha), radius = pr[i], center = center)
            if (i == sel) {
                drawCircle(
                    color = color,
                    radius = pr[i] + 3.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        // Draw labels for ego graphs (nodes <= 60)
        if (n <= 60) {
            for (i in order) {
                if (is3D && pd[i] < 0.35f) continue
                val node = graph.nodes[i]
                var label = node.name
                if (label.length > 20) label = label.take(19) + "\u2026"
                val style = if (i == 0) centerLabelStyle else labelStyle
                val layout = textMeasurer.measure(label, style)
                val alpha = if (i == sel) 1f else 0.85f
                val lx = (px[i] - layout.size.width / 2f).coerceIn(0f, (w - layout.size.width).coerceAtLeast(0f))
                val ly = (py[i] + pr[i] + 2.dp.toPx()).coerceIn(0f, (h - layout.size.height).coerceAtLeast(0f))
                drawText(
                    layout,
                    topLeft = Offset(lx, ly),
                    alpha = alpha,
                )
            }
        }
    }
}

/** Computes the view orientation + fit factor for the current camera and mode. */
private fun viewOrientation(
    cam: Camera,
    threeD: Boolean,
    width: Float,
    height: Float,
    positions: List<Graph3DLayout.Vec3>,
): Graph3DProjector.Orientation {
    var maxR = 1.0
    for (p in positions) {
        val r = if (threeD) {
            sqrt(p.x * p.x + p.y * p.y + p.z * p.z)
        } else {
            sqrt(p.x * p.x + p.y * p.y)
        }
        if (r > maxR) maxR = r
    }
    val fit = (min(width, height) / (2.0 * maxR) * 0.40).coerceIn(0.05, 20.0)
    return if (threeD) {
        Graph3DProjector.Orientation(cam.yaw, cam.pitch, cam.zoom * fit)
    } else {
        Graph3DProjector.Orientation(0.0, 0.0, cam.zoom * fit)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NodeDetailCard(
    graph: KgGraphData.Graph,
    nodeIndex: Int,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onFocus: (String) -> Unit = {},
    onOpenArticle: (String, String) -> Unit = { _, _ -> },
) {
    val node = graph.nodes[nodeIndex]
    val center = remember(nodeIndex, graph) { KgCategories.buildCenter(node, graph.articles) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
    ) {
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 420.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(Color(KgCategories.colorHex(node.category)), CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(KgCategories.labelRes(node.category)),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.kg_relations, node.relationCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            item {
                Text(
                    node.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            node.description?.let { description ->
                item {
                    Text(description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (center.related.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.kg_related_terms),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        center.related.take(24).forEach { rel ->
                            AssistChip(
                                onClick = { onFocus(rel.name) },
                                label = {
                                    Text(
                                        rel.name,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }
            }
            if (center.articles.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.kg_related_articles),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(center.articles, key = { it.url }) { article ->
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArticle(article.url, article.title) }
                            .padding(vertical = 6.dp),
                    )
                }
            }
        }
    }
}
