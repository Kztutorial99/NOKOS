package com.example.network

import com.example.model.BalanceDto
import com.example.model.CountryDto
import com.example.model.CreateOrderBody
import com.example.model.OrderResponseDto
import com.example.model.ProductDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SmsCodeApi {

    @GET("v1/balance")
    suspend fun getBalance(
        @Header("Authorization") authHeader: String
    ): Response<BalanceDto>

    @GET("v1/catalog/countries")
    suspend fun getCountries(
        @Header("Authorization") authHeader: String
    ): Response<List<CountryDto>>

    @GET("v1/catalog/products")
    suspend fun getProducts(
        @Header("Authorization") authHeader: String,
        @Query("country_id") countryId: String? = null
    ): Response<List<ProductDto>>

    @POST("v1/orders")
    suspend fun createOrder(
        @Header("Authorization") authHeader: String,
        @Body body: CreateOrderBody
    ): Response<OrderResponseDto>

    @GET("v1/orders/{id}")
    suspend fun getOrder(
        @Header("Authorization") authHeader: String,
        @Path("id") orderId: String
    ): Response<OrderResponseDto>

    @POST("v1/orders/{id}/cancel")
    suspend fun cancelOrder(
        @Header("Authorization") authHeader: String,
        @Path("id") orderId: String
    ): Response<ResponseBody>

    @POST("v1/orders/{id}/finish")
    suspend fun finishOrder(
        @Header("Authorization") authHeader: String,
        @Path("id") orderId: String
    ): Response<ResponseBody>
}
