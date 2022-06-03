package cr.ac.serviciogps.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="location_table")

data class Location(
    @PrimaryKey (autoGenerate = true)

    var locationId: Long?,
    var latitude: Double,
    var longitude: Double
)