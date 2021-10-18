package com.chojeq.lightpollutionmeter

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = inflater.inflate(R.layout.fragment_about, container, false)
        binding.findViewById<Button>(R.id.back_home_button).setOnClickListener{
            it.findNavController().navigate(R.id.action_aboutFragment_to_homeFragment)
        }
        binding.findViewById<TextView>(R.id.github_link).movementMethod = LinkMovementMethod.getInstance()
        binding.findViewById<TextView>(R.id.paper_link).movementMethod = LinkMovementMethod.getInstance()
        return binding
    }
}