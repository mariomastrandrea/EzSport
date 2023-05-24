package it.polito.mad.sportapp.localDB.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import it.polito.mad.sportapp.entities.room.RoomDetailedReservationForAvailablePlaygrounds
import it.polito.mad.sportapp.entities.room.RoomDetailedReservation
import it.polito.mad.sportapp.entities.room.RoomPlaygroundReservation

@Dao
interface ReservationDao {

    @Query("SELECT * FROM playground_reservation")
    fun getAll(): List<RoomPlaygroundReservation>

    @Query("SELECT COUNT(id) FROM playground_reservation WHERE playground_id == :playgroundId AND datetime(start_date_time) < datetime(:endDateTime) AND datetime(end_date_time) > datetime(:startDateTime)")
    fun getReservationIfAvailable(
        playgroundId: Int,
        startDateTime: String,
        endDateTime: String
    ): Int

    @Query("SELECT * FROM playground_reservation WHERE playground_id == :playgroundId")
    fun findByPlaygroundId(playgroundId: Int): List<RoomPlaygroundReservation>

    @Query(
        "SELECT PR.id, PR.user_id, PR.playground_id , U.username, SC.name AS sport_center_name, SC.address, S.name AS sport_name, S.emoji AS sport_emoji, " +
                "PR.start_date_time, PR.end_date_time, PS.playground_name, PR.total_price, PS.sport_id, PS.sport_center_id, PS.cost_per_hour AS playground_price_per_hour " +
                " FROM sport AS S, playground_sport AS PS, playground_reservation as PR, sport_center AS SC, USER AS U " +
                "WHERE PR.sport_id = S.id AND PR.playground_id = PS.id AND PR.sport_center_id = SC.id AND PR.user_id = :userId AND user_id = U.id"
    )
    fun findByUserId(userId: Int): List<RoomDetailedReservation>

    @Query(
        "SELECT PR.id, PR.user_id, PR.playground_id, U.username, SC.name AS sport_center_name, SC.address, S.name AS sport_name, S.emoji AS sport_emoji, " +
                "PR.start_date_time, PR.end_date_time, PS.playground_name, PR.total_price, PS.sport_id, PS.sport_center_id, PS.cost_per_hour AS playground_price_per_hour " +
                " FROM sport AS S, playground_sport AS PS, playground_reservation as PR, sport_center AS SC, user AS U " +
                "WHERE PR.sport_id = S.id AND PR.playground_id = PS.id AND PR.sport_center_id = SC.id AND PR.sport_id = :sportId AND user_id = U.id"
    )
    fun findBySportId(sportId: Int): List<RoomDetailedReservation>

    @Query("SELECT * FROM playground_reservation WHERE id == :id LIMIT 1")
    fun findById(id: Int): RoomPlaygroundReservation

    @Query(
        "SELECT PR.id, PR.user_id, PR.playground_id, U.username, SC.name AS sport_center_name, SC.address, S.name AS sport_name, S.emoji AS sport_emoji, " +
                "PR.start_date_time, PR.end_date_time, PS.playground_name, PR.total_price, PS.sport_id, PS.sport_center_id, PS.cost_per_hour AS playground_price_per_hour " +
                "FROM sport AS S, playground_sport AS PS, playground_reservation as PR, sport_center AS SC, user AS U " +
                "WHERE PR.sport_id = S.id AND PR.playground_id = PS.id AND PR.sport_center_id = SC.id AND PR.id = :id AND user_id = U.id"
    )
    fun findDetailedReservationById(id: Int): RoomDetailedReservation

    @Query(
        "SELECT PR.start_date_time , PR.end_date_time, PS.id AS playground_id, PS.sport_id, S.emoji AS sport_emoji, S.name AS sport_name, SC.id AS sport_center_id,SC.name AS sport_center_name, SC.address AS sport_center_address, PS.playground_name, PS.cost_per_hour AS price_per_hour " +
                "FROM playground_reservation AS PR, PLAYGROUND_SPORT AS PS, sport_center AS SC, sport AS S " +
                "WHERE PR.playground_id = PS.Id AND " +
                "SC.Id = PS.sport_center_id AND " +
                "PS.sport_id == :sportId AND " +
                "PS.sport_id == S.id AND " +
                "PR.start_date_time LIKE :yearMonth || '%'"
    )
    fun findPlaygroundsBySportIdAndDate(
        sportId: Int,
        yearMonth: String
    ): List<RoomDetailedReservationForAvailablePlaygrounds>

    @Insert
    fun insertAll(vararg playgroundReservations: RoomPlaygroundReservation)

    @Insert
    fun insert(playgroundReservation: RoomPlaygroundReservation): Long

    @Query("UPDATE playground_reservation SET total_price = total_price + :price WHERE id LIKE :reservationId")
    fun increasePrice(reservationId: Int, price: Float)

    @Query("UPDATE playground_reservation SET total_price = total_price - :price WHERE id LIKE :reservationId")
    fun reducePrice(reservationId: Int, price: Float)

    @Query("DELETE FROM playground_reservation WHERE id LIKE :reservationId")
    fun deleteById(reservationId: Int)


}