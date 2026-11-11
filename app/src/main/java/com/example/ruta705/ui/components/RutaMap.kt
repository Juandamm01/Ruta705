package com.example.ruta705.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ruta705.BuildConfig
import com.example.ruta705.core.network.LatLngDto
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** Centro de Villavicencio (Parque Los Libertadores) como vista inicial. */
val VILLAVICENCIO_CENTER = LatLngDto(4.1420, -73.6266)

data class MapLine(val points: List<LatLngDto>, val color: Color, val widthDp: Float = 6f, val dashed: Boolean = false)

enum class MarkerKind { Stop, Origin, Destination, Transfer, Report, Me }

data class MapMarker(val position: LatLngDto, val title: String, val kind: MarkerKind)

/** Fuente de teselas configurable (p. ej. MapTiler/Stadia para producción) con plantilla {z}/{x}/{y}. */
private class TemplateTileSource(private val template: String) :
    OnlineTileSourceBase("ruta705-custom", 0, 19, 256, "", arrayOf(template), "© OpenStreetMap contributors") {
    override fun getTileURLString(pMapTileIndex: Long): String = template
        .replace("{z}", MapTileIndex.getZoom(pMapTileIndex).toString())
        .replace("{x}", MapTileIndex.getX(pMapTileIndex).toString())
        .replace("{y}", MapTileIndex.getY(pMapTileIndex).toString())
}

private fun markerIcon(context: Context, kind: MarkerKind, density: Float): BitmapDrawable {
    val (fill, stroke, radiusDp) = when (kind) {
        MarkerKind.Stop -> Triple(0xFFFFFFFF.toInt(), 0xFF0D3B36.toInt(), 6f)
        MarkerKind.Origin -> Triple(0xFF2E7D5B.toInt(), 0xFFFFFFFF.toInt(), 9f)
        MarkerKind.Destination -> Triple(0xFFF2B705.toInt(), 0xFF16211F.toInt(), 10f)
        MarkerKind.Transfer -> Triple(0xFFFFFFFF.toInt(), 0xFFB8860B.toInt(), 8f)
        MarkerKind.Report -> Triple(0xFFB3261E.toInt(), 0xFFFFFFFF.toInt(), 8f)
        MarkerKind.Me -> Triple(0xFF2B5C8A.toInt(), 0xFFFFFFFF.toInt(), 8f)
    }
    val radius = radiusDp * density
    val strokeWidth = 3f * density
    val size = ((radius + strokeWidth) * 2).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = fill
    canvas.drawCircle(size / 2f, size / 2f, radius, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = strokeWidth
    paint.color = stroke
    canvas.drawCircle(size / 2f, size / 2f, radius, paint)
    return BitmapDrawable(context.resources, bitmap)
}

/**
 * Mapa OpenStreetMap (osmdroid) para Compose. Solo dibuja lo que recibe: recorridos y puntos.
 * `fitPoints` encuadra la vista una vez por cada conjunto distinto de puntos.
 */
@Composable
fun RutaMap(
    modifier: Modifier = Modifier,
    lines: List<MapLine> = emptyList(),
    markers: List<MapMarker> = emptyList(),
    fitPoints: List<LatLngDto> = emptyList(),
    initialCenter: LatLngDto = VILLAVICENCIO_CENTER,
    initialZoom: Double = 13.5,
    interactive: Boolean = true,
    onCenterChanged: ((LatLngDto) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = context.resources.displayMetrics.density
    val fitState = remember { object { var lastFit: Int? = null } }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(
                BuildConfig.MAP_TILE_URL.takeIf { it.contains("{z}") }?.let { TemplateTileSource(it) } ?: TileSourceFactory.MAPNIK
            )
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 11.0
            maxZoomLevel = 19.0
            isTilesScaledToDpi = true
            setScrollableAreaLimitDouble(BoundingBox(4.30, -73.45, 3.95, -73.85))
            controller.setZoom(initialZoom)
            controller.setCenter(GeoPoint(initialCenter.lat, initialCenter.lng))
            if (!interactive) setOnTouchListener { _, event -> event.action != MotionEvent.ACTION_CANCEL }
            overlays.add(CopyrightOverlay(context).apply { setTextSize(10) })
            if (onCenterChanged != null) {
                addMapListener(DelayedMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?): Boolean {
                        onCenterChanged(LatLngDto(mapCenter.latitude, mapCenter.longitude)); return true
                    }

                    override fun onZoom(event: ZoomEvent?): Boolean {
                        onCenterChanged(LatLngDto(mapCenter.latitude, mapCenter.longitude)); return true
                    }
                }, 250))
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        // osmdroid puede dibujar fuera de su vista: se recorta para no invadir el contenido vecino.
        modifier = modifier.clipToBounds(),
        factory = { mapView },
        update = { map ->
            map.overlays.removeAll { it is Polyline || it is Marker }
            lines.filter { it.points.size >= 2 }.forEach { line ->
                map.overlays.add(Polyline(map).apply {
                    setPoints(line.points.map { GeoPoint(it.lat, it.lng) })
                    outlinePaint.color = line.color.toArgb()
                    outlinePaint.strokeWidth = line.widthDp * density
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    if (line.dashed) outlinePaint.pathEffect = DashPathEffect(floatArrayOf(4 * density, 8 * density), 0f)
                    isGeodesic = false
                    infoWindow = null
                })
            }
            markers.forEach { item ->
                map.overlays.add(Marker(map).apply {
                    position = GeoPoint(item.position.lat, item.position.lng)
                    title = item.title
                    icon = markerIcon(context, item.kind, density)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setInfoWindowAnchor(Marker.ANCHOR_CENTER, 0f)
                })
            }
            val key = fitPoints.hashCode()
            if (fitPoints.size >= 2 && fitState.lastFit != key) {
                fitState.lastFit = key
                val box = BoundingBox.fromGeoPoints(fitPoints.map { GeoPoint(it.lat, it.lng) })
                map.post { map.zoomToBoundingBox(box.increaseByScale(1.25f), false, (32 * density).toInt()) }
            } else if (fitPoints.size == 1 && fitState.lastFit != key) {
                fitState.lastFit = key
                map.controller.setZoom(16.0)
                map.controller.setCenter(GeoPoint(fitPoints[0].lat, fitPoints[0].lng))
            }
            map.invalidate()
        },
    )
}
