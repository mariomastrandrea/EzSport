package it.polito.mad.sportapp.reservation_details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polito.mad.sportapp.entities.DetailedReservation
import it.polito.mad.sportapp.model.Repository
import javax.inject.Inject

@HiltViewModel
class ReservationDetailsViewModel  @Inject constructor(
    repository: Repository
) : ViewModel() {

    private val _reservation = MutableLiveData<DetailedReservation>().also {
        it.value = mockReservationDetails()
    }

    //set to true when the user is adding equipments
    private var dirty = false

    val reservation :LiveData<DetailedReservation> = _reservation
}
