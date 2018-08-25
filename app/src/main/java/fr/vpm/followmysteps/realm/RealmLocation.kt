package fr.vpm.followmysteps.realm

import io.realm.RealmObject

open class RealmLocation(
        var geometry: RealmGeometry? = RealmGeometry(),
        var properties: RealmProperties? = RealmProperties(),
        var synchronised: Boolean = false

) : RealmObject()