package com.discountworld.dwapp.repositories

import com.discountworld.discount.*
import com.discountworld.dwapp.managers.RedemptionStubClient
import com.discountworld.dwapp.managers.RedemptionStubClient.grpcCall

class RedemptionRepository {

    private val stub = RedemptionStubClient.stub

    suspend fun authenticateByCnic(cnic: String): CustomerCnicAuthResponse? {
        // Strip dashes if the server expects only digits
        val cleanCnic = cnic.replace("-", "")
        android.util.Log.d("Auth", "Authenticating with CNIC: $cleanCnic")

        val request = CustomerCnicAuthRequest.newBuilder()
            .setCnic(cleanCnic)
            .build()

        val result = grpcCall { stub.authenticateByCnic(request) }
        
        result.onSuccess {
            android.util.Log.d("Auth", "Success: ${it.customer.fullName}")
        }.onFailure {
            android.util.Log.e("Auth", "Failed: ${it.message}")
        }
        
        return result.getOrNull()
    }

    suspend fun getCustomerProfile(): Customer? {
        val request = GetCustomerProfileRequest.newBuilder().build()
        val result = grpcCall { stub.getCustomerProfile(request) }
        return result.getOrNull()
    }

    suspend fun updateCustomerProfile(email: String, fullName: String, phoneNumber: String): Customer? {
        val request = UpdateCustomerProfileRequest.newBuilder()
            .setEmail(email)
            .setFullName(fullName)
            .setPhoneNumber(phoneNumber)
            .build()

        val result = grpcCall { stub.updateCustomerProfile(request) }
        return result.getOrNull()
    }

    suspend fun listCategories(): List<RedemptionCategory>? {
        val request = ListRedemptionCategoriesRequest.newBuilder().build()
        val result = grpcCall { stub.listCategories(request) }
        return result.getOrNull()?.categoriesList
    }

    suspend fun listCities(featuredOnly: Boolean = false): List<RedemptionCity>? {
        val request = ListRedemptionCitiesRequest.newBuilder()
            .setFeaturedOnly(featuredOnly)
            .build()

        val result = grpcCall { stub.listCities(request) }
        result.onFailure {
            println("gRPC listCities failed: ${it.message}")
        }
        return result.getOrNull()?.citiesList
    }
}
