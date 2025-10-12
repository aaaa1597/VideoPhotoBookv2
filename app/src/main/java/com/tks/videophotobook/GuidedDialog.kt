package com.tks.videophotobook

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tks.videophotobook.databinding.GuidedDialogBinding

class GuidedDialog : DialogFragment() {
    private var _binding: GuidedDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        _binding = GuidedDialogBinding.inflate(inflater, null, false)

        binding.dialogTitle.text = getString(R.string.no_data)
        binding.dialogMessage.text = getString(R.string.no_bind_data)

        binding.buttonPositive.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.action_mainFragment_to_settingFragment_slide)
        }
        binding.buttonNegative.visibility = View.GONE

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setCancelable(false)

        return builder.create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}