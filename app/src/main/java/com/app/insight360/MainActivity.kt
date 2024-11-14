package com.app.insight360

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.app.insight360.StackLayoutManager.StackLayoutManager
import com.app.insight360.adapters.NewsAdapter
import com.app.insight360.databinding.ActivityMainBinding
import com.app.insight360.models.Article
import com.app.insight360.models.News
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NewsAdapter
    private var articles = mutableListOf<Article>()
    private var pageNum = 1
    private var totalResults = -1

    private var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //ADMOB Code
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }
            })



        adapter = NewsAdapter(this@MainActivity, articles)
        binding.rvNews.adapter = adapter

        val layoutManager = StackLayoutManager(StackLayoutManager.ScrollOrientation.BOTTOM_TO_TOP)
        layoutManager.setPagerMode(true)
        layoutManager.setPagerFlingVelocity(3000)
        binding.rvNews.layoutManager = layoutManager

        layoutManager.setItemChangedListener(object : StackLayoutManager.ItemChangedListener {
            override fun onItemChanged(position: Int) {
                binding.container.setBackgroundColor(Color.parseColor(ColorPicker.getColor()))
                Log.d("TAG", "First Visible Item - ${layoutManager.getFirstVisibleItemPosition()}")
                Log.d("TAG", "Total Count - ${layoutManager.itemCount}")
                if (totalResults > layoutManager.itemCount && layoutManager.getFirstVisibleItemPosition() >= layoutManager.itemCount - 5) {
                    pageNum++
                    getNews()
                }

                if (mInterstitialAd != null) {
                    mInterstitialAd?.show(this@MainActivity)
                }
            }

        })

        // Fetch and display news data
        getNews()
    }

    private fun getNews() {
        Log.d("TAG", "Request sent for $pageNum")
        val news = NewsService.newsInstance.getHeadlines("us", pageNum)
        news.enqueue(object : Callback<News> {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call<News>, response: Response<News>) {
                val news = response.body()
                if (news != null) {
                    totalResults = news.totalResults
                    Log.d("DHARMIN", news.toString())
                    articles.addAll(news.articles)
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onFailure(call: Call<News>, t: Throwable) {
                Log.d("DHARMIN", "Error in Fetching News!!!", t)
            }
        })
    }
}