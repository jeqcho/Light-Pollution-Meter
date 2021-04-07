package com.example.lightpollutionmeter

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.findNavController
import java.util.*
import java.util.Calendar.*

class CalibrateFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root: View =  inflater.inflate(R.layout.fragment_calibrate, container, false)
        val toMeasure = CalibrateFragmentDirections.actionCalibrateFragmentToMeasureFragment()
        toMeasure.isCalibrate = true
        root.findViewById<Button>(R.id.dark_button).setOnClickListener {
            toMeasure.calibrateType = "dark"
            it.findNavController().navigate(toMeasure)
        }
        root.findViewById<Button>(R.id.moon_button).setOnClickListener {
            toMeasure.calibrateType = "moon"
            it.findNavController().navigate(toMeasure)
        }
        return root
    }

}