package id.cukup.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.cukup.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private fun avatarFile(context: Context) = File(context.filesDir, "avatar.jpg")

/** Menyalin foto pilihan ke penyimpanan aplikasi (diperkecil ke 512 px). Mengembalikan true bila berhasil. */
suspend fun saveAvatar(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@runCatching false
        avatarFile(context).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        true
    }.getOrDefault(false)
}

fun deleteAvatar(context: Context) {
    avatarFile(context).delete()
}

/** Foto profil bulat; bila belum ada, huruf pertama nama. */
@Composable
fun Avatar(
    name: String,
    version: Long,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    background: Color = Color.White.copy(alpha = 0.18f),
    textColor: Color = Color.White,
) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, version) {
        value = if (version <= 0) null else withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(avatarFile(context).path)?.asImageBitmap() }.getOrNull()
        }
    }
    Box(modifier.size(size).clip(CircleShape).background(background), contentAlignment = Alignment.Center) {
        val img = image
        if (img != null) {
            Image(img, "Foto profil", contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(
                name.firstOrNull()?.uppercase() ?: "☺",
                style = Type.strong.copy(fontSize = Type.strong.fontSize * (size.value / 40f)),
                color = textColor,
            )
        }
    }
}
