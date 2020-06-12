package wee.digital.camera

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction


fun debug(s: Any?) {
    if (BuildConfig.DEBUG) Log.d("Camera", s.toString())
}

val uiHandler: Handler get() = Handler(Looper.getMainLooper())

val isOnUiThread: Boolean get() = Looper.myLooper() == Looper.getMainLooper()

fun uiThread(block: () -> Unit) {
    if (isOnUiThread) block()
    else uiHandler.post { block() }
}

fun uiThread(delay: Long, block: () -> Unit) {
    uiHandler.postDelayed({ block() }, delay)
}

fun FragmentActivity?.addFragment(
        fragment: Fragment, @IdRes container: Int,
        backStack: Boolean = true,
        animations: IntArray? = null
) {
    this ?: return
    val tag = fragment::class.java.simpleName
    supportFragmentManager.scheduleTransaction({
        add(container, fragment, tag)
        if (backStack) addToBackStack(tag)
    }, animations)
}

fun FragmentActivity?.replaceFragment(
        fragment: Fragment, @IdRes container: Int,
        backStack: Boolean = true,
        animations: IntArray? = null
) {
    this ?: return
    val tag = fragment::class.java.simpleName
    supportFragmentManager.scheduleTransaction({
        replace(container, fragment, tag)
        if (backStack) addToBackStack(tag)
    }, animations)
}

fun FragmentActivity?.isExist(cls: Class<*>): Boolean {
    this ?: return false
    val tag = cls.simpleName
    val fragment = supportFragmentManager.findFragmentByTag(tag)
    return null != fragment
}

fun FragmentActivity?.isNotExist(cls: Class<*>): Boolean {
    this ?: return false
    val tag = cls.simpleName
    val fragment = supportFragmentManager.findFragmentByTag(tag)
    return null == fragment
}

fun FragmentActivity?.remove(cls: Class<*>, animations: IntArray? = null) {
    remove(cls.simpleName, animations)
}

fun FragmentActivity?.remove(tag: String?, animations: IntArray? = null) {
    this ?: return
    tag ?: return
    val fragment = supportFragmentManager.findFragmentByTag(tag) ?: return
    supportFragmentManager.scheduleTransaction({
        remove(fragment)
    }, animations)
}

fun FragmentManager.scheduleTransaction(
        block: FragmentTransaction.() -> Unit,
        animations: IntArray? = null
) {

    val transaction = beginTransaction()
    if (null != animations) transaction.setCustomAnimations(
            animations[0],
            animations[1],
            animations[2],
            animations[3]
    )
    transaction.block()
    transaction.commitAllowingStateLoss()

}

