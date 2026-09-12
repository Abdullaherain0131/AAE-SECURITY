package com.example.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class AaeSecurityEngine(private val context: Context? = null) {

    private var tfLiteInterpreter: Interpreter? = null
    var isModelLoaded: Boolean = false
        private set
    var loadedModelName: String = "No Model Loaded"
        private set

    var totalTrainedSamples = 0L
        private set
    var currentLoss = 0.0f
        private set

    fun loadBundledModel(appContext: Context, assetFileName: String = "singularity.tflite"): Result<String> {
        return try {
            val assetFileDescriptor = appContext.assets.openFd(assetFileName)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            tfLiteInterpreter?.close()
            tfLiteInterpreter = Interpreter(mappedByteBuffer)
            isModelLoaded = true
            loadedModelName = assetFileName
            
            Result.success("Yerleşik TFLite modeli başarıyla yüklendi: $assetFileName")
        } catch (e: Exception) {
            Log.e("AaeSecurityEngine", "Yerleşik model yüklenemedi: $assetFileName", e)
            Result.failure(e)
        }
    }

    fun loadModelFromUri(appContext: Context, uri: Uri): Result<String> {
        return try {
            val contentResolver = appContext.contentResolver
            val fileName = uri.lastPathSegment ?: "model.tflite"

            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalArgumentException("Dosya okunamadı: $uri"))

            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("Seçilen model dosyası boş."))
            }

            val byteBuffer = ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                rewind()
            }

            loadTFLiteModel(byteBuffer, fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loadTFLiteModel(buffer: ByteBuffer, modelName: String = "model.tflite"): Result<String> {
        return try {
            tfLiteInterpreter?.close()
            tfLiteInterpreter = Interpreter(buffer)
            isModelLoaded = true
            loadedModelName = modelName
            Result.success("TensorFlow Lite modeli başarıyla belleğe alındı: $modelName")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun runInference(inputData: FloatArray): Float {
        val interpreter = tfLiteInterpreter
        if (interpreter == null) {
            Log.w("AaeSecurityEngine", "Inference çağrıldı ancak model yüklü değil!")
            return 0.0f
        }

        return try {
            val input = Array(1) { inputData }
            val output = Array(1) { FloatArray(1) }
            interpreter.run(input, output)
            output[0][0]
        } catch (e: Exception) {
            Log.e("AaeSecurityEngine", "Inference hatası", e)
            0.0f
        }
    }

    fun train(input: FloatArray, isThreat: Boolean, lr: Float = 0.005f) {
        // TFLite modeli çevrimdışı ve salt okunur olduğu için anlık eğitim kaldırıldı.
        // Ancak sayaçlar tutulmaya devam edilebilir.
        totalTrainedSamples++
    }

    companion object {
        const val INPUT_SIZE = 50
        const val HIDDEN_SIZE = 24

        val instance: AaeSecurityEngine by lazy { AaeSecurityEngine() }

        fun loadBundledModel(context: Context, assetFileName: String = "singularity.tflite"): Result<String> =
            instance.loadBundledModel(context, assetFileName)

        fun loadModelFromUri(context: Context, uri: Uri): Result<String> =
            instance.loadModelFromUri(context, uri)

        fun loadTFLiteModel(buffer: ByteBuffer, modelName: String = "model.tflite"): Result<String> =
            instance.loadTFLiteModel(buffer, modelName)

        fun runInference(inputData: FloatArray): Float =
            instance.runInference(inputData)

        fun train(input: FloatArray, isThreat: Boolean, lr: Float = 0.005f) =
            instance.train(input, isThreat, lr)

        // Uyumluluk için tutulan dummy fonksiyonlar
        fun predictThreatScore(input: FloatArray): Float = runInference(input)
        fun importWeightsFromJsonOrJs(content: String): Result<String> = Result.failure(Exception("JSON weights not supported. Use TFLite."))

        val totalTrainedSamples: Long get() = instance.totalTrainedSamples
        val currentLoss: Float get() = instance.currentLoss
        val isModelLoaded: Boolean get() = instance.isModelLoaded
        var loadedModelName: String
            get() = instance.loadedModelName
            set(value) { instance.loadedModelName = value }
    }
}
