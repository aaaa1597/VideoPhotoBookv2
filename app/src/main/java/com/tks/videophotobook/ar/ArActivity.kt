package com.tks.videophotobook.ar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tks.videophotobook.EntranceActivity
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
        WindowCompat.setDecorFitsSystemWindows(window, false)

        /* フルスクリーン制御 */
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        /* ナビゲーションバー非表示 */
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        /* ステータスバー常時表示＋透明 */
        controller.show(WindowInsetsCompat.Type.statusBars())

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, StagingFragment())
                .commit()
        }

        onBackPressedDispatcher.addCallback(this) {
            finish()
            /* EntranceActivityを起動 */
            val intent = Intent(this@ArActivity, EntranceActivity::class.java)
            startActivity(intent)
        }
    }
}