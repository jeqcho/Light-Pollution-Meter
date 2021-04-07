package com.example.lightpollutionmeter

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraManager.AvailabilityCallback
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MeasureFragment : Fragment(), SensorEventListener {
    private var measuring: Boolean = false
    private var results: MutableList<Int> = mutableListOf()
    private var measureCount = 0

    private lateinit var binding: View
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var args: MeasureFragmentArgs

    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // For data logging
        Log.i(TAG, "ISO: $ISO")
        Log.i(TAG, "EXP: $exp_time")

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val manager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager

        manager.registerAvailabilityCallback(object : AvailabilityCallback() {
            override fun onCameraAvailable(cameraId: String) {
                super.onCameraAvailable(cameraId)

                Log.i(TAG, "available: $cameraId")
            }

            override fun onCameraUnavailable(cameraId: String) {
                super.onCameraUnavailable(cameraId)

                Log.i(TAG, "unavailable: $cameraId")

                activity?.getPreferences(Context.MODE_PRIVATE)!!.edit().apply {
                    putString("cameraId", cameraId)
                }.apply()
            }
        }, Handler(Looper.getMainLooper()))

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = inflater.inflate(R.layout.fragment_measure, container, false)
        args = MeasureFragmentArgs.fromBundle(requireArguments())
        binding.findViewById<Button>(R.id.capture_button).isClickable = true
        binding.findViewById<Button>(R.id.capture_button).text = "MEASURE"
        binding.findViewById<Button>(R.id.capture_button).setOnClickListener {
            measuring = true
            binding.findViewById<Button>(R.id.capture_button).text = "MEASURING..."
            binding.findViewById<Button>(R.id.capture_button).isClickable = false
        }

        return binding
    }

    fun analyse(image: ImageProxy): Unit {
        fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val sum = pixels.sum()
        val num = pixels.size
        val avg = pixels.average()
        val mx: Int = pixels.maxOrNull() ?: 0
        val mn: Int = pixels.minOrNull() ?: 1000

        updateOrientationAngles()

        // get dark counts from previous calibrations, or -1 if none
        val darkCounts = activity?.getPreferences(Context.MODE_PRIVATE)!!.getInt("dark", -1)

        val infoString = "Average luminosity: $avg \n" +
                "Sum: $sum \n" +
                "Max: $mx \n" +
                "Min: $mn \n" +
                "Num: $num \n" +
                "Azi: ${orientationAngles[0]} \n" +
                "Pitch: ${orientationAngles[1]} \n" +
                "Roll: ${orientationAngles[2]} \n" +
                "Dark counts: $darkCounts"

        var readingRecorded = false

        if (measuring && mx < 255) {
            readingRecorded = true
            var validPixels = 0
            if (args.isCalibrate && args.calibrateType == "moon") {
                val pixelArray = pixels.toTypedArray()
                pixelArray.sort()
                // sum up the counts of the Moon
                var moonFlux = 0
                Log.i(TAG, "SORTED")
                for (i in pixelArray.indices) {
                    if (pixelArray[i] >= 20){
                        moonFlux += pixelArray[i]
                        ++validPixels
                    }
                }
                results.add(moonFlux)
            } else {
                results.add(sum)
            }

            measureCount += 1
            Log.i(TAG, "MEASURE_COUNT $measureCount")
            if (measureCount <= 3){
                Log.i(TAG, infoString)
                Log.i(TAG, "valid pixels: $validPixels")
            }
            if (measureCount == 3) {
                measuring = false
                var result = results.average().toInt()
                if (args.isCalibrate && args.calibrateType == "moon"){
                    result = ((result / moon_exp_sec).toInt())
                } else {
                    result = (result / exp_sec)
                }

                Log.i(TAG, "DONE Measure")
                if (args.isCalibrate) {
                    binding.findNavController().navigate(
                        MeasureFragmentDirections.actionMeasureFragmentToCalibrateDoneFragment(
                            args.calibrateType,
                            result
                        )
                    )
                } else {
                    binding.findNavController().navigate(
                        MeasureFragmentDirections.actionMeasureFragmentToResultFragment(
                            result
                        )
                    )
                }
            }
        }

        requireActivity().runOnUiThread {
            // Stuff that updates the UI
            if (!args.isCalibrate && darkCounts == -1) {
                Toast.makeText(
                    activity,
                    "Please calibrate dark frames before measuring.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            if (readingRecorded){
                Toast.makeText(
                    activity,
                    "Valid readings recorded: $measureCount",
                    Toast.LENGTH_SHORT
                ).show()
            }
            binding.findViewById<TextView>(R.id.image_info).text = infoString
        }
    }

    inner class LuminosityAnalyzer : ImageAnalysis.Analyzer {

        override fun analyze(image: ImageProxy) {
            this@MeasureFragment.analyse(image)
            image.close()
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(binding.findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)

            val imageAnalyzer = ImageAnalysis.Builder().build()
            imageAnalyzer.setAnalyzer(cameraExecutor, LuminosityAnalyzer())

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

                // Turning off auto-exposure
                val camControl = Camera2CameraControl.from(camera.cameraControl)
                val captureOptions = CaptureRequestOptions.Builder()
                captureOptions.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF
                )

                // set exposure time
                if (args.isCalibrate && args.calibrateType == "moon") {
                    captureOptions.setCaptureRequestOption(
                        CaptureRequest.SENSOR_EXPOSURE_TIME,
                        moon_exp_time
                    )
                } else {
                    captureOptions.setCaptureRequestOption(
                        CaptureRequest.SENSOR_EXPOSURE_TIME,
                        exp_time
                    )
                }

                captureOptions.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, ISO)

                camControl.captureRequestOptions = captureOptions.build()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(activity))
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireActivity().baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    activity,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                requireActivity().finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).also{ accelerometer ->
                sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        } else {
            Toast.makeText(
                activity,
                "No accelerometer detected. Altitude cannot be calculated",
                Toast.LENGTH_SHORT
            ).show()
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).also{ accelerometer ->
                sensorManager.registerListener(
                    this,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        } else {
            Toast.makeText(
                activity,
                "No magnetometer detected. Altitude cannot be calculated",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
        Log.i(TAG, orientationAngles[0].toString())
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    private fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // "rotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        // "orientationAngles" now has up-to-date information.
    }


    companion object {
        private const val TAG = "LPM"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val ISO = 100
        private const val one_second = 1000000000L
        private const val exp_sec = 3
        private const val exp_time = exp_sec * one_second
        private const val moon_exp_sec = 0.5
        private const val moon_exp_time = (moon_exp_sec * one_second).toLong()
    }

}