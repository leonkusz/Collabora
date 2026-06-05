package com.example.collabora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// STEP 1 😄🔥: TAMBAH DELETE CALLBACK
class ChatAdapter(
    private val chatList: ArrayList<ChatMessage>,
    private val onDeleteClick: (ChatMessage) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_ME = 1
    private val VIEW_TYPE_OTHER = 2

    // =========================
    // DETERMINE CHAT TYPE
    // =========================

    override fun getItemViewType(position: Int): Int {

        val currentUserId =
            FirebaseAuth.getInstance().currentUser?.uid

        return if (
            chatList[position].senderId == currentUserId
        ) {
            VIEW_TYPE_ME
        } else {
            VIEW_TYPE_OTHER
        }
    }

    // =========================
    // CREATE VIEW HOLDER
    // =========================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return if (viewType == VIEW_TYPE_ME) {

            val view = LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_chat_me,
                    parent,
                    false
                )

            MeViewHolder(view)

        } else {

            val view = LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_chat_other,
                    parent,
                    false
                )

            OtherViewHolder(view)
        }
    }

    // =========================
    // BIND VIEW HOLDER
    // =========================

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        val chat =
            chatList[position]

        val formattedTime =
            SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(
                Date(chat.timestamp)
            )

        if (holder is MeViewHolder) {

            holder.tvMessage.text =
                chat.message

            holder.tvTime.text =
                formattedTime

            // STEP 2 😄🔥: LONG PRESS DETECTION
            holder.itemView.setOnLongClickListener {

                onDeleteClick(chat)

                true
            }

        } else if (holder is OtherViewHolder) {

            holder.tvSender.text =
                chat.senderName

            holder.tvMessage.text =
                chat.message

            holder.tvTime.text =
                formattedTime
        }
    }

    // =========================
    // ITEM COUNT
    // =========================

    override fun getItemCount(): Int {
        return chatList.size
    }

    // =========================
    // VIEW HOLDER ME
    // =========================

    class MeViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvMessage: TextView =
            itemView.findViewById(R.id.tvMessage)

        val tvTime: TextView =
            itemView.findViewById(R.id.tvTime)
    }

    // =========================
    // VIEW HOLDER OTHER
    // =========================

    class OtherViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvSender: TextView =
            itemView.findViewById(R.id.tvSender)

        val tvMessage: TextView =
            itemView.findViewById(R.id.tvMessage)

        val tvTime: TextView =
            itemView.findViewById(R.id.tvTime)
    }
}