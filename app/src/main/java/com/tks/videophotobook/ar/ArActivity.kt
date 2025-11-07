package com.tks.videophotobook.ar

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tks.videophotobook.R
import com.tks.videophotobook.databinding.ActivityArBinding
import com.tks.videophotobook.databinding.FragmentGatherpointBinding

class ArActivity : AppCompatActivity() {
    private lateinit var _binding: ActivityArBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _binding = ActivityArBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StagingFragment())
                .commit()
        }
    }
}