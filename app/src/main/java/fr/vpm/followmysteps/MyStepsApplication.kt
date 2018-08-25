package fr.vpm.followmysteps

import android.app.Application
import io.realm.Realm

internal class MyStepsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        Realm.deleteRealm(Realm.getDefaultConfiguration())
    }
}