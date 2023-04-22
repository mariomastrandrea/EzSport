package it.polito.mad.sportapp.localDB

import androidx.room.ColumnInfo
import androidx.room.Ignore
import it.polito.mad.sportapp.entities.EquipmentReservation
import java.time.LocalDate
import java.time.LocalTime

//Useful classes for the queries in join operations

//data class for the join between user and sport to get the level of the sport played by the user
data class SportLevel(
    @ColumnInfo(name = "name")
    val sport: String?,
    @ColumnInfo(name = "level")
    val level: String?
)

data class DetailedReservation(
    @ColumnInfo(name = "id")
    val id: Int,
    @ColumnInfo(name = "user_id")
    val userId: Int,
    @ColumnInfo(name = "sport_center_name")
    val sportCenterName: String,
    @ColumnInfo(name = "address")
    val location: String,
    @ColumnInfo(name = "sport_name")
    val sportName : String,
    @ColumnInfo(name = "start_date_time")
    val startDateTime: String,
    @ColumnInfo(name = "end_date_time")
    val endDateTime: String,
    @ColumnInfo(name = "playground_name")
    val playgroundName: String,
    @ColumnInfo(name = "total_price")
    val totalPrice: Float,
)
{
    @Ignore
    var date: LocalDate = LocalDate.parse(startDateTime.substring(0,10))
    @Ignore
    var startTime: LocalTime = LocalTime.parse(startDateTime.substring(11,16))
    @Ignore
    var endTime: LocalTime = LocalTime.parse(endDateTime.substring(11,16))
    @Ignore
    var equipments: List<EquipmentReservation> = listOf()
}