package wee.digital.example.member

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.portrait.*
import wee.digital.example.R

class PortraitFragment private constructor() : Fragment() {

    companion object {
        fun newInstance(bitmap: Bitmap): PortraitFragment {
            return PortraitFragment().also {
                it.bitmap = bitmap
            }
        }
    }

    var bitmap: Bitmap? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.portrait, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        imageViewPortrait.setImageBitmap(bitmap)
    }

}