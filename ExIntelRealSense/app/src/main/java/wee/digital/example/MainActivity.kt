package wee.digital.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import wee.digital.camera.RealSense
import wee.digital.camera.RealSenseControl
import wee.digital.camera.replaceFragment
import wee.digital.camera.toBitmap
import wee.digital.example.detect.FaceDetectFragment
import wee.digital.example.enroll.EnrollFragment
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RealSense.requestPermission {
        }
        RealSense.imageLiveData.observe(this, Observer {
            // imageViewCapture.setBitmap(it)
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
        viewCapture.setOnClickListener {

            val t1 = System.currentTimeMillis()
            RealSense.capture {
                it ?: return@capture

                val t2 = System.currentTimeMillis()
                val bitmap = it?.toBitmap(RealSenseControl.COLOR_WIDTH, RealSenseControl.COLOR_HEIGHT)
                //val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                //val outputStream = ByteArrayOutputStream()
                //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

                val t3 = System.currentTimeMillis()
                imageViewCapture.setImageBitmap(bitmap)
                //Glide.with(this).load(outputStream.toByteArray()).into(imageViewCapture)

                textViewTime.text =
                    """
                        ${t2 - t1} ms 
                        ${t3 - t2} ms
                    """.trimIndent()
            }
        }
        viewFrames.setOnClickListener {
            RealSense.startStream()
        }

    }


}
