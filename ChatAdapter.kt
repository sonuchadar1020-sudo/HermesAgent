package com.hermes.agent.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hermes.agent.R

// ══════════════════════════════════════════════════════════════
//  ChatAdapter + ChatMessage — Chat UI
// ══════════════════════════════════════════════════════════════

data class ChatMessage(
    val text: String,
    val type: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_USER = 0
        const val TYPE_AGENT = 1
        const val TYPE_SYSTEM = 2
    }
}

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    override fun getItemViewType(position: Int) = messages[position].type

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ChatMessage.TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_message_user, parent, false)
                UserViewHolder(view)
            }
            ChatMessage.TYPE_AGENT -> {
                val view = inflater.inflate(R.layout.item_message_agent, parent, false)
                AgentViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_message_system, parent, false)
                SystemViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is UserViewHolder -> holder.tvMessage.text = msg.text
            is AgentViewHolder -> holder.tvMessage.text = msg.text
            is SystemViewHolder -> holder.tvMessage.text = msg.text
        }
    }

    override fun getItemCount() = messages.size

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
    }

    class AgentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
    }

    class SystemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
    }
}
