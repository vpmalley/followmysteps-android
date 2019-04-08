package fr.vpm.followmysteps

// Classes needed to initialize the map
import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.android.synthetic.main.activity_steps.*
import java.lang.ref.WeakReference


private const val ACCESS_FINE_LOCATION_REQ = 101
private const val CHECK_SETTINGS_REQ = 102

class StepsActivity : AppCompatActivity(), OnMapReadyCallback, PermissionsListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var firebaseAuth: FirebaseAuth
    private var requestingLocationUpdates = false
    private lateinit var mapboxMap: MapboxMap
    private lateinit var mapView: MapView
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var locationEngine: LocationEngine
    private val callback: MainActivityLocationCallback = MainActivityLocationCallback(this)
    private val DEFAULT_INTERVAL_IN_MILLISECONDS: Long = 1000L;
    private val DEFAULT_MAX_WAIT_TIME: Long = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, BuildConfig.MAPBOX_API_KEY)
        setContentView(R.layout.activity_steps)
        setSupportActionBar(toolbar)
/*
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
                    location?.let { askForTitle(location) }
                }
            }
        }
        */
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        firebaseAuth = FirebaseAuth.getInstance()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) {
            fun onStyleLoaded(style: Style) {
                enableLocationComponent(style)
            }
        };
    }

    private fun enableLocationComponent(loadedMapStyle: Style) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            val locationComponent = mapboxMap.locationComponent

// Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(this, loadedMapStyle)

// Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

// Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

// Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine()
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)

        val request: LocationEngineRequest = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper())
        locationEngine.getLastLocation(callback)
    }

    private fun askForTitle(location: Location) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.location_name_title)
                .setView(input)
                .setPositiveButton(R.string.all_ok) { dialogInterface, i ->
                    storeLocation(location, input.text.toString())
                }
                .setNegativeButton(R.string.all_cancel) { _, _ ->
                }
        builder.create().show()
    }

    private fun syncAllLocations(): Boolean {
        val firestoreLocationSync = FirestoreLocationSync()
        val realmLocationStore = RealmLocationStore()
        val nonSynchronisedLocations = realmLocationStore.retrieveNonSynchronisedLocations()

        firebaseAuth.signInAnonymously().addOnSuccessListener { authResult ->
            nonSynchronisedLocations?.forEach { location ->
                firestoreLocationSync.syncLocation(location,
                        OnSuccessListener {
                            Snackbar.make(fab, "synced", Snackbar.LENGTH_SHORT).show()
                            realmLocationStore.locationSynced(location)
                        },
                        OnFailureListener {
                            Snackbar.make(fab, "failed syncing", Snackbar.LENGTH_SHORT).show()
                        })
            }
        }
        return true
    }

    private fun storeLocation(location: Location, locationName: String) {
        RealmLocationStore().storeLocation(location, locationName)
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

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
//        startLocationUpdates()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
//        stopLocationUpdates()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState ?: Bundle())
    }

    override fun onDestroy() {
        super.onDestroy()
        locationEngine?.removeLocationUpdates(callback)
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    /*
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
    */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            ACCESS_FINE_LOCATION_REQ -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //determineGeoPosition()
                } else {
                    Snackbar.make(fab, R.string.snackbar_geoposition_permission_denied, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            if (mapboxMap.getStyle() != null) {
                enableLocationComponent(mapboxMap.getStyle()!!)
            }
        } else {
            Toast.makeText(this, R.string.snackbar_geoposition_permission_denied, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }
/*
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
*/

    private class MainActivityLocationCallback(activity: StepsActivity)
        : LocationEngineCallback<LocationEngineResult> {

        private val activityWeakReference: WeakReference<StepsActivity> = WeakReference(activity)

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        override fun onSuccess(result: LocationEngineResult) {

            val activity = activityWeakReference.get()
            if (activity != null) {
                val location: Location? = result.getLastLocation();

                if (location == null) {
                    return;
                }

// Create a Toast which displays the new location's coordinates
                Toast.makeText(activity,
                        "New location : ${result.getLastLocation()?.getLatitude()}, ${result.getLastLocation()?.getLongitude()}",
                        Toast.LENGTH_SHORT)
                        .show()

// Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can not be captured
         *
         * @param exception the exception message
         */
        override fun onFailure(exception: Exception) {
            Log.d("LocationChangeActivity", exception.getLocalizedMessage())
            val activity: StepsActivity? = activityWeakReference.get()
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }


}
