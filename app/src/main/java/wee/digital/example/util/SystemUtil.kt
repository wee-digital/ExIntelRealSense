package wee.digital.example.util

import android.os.Build
import com.zed.zedlib.GpioHelper

object SystemUtil {

    private const val GPIO = "ZEDIO5"

    val deviceName: String
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                model.capitalize()
            } else manufacturer.capitalize() + " " + model
        }

    val isWeeB22: Boolean get() = deviceName == "Rockchip Wee-B22"

    fun ledOn() {
        if (isWeeB22) try {
            GpioHelper.zedSetGpioValue(GPIO, 1)
        } catch (e: Throwable) {
        }
    }

    fun ledOff() {
        if (isWeeB22) try {
            GpioHelper.zedSetGpioValue(GPIO, 0)
        } catch (e: Throwable) {
        }
    }

}