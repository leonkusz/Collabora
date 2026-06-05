package com.example.collabora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
// STEP 4 😄🔥: IMPORT
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.AdapterView

class SubTaskAdapter(

    private val subTaskList: ArrayList<SubTask>,
    var isTaskLocked: Boolean = false, // Status tugas utama
    private val currentUserId: String, // ID user yang sedang login 😄🔥
    private val onSubTaskClick: (SubTask) -> Unit,
    private val onAttachmentClick: (SubTask) -> Unit // Untuk update link 😄🔥

) : RecyclerView.Adapter<SubTaskAdapter.SubTaskViewHolder>() {

    class SubTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        // Tambahan wajib agar code kamu tidak error 😄🔥
        val tvSubTaskText: TextView = itemView.findViewById(R.id.tvSubTaskText)
        val tvAssignedTo: TextView = itemView.findViewById(R.id.tvAssignedTo)
        val spinnerSubTaskStatus: Spinner = itemView.findViewById(R.id.spinnerSubTaskStatus)
        val tvSubTaskAttachment: TextView = itemView.findViewById(R.id.tvSubTaskAttachment)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubTaskViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_sub_task,
                parent,
                false
            )

        return SubTaskViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SubTaskViewHolder,
        position: Int
    ) {

        val subTask = subTaskList[position]

        // CORE LOGIC STRIKE-THROUGH
        holder.tvSubTaskText.text = subTask.subTaskText

        if (subTask.status == "Selesai") {

            holder.itemView.alpha = 0.5f

            holder.tvSubTaskText.paintFlags =
                holder.tvSubTaskText.paintFlags or
                        android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

        } else {

            holder.itemView.alpha = 1.0f

            holder.tvSubTaskText.paintFlags =
                holder.tvSubTaskText.paintFlags and
                        android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.tvAssignedTo.text = "👤 ${subTask.assignedToName}"

        // ATTACHMENT LOGIC 😄🔥
        if (subTask.attachmentLink.isNotEmpty()) {
            holder.tvSubTaskAttachment.text = "🔗 Lihat Lampiran"
            holder.tvSubTaskAttachment.setTextColor(android.graphics.Color.parseColor("#1E9E75"))
        } else {
            holder.tvSubTaskAttachment.text = "📎 Tambah Link Lampiran"
            holder.tvSubTaskAttachment.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
        }

        holder.tvSubTaskAttachment.setOnClickListener {
            // Jika ditugaskan ke saya DAN tugas belum selesai -> Bisa Edit 😄🔥
            if (subTask.assignedToId == currentUserId && !isTaskLocked && subTask.status != "Selesai") {
                onAttachmentClick(subTask)
            } else if (subTask.attachmentLink.isNotEmpty()) {
                // Selain itu, jika ada link -> Cuma bisa buka 😄🔥
                val browserIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(subTask.attachmentLink))
                holder.itemView.context.startActivity(browserIntent)
            }
        }

        // ==========================================
        // STEP 5 😄🔥: SET STATUS DROPDOWN
        // ==========================================
        val statusList = listOf(
            "Menunggu",
            "Proses",
            "Selesai"
        )

        val spinnerAdapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_dropdown_item,
            statusList
        )

        holder.spinnerSubTaskStatus.adapter = spinnerAdapter

        val currentPosition = statusList.indexOf(subTask.status)
        holder.spinnerSubTaskStatus.setSelection(currentPosition)
        
        // LOCK SYSTEM 😄🔥: 
        // 1. Jika status sub-tugas sudah "Selesai" -> Kunci.
        // 2. Jika tugas utama sudah "Selesai" (isTaskLocked) -> Kunci semua sub-tugas.
        if (subTask.status == "Selesai" || isTaskLocked) {
            holder.spinnerSubTaskStatus.isEnabled = false
        } else {
            holder.spinnerSubTaskStatus.isEnabled = true
        }

        // ==========================================
        // STEP 6 😄🔥: HANDLE UPDATE STATUS
        // ==========================================
        holder.spinnerSubTaskStatus.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    val selectedStatus = statusList[position]

                    if (selectedStatus != subTask.status) {

                        onSubTaskClick(
                            subTask.copy(
                                status = selectedStatus
                            )
                        )
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Biarkan kosong
                }
            }
    }

    override fun getItemCount(): Int {
        return subTaskList.size
    }
}