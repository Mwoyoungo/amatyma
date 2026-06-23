package com.lokaleza.amatyma

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.lokaleza.amatyma.databinding.ItemPostGridBinding

data class Post(
    val postId: String,
    val mediaUrl: String,
    val mediaType: String,
    val caption: String
)

class PostGridAdapter(
    private val posts: List<Post>,
    private val onPostClick: ((Post) -> Unit)? = null,
    private val onPostLongClick: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<PostGridAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPostGridBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemPostGridBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]
        holder.binding.ivPost.load(post.mediaUrl) {
            placeholder(R.drawable.ic_default_avatar)
            error(R.drawable.ic_default_avatar)
        }
        holder.binding.ivVideoIndicator.visibility =
            if (post.mediaType == "video") View.VISIBLE else View.GONE

        onPostClick?.let { holder.binding.root.setOnClickListener { it(post) } }
        onPostLongClick?.let { holder.binding.root.setOnLongClickListener { _ -> it(post); true } }
    }

    override fun getItemCount() = posts.size
}
