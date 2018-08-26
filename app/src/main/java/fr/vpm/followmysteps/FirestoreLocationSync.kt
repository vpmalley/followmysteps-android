package fr.vpm.followmysteps

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import fr.vpm.followmysteps.model.Geometry
import fr.vpm.followmysteps.model.MapboxLocation
import fr.vpm.followmysteps.model.Properties
import fr.vpm.followmysteps.realm.RealmLocation

class FirestoreLocationSync {

    val db = FirebaseFirestore.getInstance()

    fun syncLocation(location: RealmLocation,
                     successListener: OnSuccessListener<in DocumentReference> = OnSuccessListener { },
                     failureListener: OnFailureListener = OnFailureListener { }) {
        location.geometry?.let { storedGeometry ->
            location.properties?.let { storedProperties ->
                val syncableLocation = MapboxLocation(Geometry(storedGeometry.coordinates),
                        Properties(title = storedProperties.title))

                db.collection("my-steps")
                        .add(syncableLocation)
                        .addOnSuccessListener(successListener)
                        .addOnFailureListener(failureListener)
            }
        }
    }
}