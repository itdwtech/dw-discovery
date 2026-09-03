package com.discountworld.dwapp.repositories

import android.util.Log
import com.discountworld.discount.*
import com.discountworld.dwapp.managers.RedemptionStubClient
import com.discountworld.dwapp.managers.RedemptionStubClient.grpcCall

class RedemptionRepository {

    private val stub = RedemptionStubClient.stub

    suspend fun authenticateByCnic(cnic: String): CustomerCnicAuthResponse? {
        // Strip dashes if the server expects only digits
        val cleanCnic = cnic.replace("-", "")
        Log.d("Auth", "Authenticating with CNIC: $cleanCnic")

        val request = CustomerCnicAuthRequest.newBuilder()
            .setCnic(cleanCnic)
            .build()

        val result = grpcCall { stub.authenticateByCnic(request) }

        result.onSuccess {
            Log.d("Auth", "Success: ${it.customer.fullName}")
        }.onFailure {
            Log.e("Auth", "Failed: ${it.message}")
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

    suspend fun listCustomers(
        clientId: Long,
        page: Int = 1,
        pageSize: Int = 20,
        search: String = ""
    ): ListCustomersResponse? {
        val request = ListCustomersRequest.newBuilder()
            .setClientId(clientId)
            .setPage(page)
            .setPageSize(pageSize)
            .setSearch(search)
            .build()

        val result = grpcCall { stub.listCustomers(request) }
        result.onFailure {
            Log.e("RedemptionRepo", "listCustomers failed: ${it.message}")
        }
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
            Log.e("RedemptionRepo", "listCities failed: ${it.message}")
        }
        return result.getOrNull()?.citiesList
    }

    suspend fun listVendors(
        page: Int = 1,
        pageSize: Int = 20,
        cityId: Long? = null,
        categoryId: Long? = null,
        search: String? = null,
        featured: Boolean? = null,
        inStore: Boolean? = null,
        delivery: Boolean? = null,
        ecommerce: Boolean? = null
    ): ListRedemptionVendorsResponse? {
        val builder = ListRedemptionVendorsRequest.newBuilder()
            .setPage(page)
            .setPageSize(pageSize)

        cityId?.let { builder.setCityId(it) }
        categoryId?.let { builder.setCategoryId(it) }
        search?.let { builder.setSearch(it) }
        featured?.let { builder.setFeatured(it) }
        inStore?.let { builder.setInStore(it) }
        delivery?.let { builder.setDelivery(it) }
        ecommerce?.let { builder.setEcommerce(it) }

        val result = grpcCall { stub.listVendors(builder.build()) }
        result.onFailure {
            Log.e("RedemptionRepo", "listVendors failed: ${it.message}")
        }
        return result.getOrNull()
    }

    suspend fun getVendorDetail(vendorId: Long, cityId: Long? = null): RedemptionVendorDetail? {
        val builder = GetRedemptionVendorDetailRequest.newBuilder()
            .setVendorId(vendorId)

        cityId?.let { builder.setCityId(it) }

        val result = grpcCall { stub.getVendorDetail(builder.build()) }
        result.onFailure {
            Log.e("RedemptionRepo", "getVendorDetail failed: ${it.message}")
        }
        return result.getOrNull()?.vendor
    }

    suspend fun listMapPins(cityId: Long, categoryId: Long? = null): List<RedemptionMapPin>? {
        val builder = ListMapPinsRequest.newBuilder()
            .setCityId(cityId)

        categoryId?.let { builder.setCategoryId(it) }

        val result = grpcCall { stub.listMapPins(builder.build()) }
        result.onFailure {
            Log.e("RedemptionRepo", "listMapPins failed: ${it.message}")
        }
        return result.getOrNull()?.pinsList
    }

    suspend fun listStories(cityId: Long): List<RedemptionStory>? {
        val request = ListStoriesRequest.newBuilder()
            .setCityId(cityId)
            .build()

        val result = grpcCall { stub.listStories(request) }
        result.onFailure {
            Log.e("RedemptionRepo", "listStories failed: ${it.message}")
        }
        return result.getOrNull()?.storiesList
    }

    suspend fun listBanners(cityId: Long? = null): ListRedemptionBannersResponse? {
        val builder = ListRedemptionBannersRequest.newBuilder()
        cityId?.let { builder.setCityId(it) }

        val result = grpcCall { stub.listBanners(builder.build()) }
        result.onFailure {
            Log.e("RedemptionRepo", "listBanners failed: ${it.message}")
        }
        return result.getOrNull()
    }

    suspend fun listVendorDeals(vendorId: Long): List<RedemptionDealSummary>? {
        val request = ListVendorDealsRequest.newBuilder()
            .setVendorId(vendorId)
            .build()

        val result = grpcCall { stub.listVendorDeals(request) }
        result.onFailure {
            Log.e("RedemptionRepo", "listVendorDeals failed: ${it.message}")
        }
        return result.getOrNull()?.dealsList
    }

    suspend fun redeemDeal(dealId: Long, redeemPin: String? = null, cityId: Long? = null): RedeemDealResponse? {
        val builder = RedeemDealRequest.newBuilder()
            .setDealId(dealId)

        redeemPin?.let { builder.setRedeemPin(it) }
        cityId?.let { builder.setCityId(it) }

        val result = grpcCall { stub.redeemDeal(builder.build()) }
        result.onFailure {
            Log.e("RedemptionRepo", "redeemDeal failed: ${it.message}")
        }
        return result.getOrNull()
    }
}
