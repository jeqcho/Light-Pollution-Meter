package com.example.lightpollutionmeter

import android.content.Context
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
import kotlin.math.log
import kotlin.math.log10

class ResultFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val binding = inflater.inflate(R.layout.fragment_result, container, false)
        val args = ResultFragmentArgs.fromBundle(requireArguments())
        val preSum = args.sum
        val dark =  activity?.getPreferences(Context.MODE_PRIVATE)!!.getInt("dark",-1)
        val zeroPoint =  activity?.getPreferences(Context.MODE_PRIVATE)!!.getFloat("zeroPoint", 30F)
        val zeroPointCalibrated =  activity?.getPreferences(Context.MODE_PRIVATE)!!.getBoolean("zeroPointCalibrated",false)
        val sum = preSum - dark
        val defZeroPoint = 32.75
        val defSurfaceBrightness = defZeroPoint - 2.5 * log10(sum.toDouble())
        val calSurfaceBrightness = zeroPoint - 2.5 * log10(sum.toDouble())
        var resultString = "Sum: $preSum\n" +
                "Sum (dark subtracted): $sum\n" +
                "Sky brightness (default ZP=$defZeroPoint)(mpsas): ${"%.2f".format(defSurfaceBrightness)}\n"

        var remindTip = ""
        if (dark == -1){
            remindTip += "Please take dark frames before taking measurements.\n"
        }
        if (!zeroPointCalibrated){
            remindTip += "Please calibrate using the Moon before taking measurements."
        } else {
            resultString += "Sky brightness (calibrated ZP=${"%.2f".format(zeroPoint)})(mpsas): ${"%.2f".format(calSurfaceBrightness)}\n"
        }
        if(remindTip != ""){
            Toast.makeText(
                activity,
                remindTip,
                Toast.LENGTH_SHORT
            ).show()
        }

        Log.i(MainActivity.TAG, resultString)
        binding.findViewById<TextView>(R.id.result_text).text = resultString
        binding.findViewById<Button>(R.id.measure_button).setOnClickListener{
            it.findNavController().navigate(R.id.action_resultFragment_to_homeFragment)
        }
        return binding
    }

}