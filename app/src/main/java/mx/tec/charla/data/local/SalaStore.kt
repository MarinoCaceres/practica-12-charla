package mx.tec.charla.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mx.tec.charla.BuildConfig
import mx.tec.charla.domain.ANFITRION
import mx.tec.charla.domain.Destino

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sala")

/**
 * A qué sala va la app. Vacío = a la tuya.
 *
 * Se guarda para que, si cierras la app, vuelvas a la misma sala sin pedir
 * entrar otra vez: el token sigue valiendo mientras el anfitrión no borre su
 * base.
 */
@Singleton
class SalaStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val SERVIDOR = stringPreferencesKey("servidor")
    private val TOKEN = stringPreferencesKey("token")
    private val YO = stringPreferencesKey("yo")

    val destino: Flow<Destino> = context.dataStore.data.map { prefs ->
        val servidor = prefs[SERVIDOR]
        val token = prefs[TOKEN]
        val yo = prefs[YO]
        if (servidor != null && token != null && yo != null) {
            Destino(servidor, token, yo, esPropio = false)
        } else {
            propio
        }
    }

    suspend fun entrar(servidor: String, token: String, yo: String) {
        context.dataStore.edit {
            it[SERVIDOR] = servidor
            it[TOKEN] = token
            it[YO] = yo
        }
    }

    /** El botón Reiniciar: olvida la sala ajena y vuelve a la tuya. */
    suspend fun reiniciar() {
        context.dataStore.edit { it.clear() }
    }

    companion object {
        val propio = Destino(BuildConfig.SERVIDOR_PROPIO, BuildConfig.CLAVE_ANFITRION, ANFITRION, esPropio = true)
    }
}