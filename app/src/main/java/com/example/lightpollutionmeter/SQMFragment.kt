package com.example.lightpollutionmeter

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.findNavController
class SQMFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root: View =  inflater.inflate(R.layout.fragment_s_q_m, container, false)
        root.findViewById<Button>(R.id.continue_button).setOnClickListener {
            it.findNavController().navigate(SQMFragmentDirections.actionSQMFragmentToMeasureFragment())
        }
        return root
    }

}