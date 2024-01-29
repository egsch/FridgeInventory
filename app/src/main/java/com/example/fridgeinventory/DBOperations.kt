package com.example.fridgeinventory
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils

class DBOperations {
    /* adds an item and returns the primary key id of the item */
    public fun addItem(context: Context, name: String, barcode : String, expiration : String,
                location : String, lifetime : String, description : String, date : String) : Int {
        // Gets the data repository in write mode
        val dbHelper = DBHelper(context)
        val db = dbHelper.writableDatabase

        // Create a new map of values, where column names are the keys
        val values = ContentValues().apply {
            // don't set the id because we are using autoincrement
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
        if (newRowId != null && newRowId > 0) {
            val newIdCursor = db.rawQuery("SELECT ${DBContract.ItemEntry.ID_COL} FROM ${DBContract.ItemEntry.TABLE_NAME} where rowid = $newRowId;", null)
            newIdCursor.moveToFirst()
            val newId = newIdCursor.getInt(0)
            newIdCursor.close()
            return newId
        }
        return -1
    }

    /* removes an item */
    public fun removeItem(context: Context, id: Int) {
        val dbHelper = DBHelper(context)
        val db = dbHelper.writableDatabase

        db.execSQL("DELETE FROM ${DBContract.ItemEntry.TABLE_NAME} WHERE id=$id")
    }

    /**
     * Check if item exists
     * true if exists, false if not
     * uses id, NOT rowid
     */
    public fun itemExists(context: Context, id: Int) : Boolean {
        val dbHelper = DBHelper(context)
        val db = dbHelper.writableDatabase

        val cursor = db.rawQuery("SELECT * FROM ${DBContract.ItemEntry.TABLE_NAME} WHERE id=$id", null)
        val bool = cursor.moveToFirst()
        cursor.close()
        return bool
    }

    public fun getItemEntry(context: Context?, id: Int) : DBItemEntry? {
        val dbHelper = DBHelper(context)
        val db = dbHelper.writableDatabase

        val cursor = db.rawQuery("SELECT * FROM ${DBContract.ItemEntry.TABLE_NAME} WHERE id=$id", null)
        if (cursor.moveToFirst()) {
            val itemEntry = DBItemEntry(
                cursor.getInt(0),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5),
                cursor.getString(1),
                cursor.getString(6),
                cursor.getString(7)
            )
            return itemEntry
        } else {
            return null
        }
        cursor.close()
    }

    /* gets a list of all elements, with location filter applied */
    public fun readData(context: Context?, filter: String, sort: String) : ArrayList<DBItemEntry> {
        val dbHelper = DBHelper(context)
        val db = dbHelper.readableDatabase

        val queryString : String = if (filter == "") {
            "SELECT * FROM ${DBContract.ItemEntry.TABLE_NAME}" + " ORDER BY $sort"
        } else {
            "SELECT * FROM ${DBContract.ItemEntry.TABLE_NAME}" + " WHERE ${DBContract.ItemEntry.LOCATION_COL} = '$filter'"+ " ORDER BY $sort"
        }

        val cursor: Cursor = db.rawQuery(queryString, null)
        // create array list (will need to make some sort of item model)
        val dataset : ArrayList<DBItemEntry> = ArrayList()
        if (cursor.moveToFirst()) {
            do {
                val itemEntry = DBItemEntry(
                    cursor.getInt(0),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(1),
                    cursor.getString(6),
                    cursor.getString(7)
                )
                dataset.add(itemEntry)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return dataset
    }

    /**
     * Returns list of locations
     */
    fun getLocations(context: Context?) : ArrayList<String> {
        val dbHelper = DBHelper(context)
        val db = dbHelper.readableDatabase
        val cursor : Cursor = db.rawQuery("SELECT ${DBContract.LocationEntry.NAME_COL} FROM ${DBContract.LocationEntry.TABLE_NAME};", null)
        var locations = ArrayList<String>()
        if (cursor.moveToFirst()) {
            do {
                locations.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return locations
    }

    /**
     * Returns true if successfully added, false otherwise
     */
    fun addLocation(context : Context?, locationString: String) : Boolean {
        val dbHelper = DBHelper(context)
        val db = dbHelper.readableDatabase
        val cursor : Cursor = db.rawQuery("SELECT ${DBContract.LocationEntry.NAME_COL} FROM ${DBContract.LocationEntry.TABLE_NAME} WHERE ${DBContract.LocationEntry.NAME_COL} = '" + locationString + "';", null)
        if (!cursor.moveToFirst()) {
            db?.insert(DBContract.LocationEntry.TABLE_NAME, null, ContentValues(1).apply { put(DBContract.LocationEntry.NAME_COL, locationString) })
            return true
        }
        cursor.close()
        return false
    }

    public fun searchData(context: Context?, searchTerm: String, location: String, sort: String) : ArrayList<DBItemEntry> {
        val dbHelper = DBHelper(context)
        val db = dbHelper.readableDatabase

        val dataset : ArrayList<DBItemEntry> = ArrayList()
        db.execSQL("INSERT INTO t3(docid, "+DBContract.ItemEntry.NAME_COL+", "+DBContract.ItemEntry.BARCODE_COL+") SELECT id, " +DBContract.ItemEntry.NAME_COL+ ", "+DBContract.ItemEntry.BARCODE_COL+" FROM "+DBContract.ItemEntry.TABLE_NAME+";" )
        val queryString : String = if (location == "") {
            "SELECT * FROM ${DBContract.ItemEntry.TABLE_NAME} WHERE id IN (SELECT docid FROM t3 WHERE t3 MATCH '\"$searchTerm\"') ORDER BY $sort;"
        } else {
            "SELECT * FROM ${DBContract.ItemEntry.TABLE_NAME} WHERE ${DBContract.ItemEntry.LOCATION_COL} = \"$location\" AND id IN (SELECT docid FROM t3 WHERE t3 MATCH '\"$searchTerm\"') ORDER BY $sort;"
        }
        val cursor: Cursor = db.rawQuery(
            queryString, null)
        if (cursor.moveToFirst()) {
            do {
                val itemEntry = DBItemEntry(
                    cursor.getInt(0),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(1),
                    cursor.getString(6),
                    cursor.getString(7)
                )
                dataset.add(itemEntry)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return dataset
    }

    fun getNumEntries(context: Context?) : Int {
        val dbHelper = DBHelper(context)
        val db = dbHelper.readableDatabase
        return DatabaseUtils.queryNumEntries(db, DBContract.ItemEntry.TABLE_NAME).toInt()
    }
}