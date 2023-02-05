package com.yandex.mail.shadows

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.IOException
import java.util.concurrent.ExecutionException

@Implements(FirebaseMessaging::class)
class ShadowFirebaseMessaging {

    private var token: String? = null

    private var exceptionCause: String? = null

    fun setToken(token: String) {
        this.token = token
        this.exceptionCause = null
    }

    fun setExceptionCause(exceptionCause: String) {
        this.token = null
        this.exceptionCause = exceptionCause
    }

    @Implementation
    fun getToken(): Task<String> {
        return when {
            exceptionCause != null -> Tasks.forException(ExecutionException(IOException(exceptionCause)))
            token != null -> Tasks.forResult(token!!)
            else -> throw RuntimeException("Token or exception cause must be specified!")
        }
    }
}
