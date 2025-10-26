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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.tks.videophotobook.R
import com.tks.videophotobook.databinding.FragmentStagingBinding
import kotlinx.coroutines.launch

class StagingFragment : Fragment() {
    private var _binding: FragmentStagingBinding? = null
    private val binding get() = _binding!!
    private val _viewModel: StagingViewModel by activityViewModels()
    private lateinit var _adapter: StagingLogAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentStagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rcvLog.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        /* アダプター定義 */
        _adapter = StagingLogAdapter(_viewModel)
        binding.rcvLog.adapter = _adapter
        collectLogListFlow(binding, _adapter)

        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)

                /* Fragment起動アニメーション開始で呼ばれる */
                view.postOnAnimation {
                    Log.d("aaaaa", "アニメーション開始！")
                    _viewModel.addItem("アニメーション開始！")
                    /* 設定時間と同じ時間を設定する。 */
                    view.postDelayed({
                        Log.d("aaaaa", "アニメーション完了！")
                        _viewModel.addItem("アニメーション完了！")
                        _viewModel.addItem("アニメーション完了！22")
                        _viewModel.addItem("アニメーション完了！33")
                        binding.viwTop.background = null
                    }, resources.getInteger(R.integer.config_navAnimTime400).toLong())
                }
                return true
            }
        })

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

