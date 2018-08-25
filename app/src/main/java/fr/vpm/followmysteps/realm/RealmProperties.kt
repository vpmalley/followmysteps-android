package fr.vpm.followmysteps.realm

import io.realm.RealmObject

open class RealmProperties(
        var icon: String = "marker",
        var title: String = ""

) : RealmObject()