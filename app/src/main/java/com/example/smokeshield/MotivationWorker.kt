package com.example.smokeshield

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MotivationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    // Questa è la funzione che Android avvierà in background!
    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: return Result.success() // Se non è loggato, si ferma

        val db = FirebaseFirestore.getInstance()

        return try {
            // Eseguiamo lo scaricamento da Firebase in modo sincrono usando "Tasks.await"
            // perché siamo già in un thread di background sicuro
            val document = withContext(Dispatchers.IO) {
                Tasks.await(db.collection("users").document(user.uid).get())
            }

            // Peschiamo la motivazione (o mettiamo un testo standard se manca)
            val motivazione = document.getString("motivazione") ?: "il tuo benessere"

            // Creiamo la notifica
            showNotification(motivazione)
            Result.success()

        } catch (e: Exception) {
            Log.e("MotivationWorker", "Errore nel background task: ${e.message}")
            Result.retry() // Se non c'è internet, dice ad Android di riprovare più tardi!
        }
    }

    private fun showNotification(motivazione: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "smokeshield_channel"

        // Da Android 8 in poi, le notifiche richiedono obbligatoriamente un "Canale"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Supporto Motivazionale",
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH fa apparire il banner a comparsa!
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Questo serve per riaprire l'app quando l'utente tocca la notifica
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // Costruiamo la grafica della Notifica
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icona standard di Android
            .setContentTitle("Non mollare!")
            .setContentText("Ricorda che lo fai per: $motivazione") // IL DATO DA FIREBASE!
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Spara la notifica! (ID 1)
        notificationManager.notify(1, builder.build())
    }
}