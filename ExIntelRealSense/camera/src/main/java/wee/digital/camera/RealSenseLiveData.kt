package wee.digital.camera

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData

class RealSenseLiveData : MutableLiveData<Bitmap?>() {

    companion object {
        val instance: RealSenseLiveData by lazy {
            RealSenseLiveData()
        }
    }
}