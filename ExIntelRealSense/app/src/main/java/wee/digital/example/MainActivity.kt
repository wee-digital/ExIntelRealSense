package wee.digital.example

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.activity_main.*
import wee.digital.camera.ColorSense
import wee.digital.camera.DepthSense
import wee.digital.camera.RealSense
import wee.digital.camera.replaceFragment
import wee.digital.example.detect.FaceDetectFragment
import wee.digital.example.enroll.EnrollFragment


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        onClicksInit()

        RealSense.requestPermission {
        }

        ColorSense.liveData.observe(this, Observer {
            imageViewColor.setImageBitmap(it)
        })

        DepthSense.liveData.observe(this, Observer {
            imageViewDepth.setImageBitmap(it)
        })
    }

    private fun onClicksInit() {

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


        viewColorStart.setOnClickListener {
            textViewColor.setTextColor(Color.GREEN)
            ColorSense.initPipeline()
        }
        viewColorStop.setOnClickListener {
            textViewColor.setTextColor(Color.DKGRAY)
            ColorSense.stopPipeline()
        }
        viewColorCapture.setOnClickListener {
            ColorSense.capture { imageViewColor.setImageBitmap(it) }
        }
        viewColorStream.setOnClickListener {
            ColorSense.startStream()
        }


        viewDepthStart.setOnClickListener {
            textViewDepth.setTextColor(Color.GREEN)
            DepthSense.initPipeline()
        }
        viewDepthStop.setOnClickListener {
            textViewDepth.setTextColor(Color.DKGRAY)
            DepthSense.stopPipeline()
        }
        viewDepthCapture.setOnClickListener {
            DepthSense.capture { imageViewDepth.setImageBitmap(it) }
        }
        viewDepthStream.setOnClickListener {
        }
    }

}
