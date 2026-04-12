package com.discountworld.easypaisasdk.variables

interface FilterCallback{
    fun onChangeFilter(type : String)
}

interface CityCallback{
    fun onChangeCity(id : Int)
}
