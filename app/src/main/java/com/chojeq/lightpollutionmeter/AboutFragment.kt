package com.chojeq.lightpollutionmeter

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.findNavController
import com.chojeq.lightpollutionmeter.R

class AboutFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = inflater.inflate(R.layout.fragment_about, container, false)
        binding.findViewById<Button>(R.id.back_home_button).setOnClickListener{
            it.findNavController().navigate(R.id.action_aboutFragment_to_homeFragment)
        }
        return binding
    }
}