package it.polito.mad.sportapp.reservation_details

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polito.mad.sportapp.entities.DetailedReservation
import it.polito.mad.sportapp.entities.Equipment
import it.polito.mad.sportapp.model.Repository
import javax.inject.Inject

@HiltViewModel
class ReservationDetailsViewModel  @Inject constructor(
    private val repository: Repository
) : ViewModel() {

/*
    private val _reservation = MutableLiveData<DetailedReservation>().also {
        it.value = mockReservationDetails()
    }

 */


    private var  _reservation = MutableLiveData<DetailedReservation>()
    val reservation :LiveData<DetailedReservation> = _reservation

    fun getReservationFromDb(reservationId : Int) {

        // get reservation from database
        val dbThread = Thread {
            this._reservation.postValue(repository.getDetailedReservationById(reservationId))
        }

        // start db thread
        dbThread.start()
    }

    fun deleteReservation(): Boolean {
        println("DELETE RESERVATION!")
        //repository.deleteReservationById(_reservation.value.id)
        return true
    }

}

