package com.devhjs.mathgraphstudy.presentation.util

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Play 인앱 리뷰 창을 띄웁니다.
 * 실제로 창이 보일지는 Play 스토어가 정하며(할당량, 설치 경로 등), 실패해도 앱 흐름에는 영향이 없습니다.
 */
object InAppReviewLauncher {

    fun launch(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) {
                manager.launchReviewFlow(activity, request.result)
            } else {
                Log.w("InAppReview", "리뷰 요청 실패", request.exception)
            }
        }
    }
}
