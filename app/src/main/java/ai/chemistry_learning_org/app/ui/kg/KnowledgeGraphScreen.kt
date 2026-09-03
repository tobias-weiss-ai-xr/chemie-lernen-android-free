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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlin.math.sqrt

private data class Camera(val yaw: Double, val pitch: Double, val zoom: Float) {
    companion object {
        val DEFAULT = Camera(yaw = 0.7, pitch = 0.35, zoom = 1f)
    }
}

private sealed interface KgUiState {
    data object Loading : KgUiState

    /** Snapshot fetched and graph built; 3D layout still computing. */
    data class Computing(val graph: KgGraphData.Graph, val fromCache: Boolean) : KgUiState

    data class Ready(
        val graph: KgGraphData.Graph,
        val positions: List<Graph3DLayout.Vec3>,
        val fromCache: Boolean,
    ) : KgUiState

    data object Unavailable : KgUiState
}

/**
 * Native 3D knowledge graph (Wissensnetz): force-directed layout computed
 * on device from the platform kg-data API, rendered on a Compose Canvas.
 * Drag rotates, pinch zooms, tap selects a node and opens the detail card
 * with related terms and article deep links. Replaces the removed
 * /wissensnetz/ web page (404) — not a port of the web D3 view.
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
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var camera by remember { mutableStateOf(Camera.DEFAULT) }

    val repo = remember {
        KnowledgeGraphRepository(
            fetchJson = { url -> withContext(Dispatchers.IO) { URL(url).readText() } },
            cacheFile = File(context.filesDir, "kg-cache.json"),
        )
    }

    suspend fun load() {
        state = KgUiState.Loading
        selectedIndex = null
        state = when (val result = repo.load()) {
            is KnowledgeGraphRepository.LoadResult.Success ->
                KgUiState.Computing(KgGraphData.build(result.snapshot), fromCache = false)
            is KnowledgeGraphRepository.LoadResult.Offline ->
                KgUiState.Computing(KgGraphData.build(result.snapshot), fromCache = true)
            KnowledgeGraphRepository.LoadResult.Unavailable -> KgUiState.Unavailable
        }
    }

    LaunchedEffect(Unit) { load() }

    // heavy force-directed layout off the main thread (fixed iteration budget)
    LaunchedEffect(state) {
        val computing = state as? KgUiState.Computing ?: return@LaunchedEffect
        val positions = withContext(Dispatchers.Default) {
            Graph3DLayout.compute(Graph3DLayout.Input(computing.graph.nodes, computing.graph.edges))
        }
        state = KgUiState.Ready(computing.graph, positions, computing.fromCache)
    }

    // gentle auto-rotation while nothing is selected
    LaunchedEffect(state) {
        if (state !is KgUiState.Ready) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L && selectedIndex == null) {
                    camera = camera.copy(yaw = camera.yaw + (now - last) * 0.00000005)
                }
                last = now
            }
        }
    }

    BackHandler(enabled = selectedIndex != null) { selectedIndex = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.kg_title), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { if (selectedIndex != null) selectedIndex = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        searchOpen = !searchOpen
                        if (!searchOpen) query = ""
                    }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.kg_search_hint))
                    }
                    IconButton(onClick = { camera = Camera.DEFAULT }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.kg_reset_view))
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
            when (val current = state) {
                KgUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                is KgUiState.Computing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.kg_computing), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                KgUiState.Unavailable -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.kg_error), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { scope.launch { load() } }) {
                            Text(stringResource(R.string.kg_retry))
                        }
                    }
                }

                is KgUiState.Ready -> {
                    if (current.fromCache) {
                        Text(
                            text = stringResource(R.string.kg_offline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        )
                    }
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
                                items(KgCategories.chipOrder) { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = {
                                            selectedCategory =
                                                if (selectedCategory == category) null else category
                                        },
                                        label = { Text(stringResource(KgCategories.labelRes(category))) },
                                        leadingIcon = {
                                            Box(
                                                Modifier
                                                    .size(10.dp)
                                                    .background(
                                                        Color(KgCategories.colorHex(category)),
                                                        CircleShape,
                                                    ),
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        GraphCanvas(
                            graph = current.graph,
                            positions = current.positions,
                            camera = camera,
                            onCamera = { camera = it },
                            query = query,
                            selectedCategory = selectedCategory,
                            selectedIndex = selectedIndex,
                            onSelect = { selectedIndex = it },
                        )
                        selectedIndex?.let { idx ->
                            if (idx in current.graph.nodes.indices) {
                                NodeDetailCard(
                                    graph = current.graph,
                                    nodeIndex = idx,
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    onClose = { selectedIndex = null },
                                    onFocus = { name ->
                                        selectedIndex = current.graph.index[name.lowercase()] ?: idx
                                    },
                                    onOpenArticle = onOpenArticle,
                                )
                            }
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
    camera: Camera,
    onCamera: (Camera) -> Unit,
    query: String,
    selectedCategory: String?,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
) {
    val currentCamera by rememberUpdatedState(camera)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(graph, positions) {
                detectTapGestures { offset ->
                    val cam = currentCamera
                    val o = Graph3DProjector.Orientation(cam.yaw, cam.pitch, cam.zoom.toDouble())
                    var best = -1
                    var bestDist = Double.MAX_VALUE
                    for (i in positions.indices) {
                        val sp = Graph3DProjector.project(
                            positions[i], o, size.width.toFloat(), size.height.toFloat(),
                        )
                        val dx = (sp.x - offset.x).toDouble()
                        val dy = (sp.y - offset.y).toDouble()
                        val d2 = dx * dx + dy * dy
                        if (d2 < bestDist) {
                            bestDist = d2
                            best = i
                        }
                    }
                    val threshold = 36.dp.toPx().toDouble()
                    onSelect(if (best >= 0 && bestDist <= threshold * threshold) best else null)
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val cam = currentCamera
                    onCamera(
                        cam.copy(
                            yaw = cam.yaw + pan.x * 0.006,
                            pitch = (cam.pitch + pan.y * 0.006).coerceIn(-1.5, 1.5),
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
        val o = Graph3DProjector.Orientation(cam.yaw, cam.pitch, cam.zoom.toDouble())

        val px = FloatArray(n)
        val py = FloatArray(n)
        val pd = FloatArray(n)
        val ps = FloatArray(n)
        for (i in 0 until n) {
            val sp = Graph3DProjector.project(positions[i], o, w, h)
            px[i] = sp.x
            py[i] = sp.y
            pd[i] = sp.depth
            ps[i] = sp.scale
        }

        val q = query.trim()
        val bright = BooleanArray(n)
        for (i in 0 until n) {
            val node = graph.nodes[i]
            bright[i] = (selectedCategory == null || node.category == selectedCategory) &&
                (q.isEmpty() || node.name.contains(q, ignoreCase = true))
        }

        val sel = selectedIndex?.takeIf { it in 0 until n }
        val strokeUnit = 1.dp.toPx()
        for ((a, b) in graph.edges) {
            val highlighted = sel != null && (a == sel || b == sel)
            if (sel != null && !highlighted && !bright[a] && !bright[b]) continue
            val depth = (pd[a] + pd[b]) / 2f
            val color: Color
            val alpha: Float
            if (highlighted) {
                val other = if (a == sel) b else a
                color = Color(KgCategories.colorHex(graph.nodes[other].category))
                alpha = 0.85f
            } else {
                color = onSurfaceVariant
                alpha = (0.04f + 0.20f * depth) * if (bright[a] || bright[b]) 1f else 0.2f
            }
            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(px[a], py[a]),
                end = Offset(px[b], py[b]),
                strokeWidth = strokeUnit * (0.6f + 0.7f * depth),
                cap = StrokeCap.Round,
            )
        }

        // painter's order: far nodes first
        val order = (0 until n).sortedBy { pd[it] }
        val baseR = 3.dp.toPx()
        for (i in order) {
            val node = graph.nodes[i]
            val weight = sqrt(node.relationCount.coerceAtLeast(1).toDouble()).toFloat()
            val radius = baseR * (1f + weight * 0.20f) * ps[i] * (0.55f + 0.65f * pd[i])
            val center = Offset(px[i], py[i])
            val color = Color(KgCategories.colorHex(node.category))
            val alpha = when {
                i == sel -> 1f
                bright[i] -> 0.45f + 0.55f * pd[i]
                else -> 0.05f + 0.07f * pd[i]
            }
            drawCircle(color = color.copy(alpha = alpha), radius = radius, center = center)
            if (i == sel) {
                drawCircle(
                    color = color,
                    radius = radius + 3.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
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
                        text = stringResource(KgCategories.labelRes(node.category ?: "")),
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
                Text(node.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
