package fr.vpm.followmysteps

import android.location.Location
import fr.vpm.followmysteps.model.Geometry
import fr.vpm.followmysteps.model.MapboxLocation
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore



class FirestoreLocationSync {

    fun syncLocation(location: Location) {
        val syncableLocation = MapboxLocation(Geometry(Arrays.asList(location.longitude, location.latitude)),
                fr.vpm.followmysteps.model.Properties(title = "I was here"))

        val db = FirebaseFirestore.getInstance()
        db.collection("my-steps")
                .add(syncableLocation)
                .addOnSuccessListener {  }
    }
}