package com.aitutor.chatbot.app.di

import com.aitutor.chatbot.app.data.firebase.FirebaseAuthGateway
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import org.koin.dsl.module

val firebaseModule = module {
    single { Firebase.auth }
    single { Firebase.firestore }
    single { FirebaseAuthGateway(get()) }
}
