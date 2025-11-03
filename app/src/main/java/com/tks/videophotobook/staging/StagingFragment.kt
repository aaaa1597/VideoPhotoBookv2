package com.tks.videophotobook.staging

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.tks.videophotobook.BuildConfig
import com.tks.videophotobook.R
import com.tks.videophotobook.Utils
import com.tks.videophotobook.databinding.FragmentStagingBinding
import com.tks.videophotobook.initAR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StagingFragment : Fragment() {
    private var _binding: FragmentStagingBinding? = null
    private val binding get() = _binding!!
    private val _viewModel: StagingViewModel by activityViewModels()
    private lateinit var _adapter: StagingLogAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _viewModel.addLogStr("onCreateView start.")
        _binding = FragmentStagingBinding.inflate(inflater, container, false)
        _viewModel.addLogStr("onCreateView end.")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _viewModel.addLogStr("onViewCreated start.")

        _viewModel.addLogStr("setting RecyclerView layout start.")
        binding.rcvLog.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
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

        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)

                /* Fragment起動アニメーション開始で呼ばれる */
                view.postOnAnimation {
                    Log.d("aaaaa", "アニメーション開始！")
                    _viewModel.addLogStr(resources.getString(R.string.animation_s))
                    /* 設定時間と同じ時間を設定する。 */
                    view.postDelayed({
                        Log.d("aaaaa", "アニメーション完了！")
                        _viewModel.addLogStr(resources.getString(R.string.animation_e))
                        binding.viwTop.background = null
                    }, resources.getInteger(R.integer.config_navAnimTime400).toLong())
                }
                return true
            }
        })

        /* C++ Callbackの設定 */
        _viewModel.addLogStr("set C++ _garnishLog start.")
        _viewModel.passToNativeBridge()
        _viewModel.addLogStr("set C++ _garnishLog end.")

        /* vuforia初期化 */
        lifecycleScope.launch {
            val ret = withContext(Dispatchers.Default) {
                delay(1000)
                _viewModel.addLogStr(resources.getString(R.string.init_vuforia_s))
                val retErr = initAR(requireActivity(), BuildConfig.LICENSE_KEY)
                _viewModel.addLogStr(resources.getString(R.string.init_vuforia_e))
                retErr
            }
            /* Vuforia初期化正常完了 */
            if(ret == 0) {
//                requireActivity().findViewById<ConstraintLayout>(R.id.main).background = null
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.stagingFragment, true)
                    .build()
                findNavController().navigate(R.id.action_stagingFragment_to_mainFragment_zoom, null, navOptions)
            }
            /* Vuforia初期化失敗 */
            else {
                val titlestr:String = resources.getString(R.string.init_vuforia_err)
                val errstr:String = Utils.getErrorMessage(requireContext(),ret)
                _viewModel.addLogStr(errstr)
                AlertDialog.Builder(requireContext())
                    .setTitle(titlestr)
                    .setMessage(errstr)
                    .setPositiveButton(R.string.ok) {
                        dialog, which ->
                            dialog.dismiss();
                            requireActivity().finish()
                    }
                    .show()
            }
        }
        _viewModel.addLogStr("onViewCreated end.")
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun collectLogListFlow(binding: FragmentStagingBinding, adapter: StagingLogAdapter) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var prevList: List<String> = emptyList()
                _viewModel.logListFlow.collect { newList ->
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
                        newList.size == prevList.size + 1 &&
                                newList.dropLast(1) == prevList -> {
                            adapter.notifyItemInserted(newList.lastIndex)
                            binding.rcvLog.smoothScrollToPosition(newList.lastIndex)
                        }
                        /* 古い削除 + 1件追加(500件上限時など) */
                        newList.size == prevList.size &&
                                newList.dropLast(1) == prevList.drop(1) -> {
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
        super.onDestroyView()
        _binding = null
    }

    companion object {
        init {
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
        holder.textView.text = _viewModel.logListFlow.value[position]
    }

    override fun getItemCount(): Int = _viewModel.logListFlow.value.size
}

