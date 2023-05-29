package it.polito.mad.sportapp.model

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import it.polito.mad.sportapp.entities.Achievement
import it.polito.mad.sportapp.entities.NewReservation
import it.polito.mad.sportapp.entities.Notification
import it.polito.mad.sportapp.entities.User
import it.polito.mad.sportapp.entities.firestore.FireEquipment
import it.polito.mad.sportapp.entities.firestore.FireEquipmentReservationSlot
import it.polito.mad.sportapp.entities.firestore.FireNotification
import it.polito.mad.sportapp.entities.firestore.FirePlaygroundReservation
import it.polito.mad.sportapp.entities.firestore.FirePlaygroundSport
import it.polito.mad.sportapp.entities.firestore.FireReservationSlot
import it.polito.mad.sportapp.entities.firestore.FireUser
import it.polito.mad.sportapp.entities.firestore.FireUserForPlaygroundReservation
import it.polito.mad.sportapp.entities.firestore.utilities.DefaultGetFireError
import it.polito.mad.sportapp.entities.firestore.utilities.DefaultInsertFireError
import it.polito.mad.sportapp.entities.firestore.utilities.FireListener
import it.polito.mad.sportapp.entities.firestore.utilities.FireResult
import it.polito.mad.sportapp.entities.firestore.utilities.FireResult.*
import it.polito.mad.sportapp.entities.firestore.utilities.NewReservationError
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter


/* user */

internal fun FireRepository.getStaticUser(
    userId: String,
    fireCallback: (FireResult<User, DefaultGetFireError>) -> Unit
) {
    db.collection("users")
        .document(userId)
        .get()
        .addOnSuccessListener { document ->
            if (document == null || !document.exists()) {
                // no data exists
                fireCallback(
                    DefaultGetFireError.notFound(
                        "Error: User has not been found"
                    ))
                return@addOnSuccessListener
            }

            // * user exists *

            // deserialize data from db
            val fireUser = FireUser.deserialize(document.id, document.data)

            if (fireUser == null) {
                // deserialization error
                Log.d("deserialization error", "Error: a generic error occurred deserializing user with id $userId in FireRepository.getUser()")

                fireCallback(
                    DefaultGetFireError.duringDeserialization(
                    "Error: a generic error occurred retrieving user"
                ))
                return@addOnSuccessListener
            }

            // * user correctly retrieved *

            // transform to user entity
            val user = fireUser.toUser()

            // compute user achievements (statically)
            this.buildAchievements(userId) { result ->
                when (result) {
                    is Success -> {
                        // attach user achievements and return successfully
                        user.achievements = result.unwrap()
                        fireCallback(Success(user))
                    }
                    is Error -> {
                        fireCallback(Error(result.errorType()))
                    }
                }
            }
        }
        .addOnFailureListener {
            Log.d("default error", "Error: a generic error occurred retrieving user with id $userId in FireRepository.getStaticUser(). Message: ${it.message}")
            fireCallback(
                DefaultGetFireError.default(
                "Error: a generic error occurred retrieving user",
            ))
            return@addOnFailureListener
        }
}

// TODO
internal fun FireRepository.buildAchievements(
    userId: String,
    fireCallback: (FireResult<Map<Achievement, Boolean>, DefaultGetFireError>) -> Unit
) {
    // TODO: statically compute the real achievements of user $userId
    fireCallback(
        Success(
            mapOf(
                Achievement.AtLeastOneSport to false,
                Achievement.AtLeastFiveSports to false,
                Achievement.AllSports to false,
                Achievement.AtLeastThreeMatches to false,
                Achievement.AtLeastTenMatches to false,
                Achievement.AtLeastTwentyFiveMatches to false,
            )
        )
    )
}

/* reservations */

internal fun FireRepository.getReservationSlotsDocumentsReferences(
    reservationId: String?,
    fireCallback: (FireResult<List<DocumentReference>, NewReservationError>) -> Unit
) {
    if (reservationId == null) {
        fireCallback(Success(mutableListOf()))
        return
    }

    db.collection("reservationSlots")
        .whereEqualTo("reservationId", reservationId)
        .get()
        .addOnSuccessListener { res ->
            if (res == null) {
                // generic error
                Log.d("generic error", "Error: a generic error occurred retrieving reservation slots references in FireRepository.getReservationSlotsDocumentsReferences()")
                fireCallback(NewReservationError.unexpected())
                return@addOnSuccessListener
            }

            val reservationSlotsDocumentsRefs = res.map { it.reference }
            fireCallback(Success(reservationSlotsDocumentsRefs))
        }
        .addOnFailureListener {
            // generic error
            Log.d("generic error", "Error: a generic error occurred retrieving reservation slots references in FireRepository.getReservationSlotsDocumentsReferences(). Message: ${it.message}")
            fireCallback(NewReservationError.unexpected())
        }
}

