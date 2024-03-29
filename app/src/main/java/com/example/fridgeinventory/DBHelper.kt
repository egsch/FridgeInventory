package com.example.fridgeinventory

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.fridgeinventory.DBContract.ItemEntry
import com.example.fridgeinventory.ui.DBOperations

const val DATABASE_NAME = "INVENTORY"

class DBHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    override fun onCreate(db: SQLiteDatabase?) {
        val query =
            ("CREATE TABLE " + ItemEntry.TABLE_NAME + " ("
                    + ItemEntry.ID_COL + " INTEGER PRIMARY KEY, "
                    + ItemEntry.NAME_COL + " TEXT, "
                    + ItemEntry.BARCODE_COL + " TEXT, "
                    + ItemEntry.EXPIRATION_COL + " TEXT, "
                    + ItemEntry.LOCATION_COL + " TEXT, "
                    + ItemEntry.LIFETIME_COL + " TEXT, "
                    + ItemEntry.DESCRIPTION_COL + " TEXT, "
                    + ItemEntry.DATE_COL + " TEXT"
                    + ")")
        var query2 = "CREATE VIRTUAL TABLE t3 USING fts4(content=\"" + ItemEntry.TABLE_NAME+ "\", " + DBContract.ItemEntry.NAME_COL + ", " + DBContract.ItemEntry.BARCODE_COL + ");"
        var query3 = "CREATE TABLE " + DBContract.LocationEntry.TABLE_NAME + " (" + DBContract.LocationEntry.NAME_COL + " TEXT PRIMARY KEY)"

        db?.execSQL(query)
        db?.execSQL(query2)
        db?.execSQL(query3)

        db?.insert(DBContract.LocationEntry.TABLE_NAME, null, ContentValues(1).apply { put(DBContract.LocationEntry.NAME_COL, "Fridge") })
        db?.insert(DBContract.LocationEntry.TABLE_NAME, null, ContentValues(1).apply { put(DBContract.LocationEntry.NAME_COL, "Pantry") })
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // if the schema changes, replace the whole database
        db?.execSQL("DROP TABLE IF EXISTS " + ItemEntry.TABLE_NAME)
        db?.execSQL("DROP TABLE IF EXISTS t3")
        db?.execSQL("DROP TABLE IF EXISTS " + DBContract.LocationEntry.TABLE_NAME)

        Log.d("upgrading", "dropping table")
        onCreate(db)
    }
}