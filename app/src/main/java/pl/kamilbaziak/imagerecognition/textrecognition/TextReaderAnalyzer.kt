package pl.kamilbaziak.imagerecognition.textrecognition

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import pl.kamilbaziak.imagerecognition.ui.MainActivity.Companion.TAG
import pl.kamilbaziak.imagerecognition.utils.Utils
import pl.kamilbaziak.imagerecognition.utils.Utils.toBitmap
import java.io.IOException

class TextReaderAnalyzer(
    private val textFoundListener: (String) -> Unit,
    private val percentages: Pair<Int, Int>
) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        var imageCropPercentages = percentages?: return
        var mediaImage = imageProxy.image!!.toBitmap()

        if (mediaImage.width > mediaImage.height)
            mediaImage = Utils.rotateBitmap(mediaImage, 90f)!!

        val imageHeight = mediaImage.height
        val imageWidth = mediaImage.width

        val actualAspectRatio = imageWidth / imageHeight

        val cropRect = Rect(0, 0, imageWidth, imageHeight)

        val currentCropPercentages = imageCropPercentages
        if (actualAspectRatio > 3) {
            val originalHeightCropPercentage = currentCropPercentages.first
            val originalWidthCropPercentage = currentCropPercentages.second
            imageCropPercentages =
                Pair(originalHeightCropPercentage / 2, originalWidthCropPercentage)
        }

        val cropPercentages = imageCropPercentages
        val heightCropPercent = cropPercentages.first
        val widthCropPercent = cropPercentages.second
        val (widthCrop, heightCrop) = when (0) {
            90, 270 -> Pair(heightCropPercent / 100f, widthCropPercent / 100f)
            else -> Pair(widthCropPercent / 100f, heightCropPercent / 100f)
        }

        cropRect.inset(
            (imageWidth * widthCrop / 2).toInt(),
            (imageHeight * heightCrop / 2).toInt()
        )

        val x: Int = cropRect.left
        val y: Int = cropRect.top
        val width: Int = (cropRect.right - cropRect.left)
        val height: Int = cropRect.bottom - cropRect.top

        val croppedBmp: Bitmap = Bitmap.createBitmap(
            mediaImage,
            x,
            y,
            width,
            height
        )

        process(croppedBmp, imageProxy)
    }

    private fun process(image: Bitmap, imageProxy: ImageProxy) {
        try {
            readTextFromImage(InputImage.fromBitmap(image, 0), imageProxy)
        } catch (e: IOException) {
            Log.d(TAG, "Failed to load the image")
            e.printStackTrace()
        }
    }

    private fun readTextFromImage(image: InputImage, imageProxy: ImageProxy) {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .addOnSuccessListener { visionText ->
                processTextFromImage(visionText, imageProxy)
                imageProxy.close()
            }
            .addOnFailureListener { error ->
                Log.d(TAG, "Failed to process the image")
                error.printStackTrace()
                imageProxy.close()
            }
    }

    private fun processTextFromImage(visionText: Text, imageProxy: ImageProxy) {
        textFoundListener(visionText.text)
    }
}
