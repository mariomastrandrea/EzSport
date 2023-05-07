package it.polito.mad.sportapp.playground_availabilities

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import it.polito.mad.sportapp.R
import it.polito.mad.sportapp.playground_availabilities.calendar_utils.CalendarDayBinder
import it.polito.mad.sportapp.playground_availabilities.calendar_utils.MonthViewContainer
import it.polito.mad.sportapp.playground_availabilities.recycler_view.PlaygroundAvailabilitiesAdapter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.min


/* calendar init functions */
internal fun PlaygroundAvailabilitiesFragment.initCalendar() {
    /* retrieve the calendar view */
    calendarView = requireView().findViewById(R.id.calendar_view)

    val daysOfWeek = daysOfWeek(firstDayOfWeek=DayOfWeek.MONDAY)

    /* initialize calendar header */
    this.initCalendarHeader(daysOfWeek)

    /* initialize month days */
    this.initCalendarDays()

    /* setup calendar */
    viewModel.currentMonth.value?.let {
        val startMonth = it.minusMonths(100)
        val endMonth = it.plusMonths(100)

        calendarView.setup(startMonth, endMonth, daysOfWeek.first())
        calendarView.scrollToMonth(it)
    }
}

private fun PlaygroundAvailabilitiesFragment.initCalendarHeader(daysOfWeek: List<DayOfWeek>) {
    /* initialize calendar header with days of week */
    calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
        override fun create(view: View) = MonthViewContainer(view as ViewGroup)

        override fun bind(container: MonthViewContainer, data: CalendarMonth) {
            // execute this just once
            if (container.viewGroup.tag == null) {
                container.viewGroup.tag = data.yearMonth

                container.viewGroup.children.zip(
                    daysOfWeek.asSequence()).forEach { (dayTextView, dayOfWeek) ->
                    val weekDayName = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    (dayTextView as TextView).text = weekDayName
                }
            }
        }
    }
}

private fun PlaygroundAvailabilitiesFragment.initCalendarDays() {
    // create and initialize day binder
    val calendarDayBinder = CalendarDayBinder(
        requireContext(),
        viewModel.selectedDate,
        viewModel::setSelectedDate,
        this::getAvailabilityPercentageOf
    )

    // attach day binder to the calendar view
    calendarView.dayBinder = calendarDayBinder
}

internal fun PlaygroundAvailabilitiesFragment.initCalendarMonthButtons() {
    val previousMonthButton = requireView().findViewById<ImageView>(R.id.previous_month_button)
    val nextMonthButton = requireView().findViewById<ImageView>(R.id.next_month_button)

    previousMonthButton.setOnClickListener {
        viewModel.currentMonth.value?.let {
            val previousMonth = it.minusMonths(1)
            calendarView.smoothScrollToMonth(previousMonth)
        }
    }

    nextMonthButton.setOnClickListener {
        viewModel.currentMonth.value?.let {
            val nextMonth = it.plusMonths(1)
            calendarView.smoothScrollToMonth(nextMonth)
        }
    }

    // change selected month if user swipes to another month
    calendarView.monthScrollListener = { newMonth ->
        viewModel.setCurrentMonth(newMonth.yearMonth)
    }
}

/* selected sport spinner */
internal fun PlaygroundAvailabilitiesFragment.initSelectedSportSpinner() {
    // create the spinner adapter
    selectedSportSpinnerAdapter = ArrayAdapter(
        requireContext(),
        android.R.layout.simple_spinner_item,
        viewModel.sports.value ?: mutableListOf()
    ).also {
        it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    // mount the adapter in the selected sport spinner
    selectedSportSpinner = requireView().findViewById(R.id.selected_sport_spinner)
    selectedSportSpinner.adapter = selectedSportSpinnerAdapter

    // specify the spinner callback to call at each user selection
    selectedSportSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            // retrieve selected option
            val justSelectedSport = selectedSportSpinnerAdapter.getItem(position)
            // update selected sport
            viewModel.setSelectedSport(justSelectedSport)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            viewModel.setSelectedSport(null)
        }
    }
}

