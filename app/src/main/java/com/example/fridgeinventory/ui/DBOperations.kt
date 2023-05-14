package com.example.fridgeinventory.ui

import android.content.ContentValues
import android.content.Context
import com.example.fridgeinventory.DBContract
import com.example.fridgeinventory.DBHelper

class DBOperations {
    fun addItem(context: Context, id : Int, name: String, barcode : String, expiration : String,
                location : String, lifetime : String, description : String, date : String) {
        // Gets the data repository in write mode
        val dbHelper = DBHelper(context)
        val db = dbHelper.writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues().apply {
            put(DBContract.ItemEntry.ID_COL, id)
            put(DBContract.ItemEntry.BARCODE_COL, barcode)
            put(DBContract.ItemEntry.NAME_COL, name)
            put(DBContract.ItemEntry.EXPIRATION_COL, expiration)
            put(DBContract.ItemEntry.LOCATION_COL, location)
            put(DBContract.ItemEntry.LIFETIME_COL, lifetime)
            put(DBContract.ItemEntry.DESCRIPTION_COL, description)
            put(DBContract.ItemEntry.DATE_COL, date)
        }

        // Insert the new row, returning the primary key value of the new row
        val newRowId = db?.insert(DBContract.ItemEntry.TABLE_NAME, null, values)
    }
}