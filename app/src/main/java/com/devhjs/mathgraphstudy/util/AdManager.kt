package com.devhjs.mathgraphstudy.util
 
 import android.app.Activity
import android.content.Context
import android.util.Log
 import com.devhjs.mathgraphstudy.BuildConfig
 import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    // 빌드 분기를 통한 Test ID 처리
    private val AD_UNIT_ID = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/1033173712" // Test ID
    } else {
        "ca-app-pub-3216980827282944/1022276279" // Real ID
    }
    // 비공개 테스트용 광고 ID 처리
//     private val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"


     private var interstitialAd: InterstitialAd? = null
     private var isAdLoading = false
 
     fun initialize(context: Context) {
         MobileAds.initialize(context) { }
         loadInterstitial(context)
     }
 
     fun loadInterstitial(context: Context) {
         if (interstitialAd != null || isAdLoading) return
 
         isAdLoading = true
         val adRequest = AdRequest.Builder().build()
 
         InterstitialAd.load(
             context,
             AD_UNIT_ID,
             adRequest,
             object : InterstitialAdLoadCallback() {
                 override fun onAdFailedToLoad(adError: LoadAdError) {
                     Log.e("AdManager", adError.message)
                     interstitialAd = null
                     isAdLoading = false
                 }
 
                 override fun onAdLoaded(ad: InterstitialAd) {
                     interstitialAd = ad
                     isAdLoading = false
                 }
             }
         )
     }
 
     fun showInterstitial(activity: Activity) {
         if (interstitialAd != null) {
             interstitialAd?.show(activity)
             interstitialAd = null
             loadInterstitial(activity)
         } else {
             Log.d("AdManager", "The interstitial ad wasn't ready yet.")
             loadInterstitial(activity)
         }
     }
 }