internal fun FireRepository.getEquipmentReservationSlotsDocumentsReferences(
    reservationId: String?,
    fireCallback: (FireResult<List<DocumentReference>, NewReservationError>) -> Unit
) {
    if (reservationId == null) {
        fireCallback(Success(mutableListOf()))
        return
    }

    db.collection("equipmentReservationSlots")
        .whereEqualTo("playgroundReservationId", reservationId)
        .get()
        .addOnSuccessListener { res ->
            if (res == null) {
                // generic error
                Log.d("generic error", "Error: a generic error occurred retrieving equipment reservation slots references in FireRepository.getEquipmentReservationSlotsDocumentsReferences()")
                fireCallback(NewReservationError.unexpected())
                return@addOnSuccessListener
            }

            val equipmentReservationSlotsDocumentsRefs = res.map { it.reference }
            fireCallback(Success(equipmentReservationSlotsDocumentsRefs))
        }
        .addOnFailureListener {
            // generic error
            Log.d("generic error", "Error: a generic error occurred retrieving equipment reservation slots references in FireRepository.getEquipmentReservationSlotsDocumentsReferences(). Message: ${it.message}")
            fireCallback(NewReservationError.unexpected())
        }
}

internal fun FireRepository.checkSlotsAvailabilities(
    reservation: NewReservation,
    fireCallback: (FireResult<Unit, NewReservationError>) -> Unit
) {
    // search for any busy slot, of that playground, contained in
    // [reservation.StartTime, reservation.EndTime],
    // excluding the ones of the reservation itself (if any)
    db.collection("reservationSlots")
        .whereEqualTo("playgroundId", reservation.playgroundId)
        .whereGreaterThanOrEqualTo("startSlot", reservation.startTime.format(DateTimeFormatter.ISO_DATE_TIME))
        .whereLessThanOrEqualTo("endSlot", reservation.endTime.format(DateTimeFormatter.ISO_DATE_TIME))
        .get()
        .addOnSuccessListener { res ->
            if (res == null) {
                // generic error
                Log.d(
                    "generic error",
                    "Error: a generic error occurred retrieving reservationSlots in FireRepository.checkSlotsAvailabilities()"
                )
                fireCallback(NewReservationError.unexpected())
                return@addOnSuccessListener
            }

            // deserialize reservation slots
            var reservationSlotsDocuments = res.map { rawDocument ->
                val deserializedDocument =
                    FireReservationSlot.deserialize(rawDocument.id, rawDocument.data)

                if (deserializedDocument == null) {
                    // deserialization error
                    Log.d(
                        "deserialization error",
                        "Error: an error occurred deserializing reservation slot $rawDocument in FireRepository.checkSlotsAvailabilities()"
                    )
                    fireCallback(NewReservationError.unexpected())
                    return@addOnSuccessListener
                }

                deserializedDocument
            }

            // filter slots occupied by the current existing reservation, if any
            reservationSlotsDocuments = reservationSlotsDocuments.filter { doc ->
                doc.reservationId != reservation.id
            }

            // if any slots exists, they are *busy*, so there is a conflict in the reservation booking
            if (reservationSlotsDocuments.isNotEmpty()) {
                // slots conflict: the slots you are trying to reserve are already busy!
                Log.d(
                    "conflict error",
                    "A conflict emerged checking available slots, in FireRepository.overrideNewReservation()"
                )
                fireCallback(NewReservationError.slotConflict())
                return@addOnSuccessListener
            }

            // * slots are available *
            fireCallback(Success(Unit))
            return@addOnSuccessListener
        }
        .addOnFailureListener {
            // generic error
            Log.d("generic error", "Error: a generic error occurred retrieving reservationSlots in FireRepository.checkSlotsAvailabilities(). Message: ${it.message}")
            fireCallback(NewReservationError.unexpected())
            return@addOnFailureListener
        }
}

