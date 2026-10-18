package com.creativeblisslabs.omnichessreader

import android.app.Application
import com.creativeblisslabs.omnichessreader.di.initKoin
import com.creativeblisslabs.omnimind.core.ui.util.AndroidContextHolder
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class ChessReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContextHolder.context = this
        initKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ChessReaderApplication)
        }
    }
}
