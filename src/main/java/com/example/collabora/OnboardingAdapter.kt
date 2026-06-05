package com.example.collabora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(private val items: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    data class OnboardingItem(
        val illustrationLayoutId: Int,
        val title: String,
        val description: String
    )

    class OnboardingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val containerIllustration: FrameLayout = view.findViewById(R.id.containerIllustration)
        val tvTitle: TextView = view.findViewById(R.id.tvOnboardingTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvOnboardingDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvDesc.text = item.description
        
        holder.containerIllustration.removeAllViews()
        LayoutInflater.from(holder.itemView.context).inflate(item.illustrationLayoutId, holder.containerIllustration, true)
    }

    override fun getItemCount(): Int = items.size
}