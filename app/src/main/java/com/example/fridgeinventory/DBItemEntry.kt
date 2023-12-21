package com.example.fridgeinventory

data class DBItemEntry (
    var id : Int,
    var barcode : String,
    var expiration: String,
    var location: String,
    var lifetime: String,
    var name: String,
    var description: String,
    var date: String,
    var deleted: Boolean = false
)