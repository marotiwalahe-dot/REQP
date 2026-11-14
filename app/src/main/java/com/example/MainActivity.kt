package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.security.EncryptedPdfCacheManager
import com.example.ui.navigation.SecureStudyAppRoot
import com.example.ui.theme.SecureStudyTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecureStudyTheme {
                SecureStudyAppRoot()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            EncryptedPdfCacheManager(applicationContext).wipeAllTemporaryFiles()
        }
    }
}
