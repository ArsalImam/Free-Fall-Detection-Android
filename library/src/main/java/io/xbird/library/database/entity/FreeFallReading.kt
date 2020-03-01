package io.xbird.library.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.xbird.library.utils.getFormatedDate
import org.apache.commons.lang3.math.NumberUtils
import java.util.*

@Entity(tableName = "tbl_free_fall_readings")
data class FreeFallReading(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "createdDate") val createdDate: Date? = Date(),
    @ColumnInfo(name = "duration") val duration: Long? = NumberUtils.LONG_ZERO,
    @ColumnInfo(name = "timestamp") val timestamp: Long? = NumberUtils.LONG_ZERO
) {
    val formatedDate: String
        get() {
            return getFormatedDate(createdDate!!)
        }
}