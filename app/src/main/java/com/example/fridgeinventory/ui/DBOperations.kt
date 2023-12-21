package com.example.fridgeinventory.ui

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import com.example.fridgeinventory.DBContract
import com.example.fridgeinventory.DBHelper
import com.example.fridgeinventory.DBItemEntry

class DBOperations {
    public fun addItem(context: Context, name: String, barcode : String, expiration : String,
                location : String, lifetime : String, description : String, date : String) {
        // Gets the data repository in write mode
        val dbHelper = DBHelper(context)
        val db = dbHelper.writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues().apply {
            // put(DBContract.ItemEntry.ID_COL, id)
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

    public fun removeItem(context: Context, id: Int) {
        // todo: test
        val dbHelper = DBHelper(context)
        var db = dbHelper.writableDatabase

        db.execSQL("DELETE FROM ${DBContract.ItemEntry.TABLE_NAME} WHERE id=$id")
    }

    public fun readData(context: Context?) : ArrayList<DBItemEntry> {
        val dbHelper = DBHelper(context)
        val db = dbHelper.readableDatabase

        val cursor: Cursor = db.rawQuery("SELECT * FROM ${DBContract.ItemEntry.TABLE_NAME}"
                + " ORDER BY ${DBContract.ItemEntry.DATE_COL}", null)
        // create array list (will need to make some sort of item model)
        var dataset : ArrayList<DBItemEntry> = ArrayList();
        if (cursor.moveToFirst()) {
            do {
                // todo: reading wrong data into elements
                var itemEntry = DBItemEntry(
                    cursor.getInt(0),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(1),
                    cursor.getString(6),
                    cursor.getString(7)
                );
                dataset.add(itemEntry)
            } while (cursor.moveToNext())
        }
        return dataset;
    }

    public fun searchData(context: Context?, searchTerm: String) : ArrayList<DBItemEntry> {
        val dbHelper = DBHelper(context)
        val db = dbHelper.readableDatabase

        var dataset : ArrayList<DBItemEntry> = ArrayList();
        // todo: match with reading above once you fix
        db.execSQL("INSERT INTO t3(docid, "+DBContract.ItemEntry.NAME_COL+", "+DBContract.ItemEntry.BARCODE_COL+") SELECT id, " +DBContract.ItemEntry.NAME_COL+ ", "+DBContract.ItemEntry.BARCODE_COL+" FROM "+DBContract.ItemEntry.TABLE_NAME+";" )
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${DBContract.ItemEntry.TABLE_NAME} WHERE id IN (SELECT docid FROM t3 WHERE t3 MATCH '\"$searchTerm\"') ORDER BY ${DBContract.ItemEntry.DATE_COL};", null)
        if (cursor.moveToFirst()) {
            do {
                var itemEntry = DBItemEntry(
                    cursor.getInt(0),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(1),
                    cursor.getString(6),
                    cursor.getString(7)
                );
                dataset.add(itemEntry)
            } while (cursor.moveToNext())
        }
        return dataset
    }

    fun getNumEntries(context: Context?) : Int {
        val dbHelper = DBHelper(context)
        val db = dbHelper.readableDatabase
        return DatabaseUtils.queryNumEntries(db, DBContract.ItemEntry.TABLE_NAME).toInt()
    }
}