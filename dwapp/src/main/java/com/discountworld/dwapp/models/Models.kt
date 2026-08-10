package com.discountworld.dwapp.models

data class TopPick(val image: Int, val name: String)

data class PopularBrand(val logo: Int, val name: String, val category: String)

data class DeliveryDeal(val banner: Int, val logo: Int, val name: String, val category: String)

data class HistoryItem(val logo: Int, val title: String, val code: String, val dateTime: String)
