package wee.digital.example.camera.vm

import android.graphics.Bitmap
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EnrollVM : ViewModel() {
    /**
     * An emulator network enroll request delayed 5 seconds
     * Replace with your presenter or view model or controller
     */
    var isOnRequest: Boolean = false

    private val enrollHandler: Handler = Handler()

    private var enrollRunnable: Runnable? = null

    val enrollResultLiveData = MutableLiveData<Bitmap?>()

    val progressLiveData = MutableLiveData<Boolean>()

    fun onEnrollRequest(bitmap: Bitmap) {
        if (isOnRequest) return
        isOnRequest = true
        enrollRunnable = Runnable {
            isOnRequest = false
            progressLiveData.value = false
            enrollResultLiveData.value = bitmap
        }
        progressLiveData.value = true
        enrollHandler.postDelayed(enrollRunnable, 5000)
    }

    fun disposeEnrollRequest() {
        isOnRequest = false
        progressLiveData.value = false
        enrollRunnable?.also {
            enrollHandler.removeCallbacks(it)
        }
    }
}