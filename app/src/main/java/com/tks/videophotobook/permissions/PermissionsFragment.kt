package com.tks.videophotobook.permissions

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.tks.videophotobook.databinding.FragmentPermissionsBinding
import com.tks.videophotobook.R

val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.INTERNET)

class PermissionsFragment : Fragment() {
    private lateinit var _binding: FragmentPermissionsBinding
    private var hasRequestedPermissions = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* 権限承認済ならメイン処理のFragmentへ(移行このFragmentには戻らない) */
        if (checkPermissionsGranted()) {
            findNavController().navigate(R.id.action_permissionsFragment_to_mainFragment_fade1, null,
                NavOptions.Builder().setPopUpTo(R.id.permissionsFragment, true).build()/* 戻る必要がない */)
        }
        else if( !hasRequestedPermissions) {
            hasRequestedPermissions = true
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun checkPermissionsGranted(): Boolean {
        var result = true
        for (permission in REQUIRED_PERMISSIONS) {
            result = result && ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }
        return result
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            isGranted: Map<String, Boolean> ->
        /* 権限チェック */
        if (isGranted.isNotEmpty() && isGranted.all {it.value == true}) {
            /* 権限承認済ならメイン処理のFragmentへ(移行このFragmentには戻らない) */
            findNavController().navigate(R.id.action_permissionsFragment_to_mainFragment_fade1, null,
                NavOptions.Builder().setPopUpTo(R.id.permissionsFragment, true).build()/* 戻る必要がない */)
        }
        else {
            /* ひとつでも権限不足ありならアラートダイアログ→Shutdown */
            PermissionDialogFragment.show(requireActivity())
        }
    }
}

class PermissionDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()


        val msgstr = activity.getString(R.string.wording_permission) +
                REQUIRED_PERMISSIONS.joinToString(separator = ",\n") +
                activity.getString(R.string.wording_permission2)

        return AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.req_permission))
            .setMessage(msgstr)
            .setPositiveButton("OK") { _, _ ->
                activity.finish()
            }
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        requireActivity().finish()
    }

    companion object {
        fun show(activity: FragmentActivity) {
            val fragment = PermissionDialogFragment()
            fragment.show(activity.supportFragmentManager, "Permission request")
        }
    }
}