internal fun FireRepository.checkEquipmentsAvailabilities(
    reservation: NewReservation,
    fireCallback: (FireResult<Unit, NewReservationError>) -> Unit
) {
    // check for equipments availability: retrieve all the equipments slots
    // in [reservation.startTime, reservation.endTime] related to the requested equipments,
    // excluding the slots of this reservation (if any); then, for each slot and equipments
    // compute the remaining qty, and check if it is < of the requested one; in this case,
    // the equipments are NOT available for the new reservation
    db.collection("equipmentReservationSlots")
        .whereGreaterThanOrEqualTo("startSlot", reservation.startTime.format(DateTimeFormatter.ISO_DATE_TIME))
        .whereLessThanOrEqualTo("endSlot", reservation.endTime.format(DateTimeFormatter.ISO_DATE_TIME))
        .whereIn("equipment.id", reservation.selectedEquipments.map { it.equipmentId })
        .get()
        .addOnSuccessListener { result ->
            if (result == null) {
                // generic error
                Log.d("generic error", "Error: a generic error occurred retrieving equipment reservation slots in FireRepository.checkEquipmentsAvailabilities()")
                fireCallback(NewReservationError.unexpected())
                return@addOnSuccessListener
            }

            // deserialize equipment reservation slots documents
            var equipmentReservationSlotsDocs = result.map { doc ->
                val equipmentReservationSlotDoc = FireEquipmentReservationSlot.deserialize(doc.id, doc.data)

                if(equipmentReservationSlotDoc == null) {
                    // deserialization error
                    Log.d("deserialization error", "Error: an error occurred deserializing equipment reservation slot $doc in FireRepository.checkEquipmentsAvailabilities()")
                    fireCallback(NewReservationError.unexpected())
                    return@addOnSuccessListener
                }

                equipmentReservationSlotDoc
            }

            // filter the slots of the current reservation, if any
            equipmentReservationSlotsDocs = equipmentReservationSlotsDocs.filter { doc ->
                doc.playgroundReservationId != reservation.id
            }

            val selectedEquipmentsQuantities = reservation.selectedEquipments.associate {
                Pair(it.equipmentId, it.selectedQuantity)
            }

            // check if in each slots, selected quantities are available
            equipmentReservationSlotsDocs.groupBy { equipmentReservationSlot ->
                equipmentReservationSlot.startSlot
            }.forEach { (_, equipmentReservations) ->
                equipmentReservations.groupBy(
                    // group by equipment id
                    { it.equipment.id },
                    { Pair(it.selectedQuantity, it.equipment.maxQuantity) }
                ).forEach { (equipmentId, quantities) ->
                    // for each equipment (in each slot) compute the available qty
                    // check if the selected qty exceeds the available one; if so, return equipment conflict error
                    val totalOccupiedQty = quantities.sumOf { (selectedQty, _) -> selectedQty }
                    val maxQty = quantities[0].second

                    // compute available quantity
                    val availableQty = maxQty - totalOccupiedQty

                    val selectedQty = selectedEquipmentsQuantities[equipmentId]!!   // equipment must be there

                    if(selectedQty > availableQty) {
                        // selected qty for this reservation *exceeds* the available one -> equipment conflict
                        Log.d("equipment conflict", "Error: selected qty $selectedQty exceeds available qty $availableQty for equipment $equipmentId, in FireRepository.checkEquipmentsAvailabilities()")
                        fireCallback(NewReservationError.equipmentConflict())
                        return@addOnSuccessListener
                    }
                }
            }

            // * selected equipments are available *

            fireCallback(Success(Unit))
            return@addOnSuccessListener
        }
        .addOnFailureListener {
            // generic error
            Log.d("generic error", "Error: a generic error occurred retrieving equipment reservation slots in FireRepository.checkEquipmentsAvailabilities(). Message: ${it.message}")
            fireCallback(NewReservationError.unexpected())
            return@addOnFailureListener
        }
}

