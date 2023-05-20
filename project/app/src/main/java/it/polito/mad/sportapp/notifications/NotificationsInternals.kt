package it.polito.mad.sportapp.notifications

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import it.polito.mad.sportapp.R
import java.time.LocalDate
import java.time.LocalTime

// manage menu item selection
internal fun NotificationsFragment.menuInit() {
    val menuHost: MenuHost = requireActivity()

    menuHost.addMenuProvider(object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.notifications_menu, menu)

            actionBar?.let {
                it.setDisplayHomeAsUpEnabled(false)
                it.title = "My Notifications"
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            // handle the menu selection
            return false
        }
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
}

/* notifications recycler view */
@SuppressLint("NotifyDataSetChanged")
internal fun NotificationsFragment.recyclerViewInit() {

    val currentDate = LocalDate.now()
    val currentTime = LocalTime.now()

    // initialize notifications RecyclerView from layout
    notificationsRecyclerView = requireView().findViewById(R.id.notifications_recycler_view)

    notificationsRecyclerView.apply {
        layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        adapter = notificationsAdapter
    }

    // setup notifications observer
    vm.notifications.observe(viewLifecycleOwner) { notificationList ->
        // add notifications to the notifications adapter
        notificationsAdapter.notifications.clear()
        notificationsAdapter.notifications.addAll(notificationList.filter {
            if (it.date.isAfter(currentDate))
                true
            else it.date.isEqual(currentDate) && it.startTime.isAfter(currentTime)
        }
            .sortedWith(compareBy<Notification> { it.date }.thenBy { it.startTime }))

        notificationsAdapter.notifyDataSetChanged()
    }
}

/* bottom bar */
internal fun NotificationsFragment.setupBottomBar() {
    // show bottom bar
    val bottomBar =
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation_bar)

    bottomBar.visibility = View.VISIBLE

    // set the right selected button
    bottomBar.menu.findItem(R.id.notifications).isChecked = true
}