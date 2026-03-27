package com.example.easypaisasdk.repositories

import com.discountworld.discovery.Banner
import com.discountworld.discovery.Branch
import com.discountworld.discovery.Campaign
import com.discountworld.discovery.CardDiscount
import com.discountworld.discovery.CardType
import com.discountworld.discovery.Deal
import com.discountworld.discovery.GetBranchRequest
import com.discountworld.discovery.GetCardDiscountRequest
import com.discountworld.discovery.GetDealRequest
import com.discountworld.discovery.GetVendorFullRequest
import com.discountworld.discovery.GetVendorRequest
import com.discountworld.discovery.ListBannersRequest
import com.discountworld.discovery.ListBranchesRequest
import com.discountworld.discovery.ListCampaignsRequest
import com.discountworld.discovery.ListCardDiscountsRequest
import com.discountworld.discovery.ListCardTypesRequest
import com.discountworld.discovery.ListDealsRequest
import com.discountworld.discovery.ListProductsRequest
import com.discountworld.discovery.PaginationRequest
import com.discountworld.discovery.Product
import com.discountworld.discovery.VendorDetail
import com.discountworld.discovery.VendorFullDetail
import com.example.easypaisasdk.managers.GrpcStubClient
import com.example.easypaisasdk.managers.GrpcStubClient.grpcCall

class DetailRepository {

    val stub = GrpcStubClient.stub

    val stubMerge = GrpcStubClient.stubMerge