internal fun FireRepository.saveNewReservationDataInBatch(
    reservationSlotsDocumentsRefs: List<DocumentReference>,
    equipmentReservationSlotsDocumentsRefs: List<DocumentReference>,
    reservation: NewReservation,
    user: User,
    sportPlaygrounds: List<FirePlaygroundSport>,
    reservationEquipmentsById: Map<String, FireEquipment>,
    fireCallback: (FireResult<String, DefaultInsertFireError>) -> Unit
) {
    db.runTransaction { transaction ->
        // (3) delete old reservation slots (if any)
        reservationSlotsDocumentsRefs.forEach(transaction::delete)

        // (4) delete old equipment reservation slots (if any)
        equipmentReservationSlotsDocumentsRefs.forEach(transaction::delete)

        // (5) create or update existing playground reservation
        val firePlaygroundReservation = FirePlaygroundReservation.fromNewReservation(
            reservation,
            FireUserForPlaygroundReservation.from(user)
        )

        val newReservationId = if (reservation.id != null) {
            // update existing playground reservation
            val docRef = db.collection("playgroundReservations").document(reservation.id)

            // update everything except for user and participants
            val dataToUpdate = firePlaygroundReservation.serialize()
                .toMutableMap()

            dataToUpdate.remove("user")
            dataToUpdate.remove("participants")

            transaction.update(docRef, dataToUpdate)

            reservation.id
        }
        else {
            // set new playground reservation
            val newDocRef = db.collection("playgroundReservations").document()
            transaction.set(newDocRef, firePlaygroundReservation.serialize())

            newDocRef.id
        }

        // (6) save new reservation slots
        val newSlots = FireReservationSlot.slotsFromNewReservation(
            reservation,
            newReservationId
        )

        // fill these slots' open playgrounds ids
        newSlots.forEach { fireReservationSlot ->
            fireReservationSlot.openPlaygroundsIds = sportPlaygrounds.filter { playground ->
                val openingTime = LocalTime.parse(playground.sportCenter.openingHours)
                val closingTime = LocalTime.parse(playground.sportCenter.closingHours)
                val startSlot = LocalDateTime.parse(fireReservationSlot.startSlot).toLocalTime()
                val endSlot = LocalDateTime.parse(fireReservationSlot.endSlot).toLocalTime()

                startSlot >= openingTime && endSlot <= closingTime
            }
                .map { playground -> playground.id }
        }

        newSlots.forEach { newSlot ->
            val newDocRef = db.collection("reservationSlots").document()
            transaction.set(newDocRef, newSlot.serialize())
        }

        // (7) save new equipment reservation slots
        val newEquipmentSlots = FireEquipmentReservationSlot.slotsFromNewReservation(
            reservation,
            newReservationId,
            reservationEquipmentsById
        )

        newEquipmentSlots.forEach { newSlot ->
            val newDocRef = db.collection("equipmentReservationSlots").document()
            transaction.set(newDocRef, newSlot.serialize())
        }

        // every save has been scheduled -> return new assigned reservation id

        newReservationId
    }.addOnSuccessListener { newReservationId ->
        // * everything was successfully saved *
        fireCallback(Success(newReservationId))
    }.addOnFailureListener {
        // generic error
        Log.d("generic error", "Error: a generic error occurred saving new reservation data in FireRepository.saveNewReservationDataInBatch(). Message: ${it.message}")
        fireCallback(
            DefaultInsertFireError.default(
            "Error: an unexpected error occurred saving the reservation"
        ))
        return@addOnFailureListener
    }
}

internal fun FireRepository.getReservationEquipmentsById(
    reservation: NewReservation,
    fireCallback: (FireResult<Map<String, FireEquipment>, DefaultGetFireError>) -> Unit
) {
    db.collection("equipments")
        .whereEqualTo("sportId", reservation.sportId)
        .whereEqualTo("sportCenterId", reservation.sportCenterId)
        .get()
        .addOnSuccessListener { res ->
            if(res == null) {
                // generic error
                Log.d("generic error", "Error: a generic error occurred retrieving equipments in FireRepository.getReservationEquipmentsById()")
                fireCallback(DefaultGetFireError.default(
                    "Error: a generic error occurred retrieving equipments"
                ))
                return@addOnSuccessListener
            }

            val equipmentsDocuments = res.map { doc ->
                val deserializedDoc = FireEquipment.deserialize(doc.id, doc.data)

                if(deserializedDoc == null) {
                    // deserialization error
                    Log.d("deserialization error", "Error: an error occurred deserializing equipment $doc in FireRepository.getReservationEquipmentsById()")
                    fireCallback(DefaultGetFireError.duringDeserialization(
                        "Error: an error occurred retrieving equipments"
                    ))
                    return@addOnSuccessListener
                }

                deserializedDoc
            }

            val ids = reservation.selectedEquipments.map { it.equipmentId }

            val selectedEquipmentsDocByIds = equipmentsDocuments.filter {
                ids.contains(it.id)
            }.associateBy { it.id }


            fireCallback(Success(selectedEquipmentsDocByIds))
        }
        .addOnFailureListener {
            // generic error
            Log.d("generic error", "Error: a generic error occurred retrieving equipments in FireRepository.getReservationEquipmentsById(). Message: ${it.message}")
            fireCallback(DefaultGetFireError.default(
                "Error: a generic error occurred retrieving equipments"
            ))
            return@addOnFailureListener
        }
}