/* observers init functions */
@SuppressLint("NotifyDataSetChanged")
internal fun PlaygroundAvailabilitiesFragment.initMonthAndDateObservers() {
    /* months view model observers */
    val monthLabel = requireView().findViewById<TextView>(R.id.month_label)

    viewModel.currentMonth.observe(this) { newMonth ->
        // change month label
        monthLabel.text = capitalize(
            newMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)))

        // retrieve new playground availabilities for the current month and the current sport
        viewModel.updatePlaygroundAvailabilitiesForCurrentMonthAndSport()
    }

    /* dates view model observers */
    val selectedDateLabel = requireView().findViewById<TextView>(R.id.selected_date_label)

    viewModel.selectedDate.observe(this) {
        // update calendar selected date
        calendarView.notifyDateChanged(it)

        // update selected date label
        selectedDateLabel.text = it.format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM y", Locale.ENGLISH))

        // update recycler view data
        playgroundAvailabilitiesAdapter.selectedDate = it
        playgroundAvailabilitiesAdapter.playgroundAvailabilities =
            viewModel.getAvailablePlaygroundsOnSelectedDate()

        // update time slots view
        playgroundAvailabilitiesAdapter.notifyDataSetChanged()
    }

    viewModel.previousSelectedDate.observe(this) {
        calendarView.notifyDateChanged(it)
    }
}

@SuppressLint("NotifyDataSetChanged")
internal fun PlaygroundAvailabilitiesFragment.initAvailablePlaygroundsObserver() {
    /* playground availabilities observer to change dates' colors */
    viewModel.availablePlaygroundsPerSlot.observe(this) {
        // * update calendar dates' dots *

        // update current, previous and next months
        val currentMonth = viewModel.currentMonth.value ?: viewModel.defaultMonth
        val previousMonth = currentMonth.minusMonths(1)
        val nextMonth = currentMonth.plusMonths(1)

        calendarView.notifyMonthChanged(currentMonth)
        calendarView.notifyMonthChanged(previousMonth)
        calendarView.notifyMonthChanged(nextMonth)

        // update recycler view data
        playgroundAvailabilitiesAdapter.playgroundAvailabilities =
            viewModel.getAvailablePlaygroundsOnSelectedDate()

        // show changes on recycler view
        playgroundAvailabilitiesAdapter.notifyDataSetChanged()
    }
}

internal fun PlaygroundAvailabilitiesFragment.initSelectedSportObservers() {
    // sports observer
    viewModel.sports.observe(this) {
        // add new sports list
        selectedSportSpinnerAdapter.addAll(it)

        // select first sport option
        selectedSportSpinner.setSelection(0)
    }

    // selected sport observer
    viewModel.selectedSport.observe(this) {
        // retrieve new playground availabilities for the current month and the current sport
        viewModel.updatePlaygroundAvailabilitiesForCurrentMonthAndSport()
    }
}

/* Playground availabilities recycler view */
internal fun PlaygroundAvailabilitiesFragment.setupAvailablePlaygroundsRecyclerView() {
    val playgroundAvailabilitiesRecyclerView =
        requireView().findViewById<RecyclerView>(R.id.playground_availabilities_rv)

    // init adapter
    playgroundAvailabilitiesAdapter = PlaygroundAvailabilitiesAdapter(
        viewModel.getAvailablePlaygroundsOnSelectedDate(),
        viewModel.selectedDate.value ?: viewModel.defaultDate,
        viewModel.slotDuration
    )

    // init recycler view
    playgroundAvailabilitiesRecyclerView.apply {
        layoutManager = LinearLayoutManager(
            requireContext(), RecyclerView.VERTICAL, false)
        adapter = playgroundAvailabilitiesAdapter
    }
}

/* bottom bar */
internal fun PlaygroundAvailabilitiesFragment.setupBottomBar() {
    // show bottom bar
    val bottomBar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation_bar)
    bottomBar.visibility = View.VISIBLE

    // set the right selected button
    bottomBar.menu.findItem(R.id.playgrounds).isChecked = true
}

/* utils */

/** Compute availability percentage as the percentage of slots with at least one available playground */
private fun PlaygroundAvailabilitiesFragment.getAvailabilityPercentageOf(date: LocalDate): Float {
    val availablePlaygrounds = viewModel.getAvailablePlaygroundsOn(date).mapValues {
        (_, playgrounds) -> playgrounds.filter { it.available }
    }

    val slotsWithAtLeastOneAvailability = availablePlaygrounds.count { it.value.isNotEmpty() }
    val maxNumOfSlotsPerDay = 25

    return min(1.0f, slotsWithAtLeastOneAvailability.toFloat() / maxNumOfSlotsPerDay.toFloat())
}

private fun capitalize(str: String): String {
    return str.substring(0,1).uppercase() + str.substring(1).lowercase()
}
