package it.polito.mad.sportapp.reservation_details

import android.content.Intent
import android.graphics.drawable.DrawableContainer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import it.polito.mad.sportapp.R
import it.polito.mad.sportapp.entities.Equipment
import it.polito.mad.sportapp.entities.EquipmentReservation
import it.polito.mad.sportapp.showToasty

@AndroidEntryPoint
class EditEquipmentActivity : AppCompatActivity() {

    private lateinit var reservationNumberTitle : TextView
    private lateinit var selectedEquipmentContainer : LinearLayout
    private lateinit var availableEquipmentContainer: LinearLayout
    private lateinit var newPrice : TextView
    private lateinit var cancelButton: Button
    private lateinit var saveButton: Button

    private var eventId : Int = -1

    private val vm by viewModels<EditEquipmentViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_equipment)

        eventId = intent.getIntExtra("id_event", -1)

        //initViews()

        vm.reservation.observe(this) {
            if(vm.reservation.value != null) {
                if(vm.reservation.value != null) {
                    vm.getEquipmentFromDb(vm.reservation.value!!)
                    initViews()
                    initButtons()
                }
            }
        }
        vm.tempSelectedEquipment.observe(this) {
            if(vm.reservation.value != null) {
                loadSelectedEquipment()
                selectedEquipmentContainer.requestLayout()
            }
        }
        vm.tempAvailableEquipment.observe(this) {
            if(vm.reservation.value != null) {
                loadAvailableEquipment()
                availableEquipmentContainer.requestLayout()
            }
        }
        vm.tempPrice.observe(this) {
                updatedPrice ->
            if(vm.reservation.value != null) {
                newPrice.text = String.format("%.2f", updatedPrice)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if(eventId != -1)
            vm.getReservationFromDb(eventId)

    }

    private fun initViews() {
        reservationNumberTitle = findViewById(R.id.reservationNumber)
        selectedEquipmentContainer = findViewById(R.id.selectedEquipmentContainer)
        availableEquipmentContainer = findViewById(R.id.availableEquipmentContainer)
        newPrice = findViewById(R.id.newPrice)
        cancelButton = findViewById(R.id.cancelButton)
        saveButton = findViewById(R.id.saveButton)

        reservationNumberTitle.text = "Reservation number: " + String.format("%010d", vm.reservation.value?.id)
        newPrice.text = String.format("%.2f", vm.tempPrice.value)

        loadSelectedEquipment()
        loadAvailableEquipment()
        initButtons()
    }

    private fun loadAvailableEquipment() {
        availableEquipmentContainer.removeAllViewsInLayout()
        if(vm.tempAvailableEquipment.value != null) {
            for ((index, e) in vm.tempAvailableEquipment.value!!.withIndex()) {

                var row = layoutInflater.inflate(
                    R.layout.available_equipment,
                    availableEquipmentContainer,
                    false
                )
                row.id = index
                val equipmentName = row.findViewById<TextView>(R.id.equipmentName)
                val remainingQuantity = row.findViewById<TextView>(R.id.remainingQty)
                val equipmentPrice = row.findViewById<TextView>(R.id.equipmentPrice)

                equipmentName.text = e.name
                remainingQuantity.text =  String.format("%d", e.availability)
                equipmentPrice.text = String.format("%.2f", e.price)

                val button = row.findViewById<ImageButton>(R.id.addEquipmentButton)
                button.setOnClickListener {
                    vm.addEquipment(vm.reservation.value?.id!!, e.id, e.price)
                }

                availableEquipmentContainer.addView(row)
            }
        }
    }

    private fun loadSelectedEquipment() {
        if(vm.tempSelectedEquipment.value != null && vm.tempSelectedEquipment.value!!.isNotEmpty()) {
            selectedEquipmentContainer.removeAllViewsInLayout()
            for ((index, e) in vm.reservation.value?.equipments!!.withIndex() ){
                var row = layoutInflater.inflate(R.layout.selected_equipment, selectedEquipmentContainer, false)
                row.id = index
                val equipmentName = row.findViewById<TextView>(R.id.equipmentName)
                val equipmentQuantity = row.findViewById<TextView>(R.id.equipmentQuantity)
                val equipmentPrice = row.findViewById<TextView>(R.id.equipmentPrice)

                equipmentName.text = e.equipmentName
                equipmentQuantity.text = String.format("%d", e.quantity)
                equipmentPrice.text = String.format("%.2f", e.totalPrice)

                val buttonPlus = row.findViewById<ImageButton>(R.id.plusButton)
                buttonPlus.setOnClickListener {
                    if(vm.incrementQuantity(e.equipmentId)){
                       loadSelectedEquipment()
                    }else{
                        Toast.makeText(this, "Every available Equipment ${e.equipmentId} is booked!", Toast.LENGTH_SHORT).show()
                    }
                }

                val buttonMinus = row.findViewById<ImageButton>(R.id.minusButton)
                buttonMinus.setOnClickListener {
                    vm.decrementQuantity(e.equipmentId)
                }

                selectedEquipmentContainer.addView(row, index)
            }
        } else {
            //TODO
            //noEquipmentMessage.visibility = TextView.VISIBLE
        }
    }

    private fun initButtons() {
        cancelButton.setOnClickListener {
            val intent = Intent(this, ReservationDetailsActivity::class.java)
            intent.putExtra("id_event", eventId)
            startActivity(intent)
        }

        saveButton.setOnClickListener {
            vm.saveEquipment()
            showToasty("success", this, "Information correctly saved!")
            val intent = Intent(this, ReservationDetailsActivity::class.java)
            intent.putExtra("id_event", eventId)
            startActivity(intent)
        }
    }
}