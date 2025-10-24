package com.tks.videophotobook.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tks.videophotobook.R
import com.tks.videophotobook.databinding.FragmentSendImageDialogBinding

private const val ARG_URILIST = "ARG_URILIST"
class SendImageDialogFragment : DialogFragment() {
    private var _binding: FragmentSendImageDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var _uriList: ArrayList<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            _uriList = it.getParcelableArrayList(ARG_URILIST, Uri::class.java) ?: arrayListOf()
        }
    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        _binding = FragmentSendImageDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvwMarkers.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvwMarkers.adapter = SendImageDialogAdapter(_uriList)
        binding.btnOk.setOnClickListener {
            /* 共有アプリ起動Intentを作成 */
            val shareIntent = Intent().apply{
                action = Intent.ACTION_SEND_MULTIPLE
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, _uriList)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            /* 共有アプリを起動 */
            startActivity(Intent.createChooser(shareIntent, getString(R.string.send_image_via)))
            dismissAllowingStateLoss()
        }
    }

    class SendImageDialogAdapter(private val _uriList: ArrayList<Uri>): RecyclerView.Adapter<SendImageDialogAdapter.SendImageDialogHolder>() {
        class SendImageDialogHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val sendImgView: ImageView = itemView.findViewById(R.id.imv_sendImage)
        }
        override fun onCreateViewHolder(parent: ViewGroup,viewType: Int): SendImageDialogHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_marker, parent, false)
            return SendImageDialogHolder(view)
        }

        override fun onBindViewHolder(holder: SendImageDialogHolder, position: Int) {
            holder.sendImgView.setImageURI(_uriList[position])
        }

        override fun getItemCount(): Int = _uriList.size
    }

    companion object {
        @JvmStatic
        fun newInstance(uriList: ArrayList<Uri>) =
            SendImageDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_URILIST, uriList)
                }
            }
    }
}