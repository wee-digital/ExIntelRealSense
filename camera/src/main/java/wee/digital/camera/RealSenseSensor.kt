package wee.digital.camera

import android.graphics.Bitmap
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

abstract class RealSenseSensor {

    var isStream: Boolean = false

    abstract val image: Bitmap?

    protected abstract fun onStartPipeline(): Boolean

    protected abstract fun onStopPipeline(): Boolean

    private val imageObservable
        get() = Single
                .just(image)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())

    private var imageObserver: Disposable? = null

    val liveData: MutableLiveData<Bitmap?> = MutableLiveData<Bitmap?>()

    fun startStream() {
        imageObserver?.dispose()
        imageObserver = imageObservable
                .doOnEvent { bitmap, e ->
                    liveData.value = bitmap
                    startStream()
                }
                .subscribe()
        isStream = true
    }

    fun stopStream() {
        imageObserver?.dispose()
        liveData.postValue(null)
        isStream = false
    }

    fun capture(block: (Bitmap?) -> Unit) {
        if (imageObserver != null && !imageObserver!!.isDisposed) {
            return
        }
        imageObserver = imageObservable
                .doOnEvent { bitmap, e ->
                    block(bitmap)
                }
                .subscribe()
    }

    @Volatile
    private var isOnCommand: Boolean = false

    private var isStarted: Boolean = false

    fun startPipeline() {
        if (isOnCommand || isStarted) return
        isOnCommand = true
        isStarted = onStartPipeline()
        isOnCommand = false
    }

    fun stopPipeline() {
        if (isOnCommand || !isStarted) return
        isOnCommand = true
        stopStream()
        Handler().postDelayed({
            isStarted = onStopPipeline()
            isOnCommand = false
        }, 1000)
    }

}