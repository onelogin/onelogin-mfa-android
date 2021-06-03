package com.onelogin.mfa.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FactorEntity::class], version = 1, exportSchema = false)
internal abstract class MfaDatabase : RoomDatabase() {
    internal abstract fun factorDao(): FactorDao
}
