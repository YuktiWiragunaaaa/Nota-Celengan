package id.cukup.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import id.cukup.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Membaca foto untuk dipotong, diperkecil ke sisi pendek ±1024 px dan diputar sesuai EXIF. */
suspend fun loadForCrop(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
                val short = minOf(info.size.width, info.size.height)
                if (short > 1024) d.setTargetSampleSize(short / 1024)
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    }.getOrNull()
}

/**
 * Layar potong foto profil: geser untuk memindah, cubit untuk memperbesar.
 * [onDone] menerima potongan persegi 512 px.
 */
@Composable
fun AvatarCropDialog(source: Bitmap, onCancel: () -> Unit, onDone: (Bitmap) -> Unit) {
    val image = remember(source) { source.asImageBitmap() }
    var zoom by remember { mutableFloatStateOf(1f) }
    var ox by remember { mutableFloatStateOf(0f) }
    var oy by remember { mutableFloatStateOf(0f) }
    var viewport by remember { mutableFloatStateOf(0f) }

    fun baseScale(v: Float) = max(v / source.width, v / source.height)
    fun clamp(v: Float) {
        val s = baseScale(v) * zoom
        val maxX = (source.width * s - v) / 2f
        val maxY = (source.height * s - v) / 2f
        ox = ox.coerceIn(-maxX, maxX)
        oy = oy.coerceIn(-maxY, maxY)
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
            Text("Sesuaikan foto", style = Type.title, color = Color.White, modifier = Modifier.padding(20.dp))
            Text(
                "Geser untuk memindah, cubit untuk memperbesar.",
                style = Type.bodySmall, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 20.dp),
            )
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxWidth().padding(20.dp).aspectRatio(1f)) {
                    Canvas(
                        Modifier.fillMaxSize().pointerInput(Unit) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                zoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                                ox += pan.x
                                oy += pan.y
                                clamp(size.width.toFloat())
                            }
                        },
                    ) {
                        val v = size.width
                        viewport = v
                        val s = baseScale(v) * zoom
                        val w = source.width * s
                        val h = source.height * s
                        drawImage(
                            image,
                            dstOffset = IntOffset(((v - w) / 2f + ox).roundToInt(), ((v - h) / 2f + oy).roundToInt()),
                            dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                        )
                        val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset.Zero, Size(v, v))) }
                        clipPath(circle, clipOp = ClipOp.Difference) { drawRect(Color.Black.copy(alpha = 0.55f)) }
                        drawCircle(Color.White.copy(alpha = 0.8f), radius = v / 2f, style = Stroke(2.dp.toPx()))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onCancel) { Text("Batal", color = Color.White) }
                TextButton({
                    val v = viewport
                    if (v <= 0f) return@TextButton
                    val s = baseScale(v) * zoom
                    val side = (v / s).roundToInt().coerceIn(1, minOf(source.width, source.height))
                    val left = (((source.width * s - v) / 2f - ox) / s).roundToInt().coerceIn(0, source.width - side)
                    val top = (((source.height * s - v) / 2f - oy) / s).roundToInt().coerceIn(0, source.height - side)
                    val cropped = Bitmap.createBitmap(source, left, top, side, side)
                    onDone(Bitmap.createScaledBitmap(cropped, 512, 512, true))
                }) { Text("Pakai foto", color = Color.White, style = Type.strong) }
            }
        }
    }
}
