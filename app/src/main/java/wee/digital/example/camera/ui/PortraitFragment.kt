package wee.digital.example.camera.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.portrait.*
import wee.digital.example.camera.R

class PortraitFragment private constructor() : Fragment() {

    companion object {
        fun newInstance(bitmap: Bitmap): PortraitFragment {
            return PortraitFragment().also {
                it.bitmap = bitmap
            }
        }
    }

    var bitmap: Bitmap? = null

    override fun onCreateView(li: LayoutInflater, vg: ViewGroup?, b: Bundle?): View? {
        return li.inflate(R.layout.portrait, vg, false)
    }

    override fun onViewCreated(v: View, b: Bundle?) {
        imageViewPortrait.setImageBitmap(bitmap)
    }

}