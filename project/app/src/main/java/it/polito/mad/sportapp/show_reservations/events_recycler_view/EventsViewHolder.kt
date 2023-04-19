package it.polito.mad.sportapp.show_reservations.events_recycler_view

import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import it.polito.mad.sportapp.R
import it.polito.mad.sportapp.reservation_details.ReservationDetailsActivity
import it.polito.mad.sportapp.show_reservations.*

/* Event View Holder */

internal class EventsViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val itemLayout = view.findViewById<LinearLayout>(R.id.event_information_container)
    private val dateText = view.findViewById<TextView>(R.id.event_date_text)

    // event information
    private val sportName = view.findViewById<TextView>(R.id.event_sport_name)
    private val eventMoreInfo = view.findViewById<TextView>(R.id.event_more_info)
    private val eventDuration = view.findViewById<TextView>(R.id.event_duration)

    fun bind(event: Event) {

        dateText.text = eventDateTimeFormatter.format(event.time)

        val eventInfo = "${event.sportCenterName} - ${event.sportPlaygroundName}"

        // set event information
        sportName.text = event.sportName
        eventMoreInfo.text = eventInfo
        eventDuration.text = event.sportDuration

        // set item click listener
        itemLayout.setOnClickListener {

            //create the intent
            val intent = Intent(it.context, ReservationDetailsActivity::class.java)

            //start the Reservation Details Activity
            startActivity(it.context, intent, null)
        }

    }

}