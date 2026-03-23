package com.example.easypaisasdk.repositories

import com.discountworld.discovery.CardType
import com.discountworld.discovery.Category
import com.discountworld.discovery.City
import com.discountworld.discovery.GeoPoint
import com.discountworld.discovery.ListCardTypesRequest
import com.discountworld.discovery.ListCategoriesRequest
import com.discountworld.discovery.ListCitiesRequest
import com.discountworld.discovery.ListVendorsRequest
import com.discountworld.discovery.PaginationRequest
import com.discountworld.discovery.ResolveCityRequest
import com.discountworld.discovery.VendorSummary
import com.example.easypaisasdk.managers.GrpcStubClient
import com.example.easypaisasdk.managers.GrpcStubClient.grpcCall

class HomeRepository {

    val stub = GrpcStubClient.stub

    suspend fun getCityByCoordinates(lat: Float, lng: Float): City?{
        val geo = GeoPoint.newBuilder()
            .setLatitude(lat.toDouble())
            .setLongitude(lng.toDouble())
            .build()

        val request = ResolveCityRequest.newBuilder()
            .setLocation(geo)
            .build()

        val result = grpcCall { stub.resolveCity(request) }
        result.onSuccess { response ->
            return response.city
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    suspend fun getListOfCities(isFeatured: Boolean): List<City>?{
        val request = ListCitiesRequest.newBuilder().setFeaturedOnly(isFeatured).build()
        val result = grpcCall { stub.listCities(request) }
        result.onSuccess { response ->
            return response.citiesList
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    suspend fun getCategories(): List<Category>?{
        val request = ListCategoriesRequest.newBuilder().build()
        val result = grpcCall { stub.listCategories(request) }
        result.onSuccess { response ->
            return response.categoriesList
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    suspend fun getListOfVendors(
        page: Int = 1,
        pageSize: Int = 20,
        cityId: Long? = null,
        categoryId: Long? = null,
        search: String? = null,
        featured: Boolean? = null,
        discountType: String? = null,
        minDiscountValue: Double? = null,
        hasDiscount: Boolean? = null,
        isOpen: Boolean? = null
    ): List<VendorSummary>? {

        val pagination = PaginationRequest.newBuilder()
            .setPage(page)
            .setSize(pageSize)
            .build()

        val requestBuilder = ListVendorsRequest.newBuilder()
            .setPagination(pagination)

        cityId?.let { requestBuilder.cityId = it }
        categoryId?.let { requestBuilder.categoryId = it }
        search?.let { requestBuilder.search = it }
        featured?.let { requestBuilder.featured = it }
        discountType?.let { requestBuilder.discountType = it }
        minDiscountValue?.let { requestBuilder.minDiscountValue = it }
        hasDiscount?.let { requestBuilder.hasDiscount = it }
        isOpen?.let { requestBuilder.isOpen = it }

        val request = requestBuilder.build()
        val result = grpcCall { stub.listVendors(request) }
        result.onSuccess { response ->
            return response.vendorsList
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }


}