package com.app.insight360.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.app.insight360.DetailActivity
import com.app.insight360.databinding.NewsItemBinding
import com.app.insight360.models.Article
import com.bumptech.glide.Glide

class NewsAdapter(val context: Context, val articles: List<Article>) :
    Adapter<NewsAdapter.ArticleViewHolder>() {

    class ArticleViewHolder(val binding: NewsItemBinding) : ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = NewsItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ArticleViewHolder(binding)
    }

    override fun getItemCount(): Int = articles.size

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]
        holder.binding.tvNewsTitle.text = article.title
        holder.binding.tvNewsDescription.text = article.description
        Glide.with(context).load(article.urlToImage).into(holder.binding.imgNews)

        holder.itemView.setOnClickListener {
            Toast.makeText(context, article.title, Toast.LENGTH_SHORT).show()
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("URL", article.url)
            context.startActivity(intent)
        }
    }
}