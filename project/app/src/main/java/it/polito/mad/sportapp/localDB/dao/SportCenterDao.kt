package it.polito.mad.sportapp.localDB.dao

import androidx.room.Dao
import androidx.room.Query
import it.polito.mad.sportapp.entities.SportCenter

@Dao
interface SportCenterDao {
    @Query("SELECT * FROM sport_center")
    fun getAll(): List<SportCenter>

    @Query("SELECT * FROM sport_center WHERE name LIKE :name LIMIT 1")
    fun findByName(name: String): SportCenter

    @Query("SELECT * FROM sport_center WHERE id == :id LIMIT 1")
    fun findById(id: Int): SportCenter




}