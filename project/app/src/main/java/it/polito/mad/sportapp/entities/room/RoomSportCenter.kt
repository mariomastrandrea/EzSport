package it.polito.mad.sportapp.entities.room

import androidx.room.*

@Entity(tableName = "sport_center",)
data class RoomSportCenter(
    @PrimaryKey(autoGenerate = true)
    val id: Int ,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "address")
    val address: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    @ColumnInfo(name = "opening_hour")
    val openingHours: String,
    @ColumnInfo(name = "closing_hour")
    val closingHours: String,
)
