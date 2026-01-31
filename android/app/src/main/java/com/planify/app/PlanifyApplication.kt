package com.planify.app

import android.app.Application
import com.planify.app.util.AppContext

class PlanifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)
    }
}
