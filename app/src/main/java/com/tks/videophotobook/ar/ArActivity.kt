package com.tks.videophotobook.ar

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
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

        /* コンテンツをSystemBars（Status/Navigation）下まで広げる */
        window.setDecorFitsSystemWindows(false)
        /* ステータスバー透明 */
        window.statusBarColor = Color.TRANSPARENT

        /* フルスクリーン制御 */
        val controller = window.insetsController ?: return
        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        /* ナビゲーションバー非表示 */
        controller.hide(WindowInsets.Type.navigationBars())
        /* ステータスバー常時表示＋透明 */
        controller.show(WindowInsets.Type.statusBars())

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StagingFragment())
                .commit()
        }
    }
}