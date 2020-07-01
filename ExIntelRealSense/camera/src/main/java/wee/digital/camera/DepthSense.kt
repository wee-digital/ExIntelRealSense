package wee.digital.camera

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers

object DepthSense {

    init {
        System.loadLibrary("real_sense")
    }

    private val now: Long get() = System.currentTimeMillis()

    private var t: Long = 0

    private const val WIDTH = 640

    private const val HEIGHT = 480

    private const val SIZE = WIDTH * HEIGHT * 3

    private var imageObserver: Disposable? = null

    val liveData: MutableLiveData<Bitmap?> by lazy {
        MutableLiveData<Bitmap?>()
    }

    private val image: Bitmap?
        get() {
            RealSense.d("Depth-------------------------")
            t = System.currentTimeMillis()
            val raw = ByteArray(SIZE)
            nWaitForFrame(raw)
            RealSense.d("capture: ${System.currentTimeMillis() - t} ms")
            t = System.currentTimeMillis()
            val argb = raw.rgbToArgb(WIDTH) ?: return null
            RealSense.d("rgbToArgb: ${now - t} ms")
            t = System.currentTimeMillis()
            RealSense.d("argbToBitmap: ${now - t} ms")
            return argb.argbToBitmap(WIDTH)
        }

    fun initPipeline() {
        nStartPipeline()
    }

    fun stopPipeline() {
        nStopPipeline()
    }

    fun startStream() {
    }

    fun stopStream() {
    }

    fun capture(block: (Bitmap?) -> Unit) {
        Observable
                .fromCallable { image }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Bitmap>() {
                    override fun onNext(it: Bitmap) {
                        block(it)
                    }

                    override fun onComplete() {
                    }

                    override fun onError(e: Throwable) {
                    }
                })
    }

    /**
     * Native
     */
    private external fun nStartPipeline()

    private external fun nStopPipeline()

    private external fun nWaitForFrame(raw: ByteArray)

}