    // Card Type Requests/Responses
    suspend fun getListOfCardType(): List<CardType>?{
        val request = ListCardTypesRequest.newBuilder().build()
        val result = grpcCall { stub.listCardTypes(request) }
        result.onSuccess { response ->
            return response.cardTypesList
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    suspend fun getVendor(vendorId: Long, cityId: Long): VendorDetail? {

        val request = GetVendorRequest.newBuilder()
            .setVendorId(vendorId)
            .setCityId(cityId)
            .build()

        val result = grpcCall { stub.getVendor(request) }
        result.onSuccess { response ->
            return response.vendor
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    suspend fun getFullVendor(vendorId: Long, cityId: Long): VendorFullDetail? {

        val request = GetVendorFullRequest.newBuilder()
            .setVendorId(vendorId)
            .setCityId(cityId)
            .build()

        val result = grpcCall { stubMerge.getVendorFull(request)}
        result.onSuccess { response ->
            return response.vendor
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    // Branch Requests/Responses

    suspend fun getListOfBranches(
        page: Int = 1,
        pageSize: Int = 20,
        vendorId: Long? = null,
        cityId: Long? = null
    ): List<Branch>? {

        val pagination = PaginationRequest.newBuilder()
            .setPage(page)
            .setSize(pageSize)
            .build()

        val requestBuilder = ListBranchesRequest.newBuilder()
            .setPagination(pagination)

        vendorId?.let { requestBuilder.vendorId = it }
        cityId?.let { requestBuilder.cityId = it }

        val request = requestBuilder.build()

        val result = grpcCall { stub.listBranches(request) }
        result.onSuccess { response ->
            return response.branchesList
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    suspend fun getBranch(branchId: Long): Branch? {

        val request = GetBranchRequest.newBuilder()
            .setBranchId(branchId)
            .build()

        val result = grpcCall { stub.getBranch(request) }
        result.onSuccess { response ->
            return response.branch
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    // Deal Requests/Responses

    suspend fun getListOfDeals(
        page: Int = 1,
        pageSize: Int = 20,
        vendorId: Long? = null,
        featured: Boolean? = null
    ): List<Deal>? {

        val pagination = PaginationRequest.newBuilder()
            .setPage(page)
            .setSize(pageSize)
            .build()


        val requestBuilder = ListDealsRequest.newBuilder()
            .setPagination(pagination)

        vendorId?.let { requestBuilder.vendorId = it }
        featured?.let { requestBuilder.featured = it }

        val request = requestBuilder.build()

        val result = grpcCall { stub.listDeals(request) }
        result.onSuccess { response ->
            return response.dealsList
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    suspend fun getDeal(dealId: Long): Deal? {

        val request = GetDealRequest.newBuilder()
            .setDealId(dealId)
            .build()

        val result = grpcCall { stub.getDeal(request) }
        result.onSuccess { response ->
            return response.deal
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    // Card Discount Requests/Responses

    suspend fun getListOfCardDiscounts(
        page: Int = 1,
        pageSize: Int = 20,
        vendorId: Long? = null,
        featured: Boolean? = null
    ): List<CardDiscount>? {

        val pagination = PaginationRequest.newBuilder()
            .setPage(page)
            .setSize(pageSize)
            .build()


        val requestBuilder = ListCardDiscountsRequest.newBuilder()
            .setPagination(pagination)

        vendorId?.let { requestBuilder.vendorId = it }
        featured?.let { requestBuilder.featured = it }

        val request = requestBuilder.build()

        val result = grpcCall { stub.listCardDiscounts(request) }
        result.onSuccess { response ->
            return response.cardDiscountsList
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    suspend fun getCardDiscount(cardDiscountId: Long): CardDiscount? {

        val request = GetCardDiscountRequest.newBuilder()
            .setCardDiscountId(cardDiscountId)
            .build()

        val result = grpcCall { stub.getCardDiscount(request) }
        result.onSuccess { response ->
            return response.cardDiscount
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    // Product Requests/Responses

    suspend fun getListOfProducts(
        page: Int = 1,
        pageSize: Int = 20,
        vendorId: Long? = null,
        categoryId: Long? = null
    ): List<Product>? {

        val pagination = PaginationRequest.newBuilder()
            .setPage(page)
            .setSize(pageSize)
            .build()

        val requestBuilder = ListProductsRequest.newBuilder()
            .setPagination(pagination)

        vendorId?.let { requestBuilder.vendorId = it }
        categoryId?.let { requestBuilder.categoryId = it }

        val request = requestBuilder.build()

        val result = grpcCall { stub.listProducts(request) }
        result.onSuccess { response ->
            return response.productsList
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    // Banner Requests/Responses

    suspend fun getListOfBanners(
        page: Int = 1,
        pageSize: Int = 20,
        vendorId: Long? = null
    ): List<Banner>? {

        val pagination = PaginationRequest.newBuilder()
            .setPage(page)
            .setSize(pageSize)
            .build()

        val requestBuilder = ListBannersRequest.newBuilder()
            .setPagination(pagination)

        vendorId?.let { requestBuilder.vendorId = it }

        val request = requestBuilder.build()

        val result = grpcCall { stub.listBanners(request) }
        result.onSuccess { response ->
            return response.bannersList
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }

    // Campaign Requests/Responses

    suspend fun getListOfCampaigns(
        page: Int = 1,
        pageSize: Int = 20,
        vendorId: Long? = null
    ): List<Campaign>? {

        val pagination = PaginationRequest.newBuilder()
            .setPage(page)
            .setSize(pageSize)
            .build()

        val requestBuilder = ListCampaignsRequest.newBuilder()
            .setPagination(pagination)

        vendorId?.let { requestBuilder.vendorId = it }

        val request = requestBuilder.build()

        val result = grpcCall { stub.listCampaigns(request) }
        result.onSuccess { response ->
            return response.campaignsList
        }.onFailure { throwable ->
            println("gRPC failed: ${throwable.message}")
        }
        return null
    }


    suspend fun getBanners(): List<Banner>? {

        val request = ListBannersRequest.newBuilder().build()

        val result = grpcCall { stub.listBanners(request) }

        result.onSuccess { response ->
            return response.bannersList
        }.onFailure {
            println("Banner API failed: ${it.message}")
        }

        return null
    }
}