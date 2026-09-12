package com.example.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite çıkarım motoru.
 *
 * ## Neden eğitim yok
 *
 * Bu sınıfta eskiden bir `train(input, isThreat, lr)` fonksiyonu vardı. Gövdesi
 * yalnızca `totalTrainedSamples++` idi — TFLite modeli salt okunur olduğu için
 * gerçek bir güncelleme yapılmıyordu. Buna rağmen [RealTimeProtectionService]
 * onu saniyede onlarca kez rastgele verilerle çağırıyor, arayüz de `currentLoss`
 * (her zaman 0.0f) üzerinden "%100.0 doğruluk" gösteriyordu. Üçü birlikte
 * ölçülmemiş bir başarı iddiasıydı; kaldırıldı.
 *
 * Cihaz üstü öğrenme gerçekten istenirse doğru araç TFLite'ın *On-Device Training*
 * imzalarıdır (`tf.lite.Interpreter.runSignature` ile `train`/`save` imzaları içeren
 * bir model). Bu, modelin eğitilebilir olarak dışa aktarılmasını gerektirir.
 *
 * ## Girdi sözleşmesi
 *
 * Gömülü model 50 → 24 → 1 boyutlu bir MLP. Ancak **50 girdinin hangi özelliğe
 * karşılık geldiği** projede hiçbir yerde tanımlı değil; modelin eğitildiği
 * özellik sırası bilinmiyor. [PackageFeatureExtractor] bu boşluğu doldurmak için
 * açıkça belgelenmiş bir özellik şeması tanımlar. Model o şemaya göre yeniden
 * eğitilene kadar çıkarım sonucu anlamlı olmayacağından, tespit kararlarına
 * dahil edilmez — bkz. [isInferenceTrustworthy].
 */
class AaeSecurityEngine(private val context: Context? = null) {

    private var tfLiteInterpreter: Interpreter? = null

    var isModelLoaded: Boolean = false
        private set

    var loadedModelName: String = "Model Yüklenmedi"
        private set

    /** Yüklü modelin beklediği girdi uzunluğu (ör. 50). Model yoksa 0. */
    var inputLength: Int = 0
        private set

    /** Yüklü modelin ürettiği çıktı uzunluğu (ör. 1). Model yoksa 0. */
    var outputLength: Int = 0
        private set

    /**
     * Modelin çıktısı tespit kararında kullanılabilir mi?
     *
     * Yalnızca model, [PackageFeatureExtractor.SPEC_VERSION] şemasıyla aynı girdi
     * boyutunu bildirdiğinde ve tek bir olasılık üretttiğinde true olur. Şu anda
     * gömülü model bu şemaya göre eğitilmediği için varsayılan olarak false'tur:
     * uydurma bir skoru gerçek bir tehdit gerekçesi gibi göstermemek için.
     */
    val isInferenceTrustworthy: Boolean
        get() = isModelLoaded &&
            inputLength == PackageFeatureExtractor.FEATURE_COUNT &&
            outputLength == 1 &&
            declaredSpecVersion == PackageFeatureExtractor.SPEC_VERSION

    /**
     * Modelin hangi özellik şemasına göre eğitildiği. Model dosyası bunu
     * bildirmediği sürece 0 kalır ve çıkarım güvenilmez sayılır.
     * Yeniden eğitim yapılırken model adına `-fsN` soneki eklenerek bildirilir
     * (ör. `singularity-fs1.tflite`).
     */
    var declaredSpecVersion: Int = 0
        private set

