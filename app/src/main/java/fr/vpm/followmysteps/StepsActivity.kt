package fr.vpm.followmysteps

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import kotlinx.android.synthetic.main.activity_steps.*

private const val ACCESS_FINE_LOCATION_REQ = 101
private const val CHECK_SETTINGS_REQ = 102

class StepsActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var requestingLocationUpdates = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_steps)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            askGeoPosition()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    stopLocationUpdates()
                    requestingLocationUpdates = false
                    location?.let { storeLocation(location) }
                }
            }
        }
    }

    private fun syncAllLocations(): Boolean {
        val firestoreLocationSync = FirestoreLocationSync()
        val nonSynchronisedLocations = RealmLocationStore().retrieveNonSynchronisedLocations()
        nonSynchronisedLocations?.forEach { location ->
            firestoreLocationSync.syncLocation(location,
                    OnSuccessListener {
                        Snackbar.make(fab, "synced", Snackbar.LENGTH_SHORT).show()
                        location.synchronised = true
                    },
                    OnFailureListener {
                        Snackbar.make(fab, "failed syncing", Snackbar.LENGTH_SHORT).show()
                    })
        }
        return true
    }

    private fun storeLocation(location: Location) {
        RealmLocationStore().storeLocation(location)
        Snackbar.make(fab, "location stored", Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_steps, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_sync ->
                syncAllLocations()
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        try {
            if (requestingLocationUpdates)
                fusedLocationClient.requestLocationUpdates(locationRequest(),
                        locationCallback,
                        null)
        } catch (e: SecurityException) {
            Snackbar.make(fab, "could not find location", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationUpdates() {
        if (requestingLocationUpdates)
            fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun askGeoPosition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.geoposition_permission_title)
                        .setMessage(R.string.geoposition_permission_message)
                        .setPositiveButton(R.string.all_ok) { dialogInterface, i ->
                            ActivityCompat.requestPermissions(this,
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                    ACCESS_FINE_LOCATION_REQ)
                        }
                builder.create().show()
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        ACCESS_FINE_LOCATION_REQ)
            }
        } else {
            determineGeoPosition()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            ACCESS_FINE_LOCATION_REQ -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    determineGeoPosition()
                } else {
                    Snackbar.make(fab, R.string.snackbar_geoposition_permission_denied, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun determineGeoPosition() {
        val locationRequest = locationRequest()
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
        LocationServices.getSettingsClient(this)
                .checkLocationSettings(builder.build())
                .addOnFailureListener { exception ->
                    if (exception is ResolvableApiException) {
                        try {
                            exception.startResolutionForResult(this@StepsActivity, CHECK_SETTINGS_REQ)
                        } catch (sendEx: IntentSender.SendIntentException) {
                            // Ignore the error.
                        }
                    }
                }

        requestingLocationUpdates = true
        startLocationUpdates()
    }

    private fun locationRequest(): LocationRequest {
        return LocationRequest().apply {
            interval = 10000
            fastestInterval = 10000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }


}
