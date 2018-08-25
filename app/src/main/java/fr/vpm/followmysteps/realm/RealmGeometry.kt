package fr.vpm.followmysteps.realm

import io.realm.RealmList
import io.realm.RealmObject

open class RealmGeometry(
        var coordinates: RealmList<Double> = RealmList()
) : RealmObject()