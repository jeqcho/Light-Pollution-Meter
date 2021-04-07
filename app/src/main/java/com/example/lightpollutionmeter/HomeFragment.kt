package com.example.lightpollutionmeter

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.findNavController

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val root: View =  inflater.inflate(R.layout.fragment_home, container, false)
        root.findViewById<Button>(R.id.measure_button).setOnClickListener {
            it.findNavController().navigate(R.id.action_homeFragment_to_measureFragment)
        }
        root.findViewById<Button>(R.id.calibrate_button).setOnClickListener {
            it.findNavController().navigate(R.id.action_homeFragment_to_calibrateFragment)
        }
        root.findViewById<Button>(R.id.about_button).setOnClickListener{
            it.findNavController().navigate(R.id.action_homeFragment_to_aboutFragment)
        }
        return root
    }
}