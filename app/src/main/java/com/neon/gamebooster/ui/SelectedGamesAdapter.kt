package com.neon.gamebooster.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.neon.gamebooster.R

data class GameModel(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)

class SelectedGamesAdapter(
    private val gamesList: List<GameModel>,
    private val onLaunchClick: (GameModel) -> Unit
) : RecyclerView.Adapter<SelectedGamesAdapter.GameViewHolder>() {

    class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon: ImageView = itemView.findViewById(R.id.imgAppIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvAppName)
        val btnLaunch: Button = itemView.findViewById(R.id.btnLaunchGame)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_selected_game, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = gamesList[position]
        holder.tvName.text = game.appName
        holder.imgIcon.setImageDrawable(game.icon)
        holder.btnLaunch.setOnClickListener {
            onLaunchClick(game)
        }
    }

    override fun getItemCount(): Int = gamesList.size
}
