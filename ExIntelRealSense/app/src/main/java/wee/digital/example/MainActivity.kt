package wee.digital.example

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import wee.digital.camera.RealSense
import wee.digital.camera.argbToBitmap
import wee.digital.camera.replaceFragment
import wee.digital.camera.rgbToArgb
import wee.digital.example.detect.FaceDetectFragment
import wee.digital.example.enroll.EnrollFragment
import java.io.BufferedReader
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RealSense.requestPermission {
        }
        RealSense.imageLiveData.observe(this, Observer {
            imageViewCapture.setImageBitmap(it)
        })

        viewDebug.setOnClickListener {
            replaceFragment(FaceDetectFragment(), R.id.fragmentContainer, true)
        }
        viewEnroll.setOnClickListener {
            replaceFragment(EnrollFragment(), R.id.fragmentContainer, true)
        }
        viewAutoEnroll.setOnClickListener {
            Toast.makeText(this, "dev", Toast.LENGTH_SHORT).show()
        }
        viewMotionEnroll.setOnClickListener {
            Toast.makeText(this, "dev", Toast.LENGTH_SHORT).show()
        }
        viewCapturePortrait.setOnClickListener {
            Toast.makeText(this, "dev", Toast.LENGTH_SHORT).show()
        }

        viewStart.setOnClickListener {
            RealSense.start()
        }
        viewStop.setOnClickListener {
            RealSense.stop()
        }

        //val image = Base64.decode(readAsset("image.txt"), Base64.NO_WRAP)
        viewCapture.setOnClickListener {
            var t = System.currentTimeMillis()
            RealSense.capture {
                it ?: return@capture

                textViewTime.text = "capture: ${System.currentTimeMillis() - t} ms - size: ${it.size}"

                t = System.currentTimeMillis()
                val argb = it.rgbToArgb(1280) ?: return@capture
                textViewTime.append("\n\nrgbToArgb:  ${now - t} ms")

                t = System.currentTimeMillis()
                textViewTime.append("\n\nargbToBitmap:  ${now - t} ms")
                val bmp = argb.argbToBitmap(1280) ?: return@capture

                t = System.currentTimeMillis()
                imageViewCapture.setImageBitmap(bmp)
                textViewTime.append("\n\nsetImageBitmap:  ${now - t} ms")
            }
        }
        viewFrames.setOnClickListener {
            RealSense.startStream()
        }

    }

    private fun ImageView.load(byteArray: ByteArray?) {
        Glide.with(this).load(byteArray).into(this)
    }

    val now: Long get() = System.currentTimeMillis()

    fun readAsset(filename: String): String {
        val sb = StringBuilder()
        BufferedReader(InputStreamReader(App.instance.assets.open(filename))).useLines { lines ->
            lines.forEach {
                sb.append(it)
            }
        }
        return sb.toString()
    }


}
