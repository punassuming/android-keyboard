package org.futo.inputmethod.latin.payment

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.dataStore
import org.futo.inputmethod.latin.uix.getSetting
import org.futo.inputmethod.latin.uix.settings.DataStoreCacheProvider
import org.futo.inputmethod.latin.uix.settings.NavigationItem
import org.futo.inputmethod.latin.uix.settings.NavigationItemStyle
import org.futo.inputmethod.latin.uix.settings.SettingsActivity
import org.futo.inputmethod.latin.uix.settings.pages.EXT_LICENSE_KEY
import org.futo.inputmethod.latin.uix.settings.pages.IS_ALREADY_PAID
import org.futo.inputmethod.latin.uix.settings.pages.IS_PAYMENT_PENDING
import org.futo.inputmethod.latin.uix.settings.pages.PAYMENT_PENDING_STARTED_AT
import org.futo.inputmethod.latin.uix.settings.pages.PaymentThankYouScreen
import org.futo.inputmethod.latin.uix.settings.pages.startAppActivity
import org.futo.inputmethod.latin.uix.theme.UixThemeAuto
import org.futo.inputmethod.updates.openURI

class PaymentCompleteActivity : ComponentActivity() {
    companion object {
        private const val PAYMENT_PENDING_WINDOW_MS = 1000L * 60L * 60L * 24L
        private const val KEYBOARD_ACTIVATION_URI = "futo-keyboard://license/activate"
        private const val VOICE_INPUT_ACTIVATION_URI = "futo-voice-input://license/activate"
    }

    private fun isExpectedActivationUri(targetData: String): Boolean {
        return targetData == KEYBOARD_ACTIVATION_URI
                || targetData == VOICE_INPUT_ACTIVATION_URI
    }

    private fun updateContent() {
        setContent {
            DataStoreCacheProvider {
                UixThemeAuto {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(Modifier.safeDrawingPadding()) {
                            PaymentThankYouScreen(onExit = {
                                startAppActivity(SettingsActivity::class.java, clearTop = true)
                                finish()
                            })
                        }
                    }
                }
            }
        }
    }

    private fun onPaid(license: String) {
        lifecycleScope.launch {
            dataStore.edit {
                it[IS_ALREADY_PAID.key] = true
                it[IS_PAYMENT_PENDING.key] = false
                it[PAYMENT_PENDING_STARTED_AT.key] = 0L
                it[EXT_LICENSE_KEY.key] = license
            }

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateContent()
            }
        }
    }

    private fun onInvalidKey() {
        lifecycleScope.launch {
            if(applicationContext.getSetting(IS_ALREADY_PAID)) {
                finish()
            } else {
                setContent {
                    DataStoreCacheProvider {
                        UixThemeAuto {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Column {
                                    Text(
                                        getString(R.string.payment_screen_license_integrity_error),
                                        modifier = Modifier.padding(8.dp)
                                    )

                                    NavigationItem(
                                        title = "Email keyboard@futo.org",
                                        style = NavigationItemStyle.Mail,
                                        navigate = {
                                            openURI("mailto:keyboard@futo.org")
                                        })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetData = intent.dataString
        val pendingStartedAt = applicationContext.getSetting(PAYMENT_PENDING_STARTED_AT)
        val isRecentPending = (System.currentTimeMillis() - pendingStartedAt) <= PAYMENT_PENDING_WINDOW_MS
        val isPending = applicationContext.getSetting(IS_PAYMENT_PENDING)

        if(targetData != null && isExpectedActivationUri(targetData) && isPending && isRecentPending) {
            onPaid("activate")
        } else {
            Log.e("PaymentCompleteActivity", "futo-keyboard launched with invalid targetData $targetData")
            lifecycleScope.launch {
                dataStore.edit {
                    it[IS_PAYMENT_PENDING.key] = false
                }
            }
            finish()
        }
    }
}
