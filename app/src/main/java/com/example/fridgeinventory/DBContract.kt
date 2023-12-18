package com.example.fridgeinventory

import android.provider.BaseColumns

object DBContract {
    object ItemEntry : BaseColumns {
        const val TABLE_NAME = "items"
        const val ID_COL = "id"
        const val BARCODE_COL = "barcode"
        const val EXPIRATION_COL = "expiration"
        const val LOCATION_COL = "location"
        const val LIFETIME_COL = "lifetime"
        const val NAME_COL = "name"
        const val DESCRIPTION_COL = "description"
        const val DATE_COL = "date"
    }
}