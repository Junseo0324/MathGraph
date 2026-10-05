package com.devhjs.mathgraphstudy.presentation.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 그래프 이미지를 PNG 로 저장해 다른 앱(메신저, 갤러리 등)으로 공유합니다.
 * 파일은 캐시 디렉터리에 두고 FileProvider 로 읽기 권한만 넘깁니다.
 */
object GraphImageSharer {

    private const val DIRECTORY = "shared"
    private const val FILE_NAME = "graph.png"

    suspend fun share(context: Context, image: ImageBitmap) {
        val file = withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
            File(directory, FILE_NAME).also { file ->
                file.outputStream().use { out ->
                    image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            // 공유 대상 선택 화면도 미리보기를 읽을 수 있도록 ClipData 로 권한을 함께 넘김
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "그래프 공유").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
    }
}
