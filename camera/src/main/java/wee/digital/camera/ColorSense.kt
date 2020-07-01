package wee.digital.camera

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

object ColorSense {

    init {
        //System.loadLibrary("real_sense")
    }

    private val now: Long get() = System.currentTimeMillis()

    private var t: Long = 0

    const val WIDTH = 1280

    const val HEIGHT = 720

    private const val SIZE = WIDTH * HEIGHT * 3

    private var imageObserver: Disposable? = null

    val liveData: MutableLiveData<Bitmap?> by lazy {
        MutableLiveData<Bitmap?>()
    }

    private val image: Bitmap?
        get() {
            RealSense.d("Color-------------------------")
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
        stopStream()
        nStopPipeline()
    }

    fun startStream() {
        imageObserver?.dispose()
        imageObserver = Observable
                .interval(0, 100, TimeUnit.MILLISECONDS)
                .map { image }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Bitmap>() {
                    override fun onNext(it: Bitmap) {
                        liveData.value = it
                    }

                    override fun onComplete() {
                    }

                    override fun onError(e: Throwable) {
                    }
                })
    }

    fun stopStream() {
        imageObserver?.dispose()
        liveData.postValue(null)
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

    private external fun nTryWaitForFrames(raw: ByteArray)

    private external fun nPollForFrames(raw: ByteArray)

}