package com.egy1best

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.lagradost.cloudstream3.R

class BlankFragment : DialogFragment() {
    constructor() : super()
    constructor(plugin: Egy1BestPlugin) : super()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_blank, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    companion object {
        fun newInstance(plugin: Egy1BestPlugin): BlankFragment {
            return BlankFragment().apply {
                arguments = Bundle().apply {
                }
            }
        }
    }
}