    fun loadBundledModel(appContext: Context, assetFileName: String = "singularity.tflite"): Result<String> {
        return try {
            val fd = appContext.assets.openFd(assetFileName)
            val mapped = FileInputStream(fd.fileDescriptor).use { stream ->
                stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
            applyInterpreter(Interpreter(mapped), assetFileName)
            Result.success("Yerleşik TFLite modeli yüklendi: $assetFileName ($inputLength→$outputLength)")
        } catch (e: Exception) {
            Log.e(TAG, "Yerleşik model yüklenemedi: $assetFileName", e)
            Result.failure(e)
        }
    }

    fun loadModelFromUri(appContext: Context, uri: Uri): Result<String> {
        return try {
            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "model.tflite"
            val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalArgumentException("Dosya okunamadı: $uri"))
            if (bytes.isEmpty()) {
                return Result.failure(IllegalArgumentException("Seçilen model dosyası boş."))
            }

            val buffer = ByteBuffer.allocateDirect(bytes.size).apply {
                order(ByteOrder.nativeOrder())
                put(bytes)
                rewind()
            }
            loadTFLiteModel(buffer, fileName).map { message ->
                val sizeMb = bytes.size / (1024.0 * 1024.0)
                "$message • ${String.format(java.util.Locale.US, "%.2f", sizeMb)} MB"
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loadTFLiteModel(buffer: ByteBuffer, modelName: String = "model.tflite"): Result<String> {
        return try {
            applyInterpreter(Interpreter(buffer), modelName)
            Result.success("TFLite modeli belleğe alındı: $modelName ($inputLength→$outputLength)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Yeni yorumlayıcıyı devreye alır ve girdi/çıktı biçimini modelin kendisinden okur.
     * Şekil bilgisini varsaymak yerine sormak, uyumsuz bir modelin çalışma anında
     * çökmesi yerine yüklenirken tespit edilmesini sağlar.
     */
    private fun applyInterpreter(interpreter: Interpreter, modelName: String) {
        tfLiteInterpreter?.close()
        tfLiteInterpreter = interpreter
        isModelLoaded = true
        loadedModelName = modelName
        inputLength = interpreter.getInputTensor(0).shape().lastOrNull() ?: 0
        outputLength = interpreter.getOutputTensor(0).shape().lastOrNull() ?: 0
        declaredSpecVersion = SPEC_SUFFIX.find(modelName)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }

    /**
     * Modeli çalıştırır ve 0..1 aralığında bir skor döner.
     *
     * @return model yoksa, girdi uzunluğu uymuyorsa veya çıkarım hata verirse null.
     *         Hata durumunda 0.0f dönmek "tehdit yok" anlamına gelir ve sessizce
     *         yanlış bir güvence verir; bu yüzden null tercih edildi.
     */
    fun runInference(inputData: FloatArray): Float? {
        val interpreter = tfLiteInterpreter ?: return null
        if (inputLength > 0 && inputData.size != inputLength) {
            Log.w(TAG, "Girdi uzunluğu uyuşmuyor: model $inputLength bekliyor, ${inputData.size} verildi")
            return null
        }
        return try {
            val output = Array(1) { FloatArray(if (outputLength > 0) outputLength else 1) }
            interpreter.run(Array(1) { inputData }, output)
            output[0][0]
        } catch (e: Exception) {
            Log.e(TAG, "Çıkarım hatası", e)
            null
        }
    }

    fun close() {
        tfLiteInterpreter?.close()
        tfLiteInterpreter = null
        isModelLoaded = false
        inputLength = 0
        outputLength = 0
        declaredSpecVersion = 0
        loadedModelName = "Model Yüklenmedi"
    }

    companion object {
        private const val TAG = "AaeSecurityEngine"

        /** Model adında özellik şeması sürümünü bildiren sonek: `...-fs1.tflite`. */
        private val SPEC_SUFFIX = Regex("-fs(\\d+)")

        val instance: AaeSecurityEngine by lazy { AaeSecurityEngine() }

        fun loadBundledModel(context: Context, assetFileName: String = "singularity.tflite"): Result<String> =
            instance.loadBundledModel(context, assetFileName)

        fun loadModelFromUri(context: Context, uri: Uri): Result<String> =
            instance.loadModelFromUri(context, uri)

        fun loadTFLiteModel(buffer: ByteBuffer, modelName: String = "model.tflite"): Result<String> =
            instance.loadTFLiteModel(buffer, modelName)

        fun runInference(inputData: FloatArray): Float? = instance.runInference(inputData)

        val isModelLoaded: Boolean get() = instance.isModelLoaded
        val loadedModelName: String get() = instance.loadedModelName
        val inputLength: Int get() = instance.inputLength
        val outputLength: Int get() = instance.outputLength
        val isInferenceTrustworthy: Boolean get() = instance.isInferenceTrustworthy
    }
}
