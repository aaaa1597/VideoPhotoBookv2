package com.tks.videophotobook.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.tks.videophotobook.R
import com.tks.videophotobook.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewPager.adapter = SettingPagerAdapter(requireActivity())

        /* TabLayoutとViewPager2を連携 */
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.free)
                1 -> getString(R.string.paid)
                else -> ""
            }
        }.attach()

        viewModel.initMarkerVideoSetList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SettingPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    private val _fragments = listOf(FreeFragment(), PaidFragment())
    override fun getItemCount(): Int = _fragments.size
    override fun createFragment(position: Int): Fragment {
        return _fragments[position]
    }
}
