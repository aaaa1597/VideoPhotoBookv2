package com.tks.videophotobook.ar

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tks.videophotobook.BuildConfig
import com.tks.videophotobook.R
import com.tks.videophotobook.Utils
import com.tks.videophotobook.databinding.FragmentStagingBinding
import com.tks.videophotobook.initAR
import com.tks.videophotobook.startAR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class StagingFragment : Fragment() {
    private var _binding: FragmentStagingBinding? = null
    private val binding get() = _binding!!
    private val _viewModel: StagingViewModel by activityViewModels()
    private lateinit var _adapter: StagingLogAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.v("aaaaa", "0-1. StagingFragment::onCreateView()")
        _viewModel.addLogStr("onCreateView start.")
        _binding = FragmentStagingBinding.inflate(inflater, container, false)
        _viewModel.addLogStr("onCreateView end.")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v("aaaaa", "0-2. StagingFragment::onViewCreated()")
        super.onViewCreated(view, savedInstanceState)
        _viewModel.addLogStr("onViewCreated start.")

        _viewModel.addLogStr("setting RecyclerView layout start.")
        binding.rcvLog.layoutManager = LinearLayoutManager(requireContext())
        _viewModel.addLogStr("setting RecyclerView layout end.")
        /* アダプター定義 */
        _viewModel.addLogStr("set RecyclerView adapter start.")
        _adapter = StagingLogAdapter(_viewModel)
        _viewModel.addLogStr("set RecyclerView adapter end.")
        _viewModel.addLogStr("set RecyclerView adapter start.")
        binding.rcvLog.adapter = _adapter
        _viewModel.addLogStr("set RecyclerView adapter end.")
        _viewModel.addLogStr("collect Log start.")
        collectLogListFlow(binding, _adapter)
        _viewModel.addLogStr("collect Log end.")

//        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
//            override fun onPreDraw(): Boolean {
//                view.viewTreeObserver.removeOnPreDrawListener(this)
//
//                /* Fragment起動アニメーション開始で呼ばれる */
//                view.postOnAnimation {
//                    Log.d("aaaaa", "アニメーション開始！")
//                    _viewModel.addLogStr(resources.getString(R.string.animation_s))
//                    /* 設定時間と同じ時間を設定する。 */
//                    view.postDelayed({
//                        Log.d("aaaaa", "アニメーション完了！")
//                        _viewModel.addLogStr(resources.getString(R.string.animation_e))
//                        binding.viwTop.background = null
//                    }, resources.getInteger(R.integer.config_navAnimTime400).toLong())
//                }
//                return true
//            }
//        })

        /* C++ Callbackの設定 */
        _viewModel.addLogStr("set C++ garnishLog start.")
        _viewModel.passToNativeBridge()
        _viewModel.addLogStr("set C++ garnishLog end.")

        lifecycleScope.launch {
            /* C++側initAR()を実行 */
            val retInitAR = async(Dispatchers.Default) {
                delay(1000)
                _viewModel.addLogStr(resources.getString(R.string.init_vuforia_s))
                val retErr = initAR(requireActivity(), BuildConfig.LICENSE_KEY)
                _viewModel.addLogStr(resources.getString(R.string.init_vuforia_e))
                retErr
            }.await()
            /* Vuforia初期化失敗 */
            if(retInitAR != 0) {
                val titlestr= resources.getString(R.string.init_vuforia_err)
                val errstr  = Utils.getErrorMessage(requireContext(),retInitAR)
                _viewModel.addLogStr(errstr)
                AlertDialog.Builder(requireContext())
                    .setTitle(titlestr)
                    .setMessage(errstr)
                    .setPositiveButton(R.string.ok) {
                        dialog, which ->
                            dialog.dismiss()
                            throw RuntimeException(errstr)
                    }
                    .show()
            }

            /* pause.pngテクスチャ読込み */
            async(Dispatchers.IO) {
                delay(500)
                _viewModel.addLogStr("read  pause.png texture image start.")
                val pausebitmap = BitmapFactory.decodeResource(requireContext().resources, R.drawable.pause)
                _viewModel.addLogStr("read pause.png texture image end.")
                delay(500)
                _viewModel.addLogStr("change bitmap to ByteBuffer start.")
                val pauseTexture: ByteBuffer = pausebitmap.let { bitmap ->
                    ByteBuffer.allocateDirect(bitmap.byteCount).apply {
                        bitmap.copyPixelsToBuffer(this)
                        rewind()
                        _viewModel.addLogStr("change bitmap to ByteBuffer end.")
                    }
                }
                _viewModel.setPauseTexture(pauseTexture, pausebitmap.width, pausebitmap.height)
            }.await()

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MainFragment())
                .commit()
        }
        _viewModel.addLogStr("onViewCreated end.")
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun collectLogListFlow(binding: FragmentStagingBinding, adapter: StagingLogAdapter) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var prevList: List<String> = emptyList()
                _viewModel.garnishLogListFlow.collect { newList ->
                    when {
                        /* 初回描画 */
                        prevList.isEmpty() && newList.isNotEmpty() -> {
                            adapter.notifyDataSetChanged()
                            binding.rcvLog.smoothScrollToPosition(newList.size - 1)
                        }
                        /* 全削除 */
                        newList.isEmpty() -> {
                            adapter.notifyDataSetChanged()
                        }
                        /* 追加だけ(古い削除なし) */
                        newList.size == prevList.size + 1 && newList.dropLast(1) == prevList -> {
                            adapter.notifyItemInserted(newList.lastIndex)
                            binding.rcvLog.smoothScrollToPosition(newList.lastIndex)
                        }
                        /* 古い削除 + 1件追加(500件上限時など) */
                        newList.size == prevList.size && newList.dropLast(1) == prevList.drop(1) -> {
                            adapter.notifyItemRemoved(0)    /* 先頭を消す */
                            adapter.notifyItemInserted(newList.lastIndex) /* 新しい行を追加 */
                            binding.rcvLog.smoothScrollToPosition(newList.lastIndex)
                        }
                        /* それ以外は大きく変化(clearやbulk変更など) */
                        else -> {
                            adapter.notifyDataSetChanged()
                            binding.rcvLog.smoothScrollToPosition(newList.size - 1)
                        }
                    }
                    prevList = newList
                }
            }
        }
    }

    override fun onDestroyView() {
        Log.d("aaaaa", "0-3. StagingFragment::onDestroyView()")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        init {
            Log.v("aaaaa", "0-0. StagingFragment::init()")
            System.loadLibrary("videophotobook")
        }
    }
}

class StagingLogAdapter(
    private val _viewModel: StagingViewModel
) : RecyclerView.Adapter<StagingLogAdapter.StagingLogViewHolder>() {
    class StagingLogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.txt_log_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StagingLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_logtxt, parent, false)
        return StagingLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: StagingLogViewHolder, position: Int) {
        holder.textView.text = _viewModel.garnishLogListFlow.value[position]
    }

    override fun getItemCount(): Int = _viewModel.garnishLogListFlow.value.size
}

