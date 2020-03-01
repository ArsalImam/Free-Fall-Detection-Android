package io.xbird.library.database.dao

import androidx.room.Dao
import androidx.room.Query
import io.reactivex.Maybe
import io.xbird.library.database.entity.FreeFallReading

@Dao
interface FreeFallReadingDao : BaseDao<FreeFallReading> {

    /**
     * Get all data from the Data table.
     */
    @Query("SELECT * FROM tbl_free_fall_readings ORDER BY id DESC")
    fun getLatestReadings(): Maybe<List<FreeFallReading>>
}