internal fun FireRepository.getPlaygroundReservationsOfUser(
    user: FireUserForPlaygroundReservation,
    fireCallback: (FireResult<List<FirePlaygroundReservation>, DefaultGetFireError>) -> Unit
): FireListener {
    val listener = db.collection("playgroundReservations")
        .whereArrayContains("participants", user.serialize())
        .addSnapshotListener { value, error ->
            if (error != null || value == null) {
                // generic error
                Log.d("generic error", "Error: a generic error occurred retrieving all the user playground reservations, for user $user, in FireRepository.getPlaygroundReservationsOfUser()")
                fireCallback(DefaultGetFireError.default(
                    "Error: a generic error occurred retrieving user reservations"
                ))
                return@addSnapshotListener
            }

            val userReservations = value.map { doc ->
                val deserializedDoc = FirePlaygroundReservation.deserialize(doc.id, doc.data)

                if (deserializedDoc == null) {
                    // deserialization error
                    Log.d("deserialization error", "Error: an error occurred deserializing playground reservation $doc in FireRepository.getPlaygroundReservationsOfUser(${user.id})")
                    fireCallback(DefaultGetFireError.default(
                        "Error: an error occurred retrieving user reservations"
                    ))
                    return@addSnapshotListener
                }

                deserializedDoc
            }

            fireCallback(Success(userReservations))
        }

    return FireListener(listener)
}

/* playgrounds */

internal fun FireRepository.getPlaygroundsBySportId(
    sportId: String,
    fireCallback: (FireResult<List<FirePlaygroundSport>, DefaultGetFireError>) -> Unit
) {
    db.collection("playgroundSports")
        .whereEqualTo("sport.id", sportId)
        .get()
        .addOnSuccessListener { res ->
            if (res == null) {
                // generic error
                Log.d("generic error", "Error: generic error occurred retrieving playground sports with sport id $sportId in FireRepository.getPlaygroundsBySportId()")
                fireCallback(DefaultGetFireError.default(
                    "Error: a generic error occurred retrieving playgrounds"
                ))
                return@addOnSuccessListener
            }

            val playgroundsDocs = res.map {
                val deserializedDoc = FirePlaygroundSport.deserialize(it.id, it.data)

                if(deserializedDoc == null) {
                    // deserialization error
                    Log.d("deserialization error", "Error: deserialization error occurred deserializing playground sport $it in FireRepository.getPlaygroundsBySportId()")
                    fireCallback(DefaultGetFireError.default(
                        "Error: a generic error occurred retrieving playgrounds"
                    ))
                    return@addOnSuccessListener
                }

                deserializedDoc
            }

            fireCallback(Success(playgroundsDocs))
        }
        .addOnFailureListener {
            // generic error
            Log.d("generic error", "Error: generic error occurred retrieving playground sports with sport id $sportId in FireRepository.getPlaygroundsBySportId(). Message: ${it.message}")
            fireCallback(DefaultGetFireError.default(
                "Error: a generic error occurred retrieving playgrounds"
            ))
        }
}

internal fun FireRepository.getPlaygroundsByIds(
    ids: List<String>,
    fireCallback: (FireResult<List<FirePlaygroundSport>, DefaultGetFireError>) -> Unit
) {
    db.collection("playgroundSports")
        .whereIn(FieldPath.documentId(), ids)
        .get()
        .addOnSuccessListener { res ->
            if(res == null) {
                // generic error
                Log.d("generic error", "Error: a generic error occurred retrieving all the reservations playgrounds with ids:$ids, in FireRepository.getPlaygroundsBySportId()")
                fireCallback(DefaultGetFireError.default(
                    "Error: an error occurred retrieving playgrounds info"
                ))
                return@addOnSuccessListener
            }

            val playgroundSportsDocs = res.map { doc ->
                val deserializedDoc = FirePlaygroundSport.deserialize(doc.id, doc.data)

                if (deserializedDoc == null) {
                    // deserialization error
                    Log.d("deserialization error", "Error: an error occurred deserializing playground sport $doc in FireRepository.getPlaygroundsByIds()")
                    fireCallback(DefaultGetFireError.duringDeserialization(
                        "Error: an error occurred retrieving playgrounds info"
                    ))
                    return@addOnSuccessListener
                }

                deserializedDoc
            }

            fireCallback(Success(playgroundSportsDocs))
        }
        .addOnFailureListener {
            // generic error
            Log.d("generic error", "Error: a generic error occurred retrieving all the reservations playgrounds with ids:$ids, in FireRepository.getPlaygroundsBySportId(). Message: ${it.message}")
            fireCallback(DefaultGetFireError.default(
                "Error: an error occurred retrieving playgrounds info"
            ))
            return@addOnFailureListener
        }
}

