package it.polito.mad.sportapp.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polito.mad.sportapp.entities.Achievement
import it.polito.mad.sportapp.entities.Sport
import it.polito.mad.sportapp.entities.SportLevel
import it.polito.mad.sportapp.entities.User
import it.polito.mad.sportapp.model.LocalRepository
import javax.inject.Inject

/* View Model related to the Profile Fragments */

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: LocalRepository
) : ViewModel() {

    /* sports flags */
    private val _userSportsLoaded = MutableLiveData<Boolean>().also { it.value = false }
    val userSportsLoaded: LiveData<Boolean> = _userSportsLoaded

    private val _sportsListLoaded = MutableLiveData<Boolean>().also { it.value = false }
    val sportsListLoaded: LiveData<Boolean> = _sportsListLoaded

    private val _sportsInflated = MutableLiveData<Boolean>().also { it.value = false }
    val sportsInflated: LiveData<Boolean> = _sportsInflated

    /* user information */

    private val _usernameAlreadyExists = MutableLiveData<Boolean>().also { it.value = false }
    val usernameAlreadyExists: LiveData<Boolean> = _usernameAlreadyExists

    private val _userFirstName = MutableLiveData<String>().also { it.value = "John" }
    val userFirstName: LiveData<String> = _userFirstName

    private val _userLastName = MutableLiveData<String>().also { it.value = "Doe" }
    val userLastName: LiveData<String> = _userLastName

    private val _userUsername = MutableLiveData<String>().also { it.value = "johndoe" }
    val userUsername: LiveData<String> = _userUsername

    private val _userGender = MutableLiveData<String>().also { it.value = "Male" }
    val userGender: LiveData<String> = _userGender

    private val _userAge = MutableLiveData<String>().also { it.value = 25.toString() }
    val userAge: LiveData<String> = _userAge

    private val _userLocation = MutableLiveData<String>().also { it.value = "Rome" }
    val userLocation: LiveData<String> = _userLocation

    private val _userBio = MutableLiveData<String>().also {
        it.value =
            "I’m a Computer Engineering student from Latina. I love playing basketball and tennis with my friends, especially on the weekend."
    }
    val userBio: LiveData<String> = _userBio

    private val _userAchievements =
        MutableLiveData<Map<Achievement, Boolean>>().also { it.value = mapOf() }
    val userAchievements: LiveData<Map<Achievement, Boolean>> = _userAchievements

    private val _userSports = MutableLiveData<List<SportLevel>>().also { it.value = listOf() }
    val userSports: LiveData<List<SportLevel>> = _userSports

    /* sports information */
    private val _sportsList = MutableLiveData<List<Sport>>()
    val sportsList: LiveData<List<Sport>> = _sportsList

    fun loadSportsFromDb() {
        // get list of sports from database
        val dbThread = Thread {
            val sports = repository.getAllSports()
            _sportsList.postValue(sports)

            // update db flag
            _sportsListLoaded.postValue(true)
        }

        // start db thread
        dbThread.start()
    }

    // check if username is unique
    fun checkUsername(username: String) {
        val dbThread = Thread {
            val usernameAlreadyExists = repository.usernameAlreadyExists(username)
            _usernameAlreadyExists.postValue(usernameAlreadyExists)
        }
        dbThread.start()
    }

    // load user information from database
    fun loadUserInformationFromDb(userId: Int) {

        // get user information from database
        val dbThread = Thread {
            val user = repository.getUser(userId)

            // update user information
            _userFirstName.postValue(user.firstName)
            _userLastName.postValue(user.lastName)
            _userUsername.postValue(user.username)
            _userGender.postValue(user.gender)
            _userAge.postValue(user.age.toString())
            _userLocation.postValue(user.location)
            _userBio.postValue(user.bio)
            _userAchievements.postValue(user.achievements)
            _userSports.postValue(user.sportLevel)

            // update db flag
            _userSportsLoaded.postValue(true)
        }

        // start db thread
        dbThread.start()
    }

    // update user information in database
    fun updateDbUserInformation(userId: Int) {

        // set user information
        val user = User(
            userId,
            _userFirstName.value!!,
            _userLastName.value!!,
            _userUsername.value!!,
            _userGender.value!!,
            _userAge.value?.toInt()!!,
            _userLocation.value!!,
            _userBio.value!!
        )

        user.sportLevel = _userSports.value!!

        // update user information in database
        val dbThread = Thread {
            repository.updateUser(user)
        }

        // start db thread
        dbThread.start()
    }

    /* user information setters */
    fun setUserFirstName(firstName: String) {
        _userFirstName.value = firstName
    }

    fun setUserLastName(lastName: String) {
        _userLastName.value = lastName
    }

    fun setUserUsername(username: String) {
        _userUsername.value = username
    }

    fun setUserGender(gender: String) {
        _userGender.value = gender
    }

    fun setUserAge(age: String) {
        _userAge.value = age
    }

    fun setUserLocation(location: String) {
        _userLocation.value = location
    }

    fun setUserBio(bio: String) {
        _userBio.value = bio
    }

    fun setUserSports(sports: List<SportLevel>) {
        _userSports.value = sports
    }

    fun setSportsInflated(value: Boolean) {
        _sportsInflated.value = value
    }

}