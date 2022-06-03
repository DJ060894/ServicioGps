package cr.ac.serviciogps.dao;


import androidx.room.Dao;
import androidx.room.Insert
import androidx.room.Query
import cr.ac.serviciogps.entity.Location

@Dao
interface LocationDao {

    @Insert
    fun insert(location: Location)

    @Query("select * from location_table")
    fun query(): List<Location>

}
