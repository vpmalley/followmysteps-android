package fr.vpm.followmysteps

import android.location.Location
import fr.vpm.followmysteps.realm.RealmGeometry
import fr.vpm.followmysteps.realm.RealmLocation
import fr.vpm.followmysteps.realm.RealmProperties
import io.realm.Realm
import io.realm.RealmList
import io.realm.kotlin.createObject


class RealmLocationStore {

    fun storeLocation(location: Location) {
        val realm = Realm.getDefaultInstance()

        realm.executeTransaction {
            val geometry = realm.createObject<RealmGeometry>()
            geometry.coordinates = RealmList(location.longitude, location.latitude)

            val properties = realm.createObject<RealmProperties>()
            properties.title = "I was here"

            val storedLocation = realm.createObject<RealmLocation>()
            storedLocation.geometry = geometry
            storedLocation.properties = properties
        }
    }
}