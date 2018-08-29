package fr.vpm.followmysteps

import android.location.Location
import fr.vpm.followmysteps.realm.RealmGeometry
import fr.vpm.followmysteps.realm.RealmLocation
import fr.vpm.followmysteps.realm.RealmProperties
import io.realm.Realm
import io.realm.RealmList
import io.realm.RealmResults
import io.realm.kotlin.createObject
import io.realm.kotlin.where


class RealmLocationStore {

    fun storeLocation(location: Location, locationName: String) {
        val realm = Realm.getDefaultInstance()

        realm.executeTransaction {
            val geometry = realm.createObject<RealmGeometry>()
            geometry.coordinates = RealmList(location.longitude, location.latitude)

            val properties = realm.createObject<RealmProperties>()
            properties.title = locationName

            val storedLocation = realm.createObject<RealmLocation>()
            storedLocation.geometry = geometry
            storedLocation.properties = properties
        }
    }

    fun locationSynced(storedLocation: RealmLocation) {
        val realm = Realm.getDefaultInstance()

        realm.executeTransaction {
            storedLocation.synchronised = true
        }
    }

    fun retrieveNonSynchronisedLocations(): RealmResults<RealmLocation>? {
        val realm = Realm.getDefaultInstance()

        val results = realm.where<RealmLocation>()
                .equalTo("synchronised", false)
                .findAll()
        return results
    }
}