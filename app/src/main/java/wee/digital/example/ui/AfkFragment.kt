package wee.digital.example.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import wee.digital.camera.addFragment
import wee.digital.camera.remove
import wee.digital.example.R

class AfkFragment private constructor() : Fragment() {

    companion object {

        private var isShown: Boolean = false

        fun show(activity: FragmentActivity?) {
            activity ?: return
            if (isShown) return
            isShown = true
            val fragment = AfkFragment()
            activity.addFragment(fragment, R.id.fragmentContainer, true)
        }

        fun dismiss(activity: FragmentActivity?) {
            activity ?: return
            if (!isShown) return
            isShown = false
            activity.remove(AfkFragment::class.java)
        }
    }

    override fun onCreateView(li: LayoutInflater, vg: ViewGroup?, b: Bundle?): View? {
        return li.inflate(R.layout.afk, vg, false)
    }
}