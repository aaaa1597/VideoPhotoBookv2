package com.tks.videophotobook.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.tks.videophotobook.R
import com.tks.videophotobook.databinding.FragmentPaidBinding
import kotlin.getValue

class PaidFragment : Fragment() {
    private lateinit var _binding: FragmentPaidBinding
    private val _viewModel: SettingViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        _binding = FragmentPaidBinding.inflate(inflater, container, false)
        return _binding.root
    }
}