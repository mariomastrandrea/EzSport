package it.polito.mad.sportapp.show_reservations

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import it.polito.mad.sportapp.R
import it.polito.mad.sportapp.profile.ShowProfileActivity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

class ShowReservationsActivity : AppCompatActivity() {

    // calendar view
    private lateinit var calendarView: CalendarView
    private lateinit var legendContainer: ViewGroup
    private lateinit var monthLabel: TextView

    // month buttons
    private lateinit var previousMonthButton: ImageView
    private lateinit var nextMonthButton: ImageView

    // show reservations view model
    private val vm by viewModels<ShowReservationsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_reservations)

        monthButtonsInit()

        // initialize CalendarView from layout
        calendarView = findViewById(R.id.calendar_view)
        legendContainer = findViewById(R.id.legend_container)

        // initialize month label
        monthLabel = findViewById(R.id.month_label)

        calendarInit()

    }

    // initialize month buttons
    private fun monthButtonsInit() {

        // initialize previous / next month buttons and their listeners
        previousMonthButton = findViewById(R.id.previous_month_button)

        previousMonthButton.setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let { month ->
                val newMonth = month.yearMonth.minusMonths(1)
                handleCurrentMonthChanged(newMonth)
            }
        }

        nextMonthButton = findViewById(R.id.next_month_button)

        nextMonthButton.setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let { month ->
                val newMonth = month.yearMonth.plusMonths(1)
                handleCurrentMonthChanged(newMonth)
            }
        }
    }

    // initialize calendar information
    private fun calendarInit() {

        // bind days to the calendar recycler view
        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            // call only when a new container is needed.
            override fun create(view: View) = DayViewContainer(view, vm)

            // call every time we need to reuse a container.
            override fun bind(container: DayViewContainer, data: CalendarDay) {

                val dayTextView = container.textView

                container.day = data
                dayTextView.text = data.date.dayOfMonth.toString()

                // set visibility of day text view as visible
                dayTextView.visibility = View.VISIBLE

                if (container.day.position == DayPosition.MonthDate) {

                    // mark current date
                    if (container.day.date == LocalDate.now()) {
                        dayTextView.setTextColor(getColor(R.color.blue_200))
                        dayTextView.background =
                            ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.current_day_selected_bg,
                                null
                            )
                    } else {
                        dayTextView.setTextColor(getColor(R.color.black))
                        dayTextView.background = null
                    }

                    // mark selected date
                    if (container.day.date == vm.selectedDate.value) {
                        dayTextView.setTextColor(getColor(R.color.red))
                        dayTextView.background =
                            ResourcesCompat.getDrawable(resources, R.drawable.day_selected_bg, null)
                    }
                }
                // set text color for out dates
                else {
                    dayTextView.setTextColor(getColor(R.color.grey))
                }

            }
        }

        // initialize current month live data variable
        vm.currentMonth.observe(this) { month ->
            val monthString = month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            monthLabel.text = capitalizeFirstLetter(monthString)
        }

        // initialize selected date live data variable
        vm.selectedDate.observe(this) { date ->

            // update calendar
            calendarView.notifyCalendarChanged()
        }

        // initialize days of week to monday
        val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY)

        legendContainer.children
            .map { it as TextView }
            .forEachIndexed { index, textView ->
                val dayOfWeek = daysOfWeek[index]
                val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                textView.text = title
            }

        // attach month scroll listener
        calendarView.monthScrollListener = { month ->
            handleCurrentMonthChanged(month.yearMonth)
        }

        // finalize calendar view initialization
        vm.currentMonth.value?.let {
            val startMonth = it.minusMonths(100)
            val endMonth = it.plusMonths(100)

            calendarView.setup(startMonth, endMonth, daysOfWeek.first())
            calendarView.scrollToMonth(it)
        }

    }

    // handle new selected month
    private fun handleCurrentMonthChanged(month: YearMonth) {
        vm.setCurrentMonth(month)
        calendarView.smoothScrollToMonth(month)

        // update calendar
        calendarView.notifyCalendarChanged()
    }

    /* app menu */

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // inflate and render the menu
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.show_reservations_menu, menu)
        // change app bar's title
        supportActionBar?.title = "Dashboard"

        return true
    }

    //TODO: create a bottom bar in order to delete the button
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        // detect which item has been selected and perform corresponding action
        R.id.show_profile_button -> handleShowProfileButton()
        else -> super.onOptionsItemSelected(item)
    }

    private fun handleShowProfileButton(): Boolean {
        val showProfileIntent = Intent(this, ShowProfileActivity::class.java)
        startActivity(showProfileIntent)

        return true
    }

}