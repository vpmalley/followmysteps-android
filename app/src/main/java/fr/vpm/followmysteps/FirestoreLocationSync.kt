package fr.vpm.followmysteps

import android.location.Location
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import fr.vpm.followmysteps.model.Geometry
import fr.vpm.followmysteps.model.MapboxLocation
import fr.vpm.followmysteps.model.Properties
import java.util.*


class FirestoreLocationSync {

    fun syncLocation(location: Location,
                     successListener: OnSuccessListener<in DocumentReference> = OnSuccessListener { },
                     failureListener: OnFailureListener = OnFailureListener { }) {
        val syncableLocation = MapboxLocation(Geometry(Arrays.asList(location.longitude, location.latitude)),
                Properties(title = "I was here"))

        val db = FirebaseFirestore.getInstance()
        db.collection("my-steps")
                .add(syncableLocation)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener)
    }
}