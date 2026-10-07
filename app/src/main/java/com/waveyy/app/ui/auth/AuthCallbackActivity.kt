package com.waveyy.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.waveyy.app.MainActivity
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.ui.theme.WaveyyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * OAuth redirect landing activity (`com.waveyy.app://auth/callback`).
 * Exchanges the `code` query parameter for a session (PKCE), mirroring
 * `app/auth/callback/page.jsx` (`exchangeCodeForSession` then home).
 */
@AndroidEntryPoint
class AuthCallbackActivity : ComponentActivity() {

    @Inject
    lateinit var repository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaveyyTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Signing you in…")
                }
            }
        }
        val code = intent?.data?.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            goHome()
            return
        }
        lifecycleScope.launch {
            repository.exchangeCode(code)
            goHome()
        }
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
