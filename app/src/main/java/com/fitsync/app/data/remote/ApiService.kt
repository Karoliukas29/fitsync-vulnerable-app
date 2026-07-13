package com.fitsync.app.data.remote

import com.fitsync.app.data.remote.models.AuthRequest
import com.fitsync.app.data.remote.models.AuthResponse
import com.fitsync.app.data.remote.models.MemberResponse
import com.fitsync.app.data.remote.models.SubscriptionResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @GET("auth/refresh")
    suspend fun refreshToken(): Response<AuthResponse>

    @GET("members")
    suspend fun getMembers(): Response<List<MemberResponse>>

    @GET("members/{id}")
    suspend fun getMember(@Path("id") memberId: String): Response<MemberResponse>

    @GET("subscriptions/verify")
    suspend fun verifySubscription(): Response<SubscriptionResponse>

    @POST("subscriptions/purchase")
    suspend fun purchaseSubscription(@Body body: Map<String, String>): Response<SubscriptionResponse>
}
