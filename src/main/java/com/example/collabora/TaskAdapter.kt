package com.example.collabora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(

    private val taskList: ArrayList<Task>,

    private val onTaskClick:
        (Task) -> Unit

) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvTaskTitle: TextView =
            itemView.findViewById(R.id.tvTaskTitle)

        val tvTaskDescription: TextView =
            itemView.findViewById(R.id.tvTaskDescription)

        val tvTaskStatus: TextView =
            itemView.findViewById(R.id.tvTaskStatus)

        val tvTaskPriority: TextView =
            itemView.findViewById(R.id.tvTaskPriority)

        val tvTaskDeadline: TextView =
            itemView.findViewById(R.id.tvTaskDeadline)

        val viewPriority: View =
            itemView.findViewById(R.id.viewPriority)

        val cardPriority: CardView =
            itemView.findViewById(R.id.cardPriority)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaskViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_task,
                    parent,
                    false
                )

        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: TaskViewHolder,
        position: Int
    ) {

        val task =
            taskList[position]

        // ==============================
        // CLICK TASK
        // ==============================

        holder.itemView.setOnClickListener {

            onTaskClick(task)
        }

        // ==============================
        // SET DATA
        // ==============================

        holder.tvTaskTitle.text =
            task.title

        holder.tvTaskDescription.text =
            task.description

        holder.tvTaskStatus.text =
            task.status

        holder.tvTaskPriority.text =
            task.priority

        holder.tvTaskDeadline.text =
            "⏰ ${task.deadline}"

        // ==============================
        // TASK SELESAI EFFECT
        // ==============================

        if (task.status == "Selesai") {

            holder.itemView.alpha = 0.5f

        } else {

            holder.itemView.alpha = 1.0f
        }

        // ==============================
        // PRIORITY COLOR
        // ==============================

        when (task.priority) {

            "Tinggi" -> {

                holder.viewPriority.setBackgroundColor(
                    android.graphics.Color.parseColor("#EF4444")
                )

                holder.cardPriority.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#FEE2E2")
                )
            }

            "Sedang" -> {

                holder.viewPriority.setBackgroundColor(
                    android.graphics.Color.parseColor("#F59E0B")
                )

                holder.cardPriority.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#FEF3C7")
                )
            }

            "Rendah" -> {

                holder.viewPriority.setBackgroundColor(
                    android.graphics.Color.parseColor("#1E9E75")
                )

                holder.cardPriority.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#DCFCE7")
                )
            }
        }
    }

    override fun getItemCount(): Int {

        return taskList.size
    }
}