/* notifications */

internal fun FireRepository.saveInvitation(
    notification: Notification,
    fireCallback: (FireResult<Unit, DefaultInsertFireError>) -> Unit
) {
    // convert notification entity to fireNotification
    val fireNotification = FireNotification.from(notification)

    if(fireNotification == null) {
        // conversion error
        Log.d("conversion error", "Error: an error occurred converting a notification entity in a fireNotification, in FireRepository.saveInvitation()")
        fireCallback(DefaultInsertFireError.duringSerialization(
            "Error: an error occurred saving the invitation"))
        return
    }

    // save notification document in the collection
    db.collection("notifications")
        .document()
        .set(fireNotification.serialize())
        .addOnSuccessListener {
            // * save was successful *
            fireCallback(Success(Unit))
        }
        .addOnFailureListener {
            // generic error
            Log.d("generic error", "Error: a generic error occured saving invitation $notification in FireRepository.saveInvitation(). Message: ${it.message}")
            fireCallback(DefaultInsertFireError.default(
                "Error: a generic error occurred saving the invitation"))
            return@addOnFailureListener
        }
}

internal fun FireRepository.createInvitationNotification(
    receiverToken: String,
    reservationId: String,
    notificationDescription: String,
    notificationTimestamp: String,
    fireCallback: (FireResult<Unit,DefaultInsertFireError>) -> Unit
) {
    // notification variables
    val tag = "NOTIFICATION TAG"
    val notificationTitle = "New Invitation"

    val notification = JSONObject()
    val notificationBody = JSONObject()

    try {
        // create notification body
        notificationBody.put("action", "invitation")
        notificationBody.put("title", notificationTitle)
        notificationBody.put("message", notificationDescription)
        notificationBody.put("id_reservation", reservationId)
        notificationBody.put("status", "PENDING")
        notificationBody.put("timestamp", notificationTimestamp)

        // create notification
        notification.put("to", receiverToken)
        notification.put("data", notificationBody)
    } catch (e: JSONException) {
        Log.e(tag, "createInvitationNotification function: " + e.message)

        Log.d("serialization error", "Error: a generic error occurred serializing notification JSON in FireRepository.createInvitationNotification()")
        fireCallback(DefaultInsertFireError.default(
            "Error: an error occurred sending the notification"
        ))
        return
    }

    // send notification
    sendInvitationNotification(notification, fireCallback)
}

private fun sendInvitationNotification(
    notification: JSONObject,
    fireCallback: (FireResult<Unit,DefaultInsertFireError>) -> Unit
) {

    // API variables
    val fcmAPI = "https://fcm.googleapis.com/fcm/send"
    val serverKey =
        "key=" + "AAAAEgeVTRw:APA91bH_I9ilwfS5o7n3U45BdKy2TQiHlBEqzbP0hONdx7IFbn1PgZdIEOk3GoMSVpQWGzKJ4so5ax50wW7hHFBuZsyVXcgp8hyM3EAqZtzSn99F5ntvV4aDht3Zl4TK5bwoWipF_9IH"
    val contentType = "application/json"

    // create request
    val request = Request.Builder()
        .url(fcmAPI)
        .post(RequestBody.create(MediaType.parse(contentType), notification.toString()))
        .addHeader("Authorization", serverKey)
        .build()

    // Send the request
    val client = OkHttpClient()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("SEND INVITATION NOTIFICATION", "Notification sending failed! ${e.message}")
            fireCallback(DefaultInsertFireError.default(
                "Error: an error occurred sending the notification"
            ))
        }

        override fun onResponse(call: Call, response: Response) {
            // Handle request success
            if (response.isSuccessful) {
                Log.i("SEND INVITATION NOTIFICATION", "Notification successfully sent!")
                // * push notification successfully sent *
                fireCallback(Success(Unit))
            } else {
                Log.e("SEND INVITATION NOTIFICATION", "Notification sending failed!")
                // push notification not sent
                fireCallback(DefaultInsertFireError.default(
                    "Error: an error occurred sending the notification"
                ))
            }
        }
    })
}

