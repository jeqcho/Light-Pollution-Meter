package com.chojeq.lightpollutionmeter

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import com.chojeq.lightpollutionmeter.MainActivity.Companion.TAG
import com.chojeq.lightpollutionmeter.R
import java.util.*
import kotlin.math.atan
import kotlin.math.log10

class CalibrateDoneFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = inflater.inflate(R.layout.fragment_calibrate_done, container, false)
        val args =
            com.chojeq.lightpollutionmeter.CalibrateDoneFragmentArgs.fromBundle(requireArguments())

        binding.findViewById<Button>(R.id.home_button).setOnClickListener {
            it.findNavController().navigate(R.id.action_calibrateDoneFragment_to_homeFragment)
        }

        var infoString: String

        Log.i("LPM", "results: ${args.calibrateData}")

        if (args.calibrateType == "dark") {
            activity?.getPreferences(Context.MODE_PRIVATE)!!.edit().apply {
                putInt("dark", args.calibrateData)
            }.apply()
            Toast.makeText(
                activity,
                "Dark frame calibration data is saved.",
                Toast.LENGTH_SHORT
            ).show()
            infoString = "Dark counts: ${args.calibrateData}"
        } else {
            val manager =
                requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val angularArea = calculateFOV(manager)

            val current = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"))

            fun bi(x: String): String {
                if (x.length == 1) {
                    return "0$x"
                }
                return x
            }

            val hour = current.get(Calendar.HOUR_OF_DAY)
            // round down
            var hourString = "00:00"
            if (hour >= 12) {
                hourString = "12:00"
            }

            val monthId = current.get(Calendar.MONTH)
            val months: List<String> = listOf(
                "Jan",
                "Feb",
                "Mar",
                "Apr",
                "May",
                "Jun",
                "Jul",
                "Aug",
                "Sep",
                "Oct",
                "Nov",
                "Dec"
            )
            val month = months[monthId]
            val day = bi(current.get(Calendar.DATE).toString())

            val date = current.get(Calendar.YEAR)
                .toString() + '-' + month + '-' + bi(day) + ' ' + hourString

            Log.i("LPM", "Current Date is: $date")
            val moonMag = MoonEphemeris.ephemeris[date]
            Log.i("LPM", "Mag is: $moonMag")

            val flux = args.calibrateData

            val zeroPoint = moonMag!! + 2.5 * log10(angularArea * flux)

            activity?.getPreferences(Context.MODE_PRIVATE)!!.edit().apply {
                putFloat("zeroPoint", zeroPoint.toFloat())
            }.apply()

            activity?.getPreferences(Context.MODE_PRIVATE)!!.edit().apply {
                putBoolean("zeroPointCalibrated", true)
            }.apply()

            Toast.makeText(
                activity,
                "Zero point ($zeroPoint) is saved.",
                Toast.LENGTH_SHORT
            ).show()

            infoString = "Moon magnitude: $moonMag\n" +
                    "Camera FOV (sas): $angularArea\n" +
                    "Sum: ${flux}\n" +
                    "Zero point: $zeroPoint"
        }

        binding.findViewById<TextView>(R.id.calibrateInfo).text = infoString
        
        Log.i(TAG, infoString)


        return binding
    }

    private fun calculateFOV(cManager: CameraManager): Double {
        var horizontalAngle: Double = 0.0
        var verticalAngle: Double = 0.0
        val cameraId = activity?.getPreferences(Context.MODE_PRIVATE)!!.getString("cameraId", "-1")
        val characteristics = cManager.getCameraCharacteristics(cameraId!!)
        val cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING)!!
        if (cOrientation == CameraCharacteristics.LENS_FACING_BACK) {
            val maxFocus =
                characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            val size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
            val w = size!!.width
            val h = size.height
            horizontalAngle =
                (2 * atan((w / (maxFocus!![0] * 2)).toDouble())).toFloat() / Math.PI * 180
            verticalAngle =
                (2 * atan((h / (maxFocus[0] * 2)).toDouble())).toFloat() / Math.PI * 180
            Log.i(
                TAG, "horizontalAngle : $horizontalAngle\n" +
                        "verticalAngle : $verticalAngle"
            )
        }
        return horizontalAngle * verticalAngle * 60 * 60 * 60 * 60
    }

}