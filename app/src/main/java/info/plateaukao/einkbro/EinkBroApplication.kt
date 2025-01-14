package info.plateaukao.einkbro

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import info.plateaukao.einkbro.browser.AdBlock
import info.plateaukao.einkbro.browser.Cookie
import info.plateaukao.einkbro.browser.Javascript
import info.plateaukao.einkbro.database.BookmarkManager
import info.plateaukao.einkbro.preference.ConfigManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class EinkBroApplication : Application() {

    private val sp: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
    }

    private val config: ConfigManager by lazy {
        ConfigManager(applicationContext, sp)
    }

    val myModule = module {
        single { config }
        single { sp }
        single { BookmarkManager(androidContext()) }
        single { AdBlock(androidContext())}
        single { Javascript(androidContext())}
        single { Cookie(androidContext()) }
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@EinkBroApplication)
            modules(myModule)
        }

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    }
}
