package com.example.matchingproto

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private val myDataset: MutableList<ChatData>,
    private val context: Context,
    private var myNickName: String
) : RecyclerView.Adapter<ChatAdapter.MyViewHolder>() {

    class MyViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val TextView_nickname: TextView = v.findViewById(R.id.TextView_nickname)
        val TextView_msg: TextView = v.findViewById(R.id.TextView_msg)
        val rootView: View = v
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_chat, parent, false) as LinearLayout

        return MyViewHolder(v)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val chat = myDataset[position]

        holder.TextView_nickname.text = chat.nickname
        holder.TextView_msg.text = chat.msg

        if (chat.nickname == myNickName) {
            holder.TextView_msg.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
            holder.TextView_nickname.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
        } else {
            holder.TextView_msg.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            holder.TextView_nickname.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        }
    }

    override fun getItemCount(): Int {
        return myDataset.size
    }

    fun getChat(position: Int): ChatData? {
        return if (position in 0 until myDataset.size) myDataset[position] else null
    }

    fun addChat(chat: ChatData) {
        myDataset.add(chat)
        notifyItemInserted(myDataset.size - 1)
    }
}