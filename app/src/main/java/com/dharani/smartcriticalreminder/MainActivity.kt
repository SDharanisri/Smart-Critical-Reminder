package com.dharani.smartcriticalreminder

import androidx.compose.runtime.LaunchedEffect
import android.telephony.SmsManager
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.graphics.Matrix
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.asImageBitmap
import android.media.RingtoneManager
import androidx.compose.material3.Card
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.media.AudioAttributes
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton

fun scheduleReminder(
    context: Context,
    title: String,
    date: String,
    time: String
) {
    val formatter = SimpleDateFormat(
        "d/M/yyyy HH:mm",
        Locale.getDefault()
    )
    val reminderTime = formatter.parse("$date $time")?.time ?: return

    if (reminderTime < System.currentTimeMillis()) {
        return
    }

    val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
            return
        }
    }
    val intent = Intent(context, ReminderReceiver::class.java)

    intent.putExtra("title", title)
    intent.putExtra("notificationId", reminderTime.toInt())

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        reminderTime.toInt(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

        if (alarmManager.canScheduleExactAlarms()) {

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )

        } else {

            val settingsIntent = Intent(
                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
            )

            context.startActivity(settingsIntent)
        }

    } else {

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )
    }
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val title =
            intent.getStringExtra("title") ?: "Reminder"

        val notificationId =
            intent.getIntExtra("notificationId", 1)

        val channelId = "reminder_channel"
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("openCamera", true)
        }

        val pendingOpen = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val soundUri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            )

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Critical Reminder Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                setSound(soundUri, audioAttributes)

                enableVibration(true)

                vibrationPattern = longArrayOf(
                    0,
                    500,
                    300,
                    500
                )
            }
            channel.setBypassDnd(true)
            channel.enableVibration(true)

            notificationManager.createNotificationChannel(channel)
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(
                context,
                channelId
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(
                    "Smart Critical Reminder"
                )
                .setContentText(title)
                .setContentIntent(pendingOpen)
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setVibrate(longArrayOf(0, 500, 300, 500))
                .setAutoCancel(true)
                .build()

        notificationManager.notify(
            notificationId,
            notification
        )
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        val openCamera = intent.getBooleanExtra("openCamera", false)

        if (openCamera) {
            setContent {
                val smsPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                }

                SmartCriticalReminderApp(openCamera = true)
            }

        } else {
            setContent {
                val smsPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }

                LaunchedEffect(Unit) {
                    smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                }

                SmartCriticalReminderApp(openCamera = true)
            }


        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                100
            )
        }

        setContent {
            SmartCriticalReminderApp()
        }
    }
}
data class ReminderItem(
    val id: Int,
    val title: String,
    val expectedAction: String,
    val date: String,
    val time: String
)
fun saveReminders(context: Context, reminders: List<ReminderItem>) {
    val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    prefs.edit().putString("list", Gson().toJson(reminders)).apply()
}

fun loadReminders(context: Context): List<ReminderItem> {
    val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)
    val json = prefs.getString("list", null) ?: return emptyList()
    val type = object : TypeToken<List<ReminderItem>>() {}.type
    return Gson().fromJson(json, type)
}
@Composable
fun SmartCriticalReminderApp(openCamera: Boolean = false) {
    var capturedImage by remember {
        mutableStateOf<Bitmap?>(null)
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val rotated = Bitmap.createBitmap(
                it, 0, 0, it.width, it.height,
                Matrix().apply { postRotate(90f) }, true
            )
            capturedImage = rotated
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        }
    }
    val context = LocalContext.current

    var reminders by remember {
        mutableStateOf(loadReminders(context).toMutableList())
    }

    var editingReminder by remember {
        mutableStateOf<ReminderItem?>(null)
    }

    var showReminderForm by remember {
        mutableStateOf(false)
    }
    var searchText by remember {
        mutableStateOf("")
    }
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Smart Critical Reminder",
                style = MaterialTheme.typography.headlineMedium
            )

            Text("Never miss an important reminder!")

            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search Reminder") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )

            Button(onClick = { showReminderForm = true }) {
                Text("Add Reminder")
            }

            if (showReminderForm) {
                ReminderForm(
                    reminders = reminders,
                    cameraPermissionLauncher = cameraPermissionLauncher,
                    cameraLauncher = cameraLauncher,
                    capturedImage = capturedImage,
                    setCapturedImage = { capturedImage = it },
                    editingReminder = editingReminder,
                    setEditingReminder = { editingReminder = it },
                    setReminders = { reminders = it },
                    setShowReminderForm = { showReminderForm = it },
                    searchText = searchText
                )
            }
        }
    }
}
@Composable
fun ReminderForm(
    reminders: MutableList<ReminderItem>,
    cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    cameraLauncher: ManagedActivityResultLauncher<Void?, Bitmap?>,
    capturedImage: Bitmap?,
    setCapturedImage: (Bitmap?) -> Unit,
    editingReminder: ReminderItem?,
    setEditingReminder: (ReminderItem?) -> Unit,
    setReminders: (MutableList<ReminderItem>) -> Unit,
    setShowReminderForm: (Boolean) -> Unit,
    searchText: String
) {
    var emergencyNumber by remember {
        mutableStateOf("")
    }


    var reminderTitle by remember {
        mutableStateOf("")
    }

    var selectedDate by remember {
        mutableStateOf("")
    }

    var selectedTime by remember {
        mutableStateOf("")
    }

    var savedReminder by remember {
        mutableStateOf("")
    }

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    var showTimePicker by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    LaunchedEffect(editingReminder) {
        editingReminder?.let {
            reminderTitle = it.title
            selectedDate = it.date
            selectedTime = it.time
        }
    }
    Text(
        text = "Add New Reminder",
        style =
            MaterialTheme.typography.headlineSmall,
        modifier =
            Modifier.padding(top = 24.dp)
    )
    var expectedAction by remember {
        mutableStateOf("")
    }
    TextField(
        value = reminderTitle,
        onValueChange = { reminderTitle = it },
        label = { Text("Reminder Title") },
        modifier = Modifier.padding(top = 16.dp)
    )
    TextField(
        value = expectedAction,
        onValueChange = { expectedAction = it },
        label = { Text("Expected Action") },
        modifier = Modifier.padding(top = 12.dp)
    )
    TextField(
        value = emergencyNumber,
        onValueChange = { emergencyNumber = it },
        label = { Text("Emergency Contact") }
    )
    Button(
        onClick = {
            showDatePicker = true
        },
        modifier =
            Modifier.padding(top = 16.dp)
    ) {
        Text(
            if (selectedDate.isEmpty())
                "Select Date"
            else
                "Date: $selectedDate"
        )
    }
    Button(
        onClick = {
            showTimePicker = true
        },
        modifier =
            Modifier.padding(top = 8.dp)
    ) {
        Text(
            if (selectedTime.isEmpty())
                "Select Time"
            else
                "Time: $selectedTime"
        )
    }
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate =
                    "$dayOfMonth/${month + 1}/$year"
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedTime =
                    String.format(
                        "%02d:%02d",
                        hourOfDay,
                        minute
                    )
                showTimePicker = false
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }
    Button(
        onClick = {
            if (
                reminderTitle.isNotEmpty() &&
                selectedDate.isNotEmpty() &&
                selectedTime.isNotEmpty()
            ) {
                scheduleReminder(
                    context = context,
                    title = reminderTitle,
                    date = selectedDate,
                    time = selectedTime
                )
                if (editingReminder == null) {
                    val newList = reminders.toMutableList()
                    newList.add(
                        ReminderItem(
                            id = reminders.size + 1,
                            title = reminderTitle,
                            expectedAction = expectedAction,
                            date = selectedDate,
                            time = selectedTime
                        )
                    )
                    setReminders(newList)
                    if (emergencyNumber.isNotEmpty()) {
                        sendEmergencySMS(
                            context,
                            emergencyNumber,
                            reminderTitle
                        )
                    }
                } else {
                    val updatedList = reminders.map {
                        if (it.id == editingReminder!!.id)
                            it.copy(
                                title = reminderTitle,
                                expectedAction = expectedAction,
                                date = selectedDate,
                                time = selectedTime
                            )
                        else it
                    }.toMutableList()
                    setReminders(updatedList)
                    setEditingReminder(null)
                    expectedAction = ""
                    setCapturedImage(null)
                    setShowReminderForm(false)
                }
                savedReminder = "Reminder Saved!"
                reminderTitle = ""
                expectedAction = ""
                selectedDate = ""
                selectedTime = ""
                setCapturedImage(null)
                setShowReminderForm(false)
            }
        },
        modifier =
            Modifier.padding(top = 16.dp)
    ) {
        Text("Save Reminder")
    }
    var showProofDialog by remember {
        mutableStateOf(false)
    }
    if (savedReminder.isNotEmpty()) {
        Text(
            text =
                "Saved Reminder: $savedReminder",
            modifier =
                Modifier.padding(top = 16.dp)
        )
    }
    Button(
        onClick = { showProofDialog = true },
        modifier = Modifier.padding(top = 12.dp)
    ) {
        Text("Verify Proof")
    }
    if (showProofDialog) {
        AlertDialog(
            onDismissRequest = { showProofDialog = false },
            title = { Text("Proof Verification") },
            text = { Text("Take a photo to verify the completed action.") },
            confirmButton = {
                Button(onClick = {
                    showProofDialog = false
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Open Camera")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProofDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    Button(
        onClick = {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    ) {
        Text("Capture Proof")
    }
    capturedImage?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
    }
    val filteredReminders = reminders.filter {
        it.title.contains(searchText, ignoreCase = true)
    }
    if (filteredReminders.isNotEmpty()) {
        Text("Saved Reminders")
        filteredReminders.forEach { reminder ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(reminder.title)
                        Text("Expected: ${reminder.expectedAction}")
                        Text("${reminder.date} ${reminder.time}")
                    }
                    Row {
                        TextButton(onClick = {
                            setEditingReminder(reminder)
                        }) {
                            Text("Edit")
                        }
                        TextButton(onClick = {
                            val newList = reminders.filter {
                                it.id != reminder.id
                            }.toMutableList()
                            setReminders(newList)
                        }) {
                            Text("Delete", color = Color.Red)
                        }
                    }
                }
            }
        }
        fun sendEmergencySMS(
            context: Context,
            number: String,
            title: String
        ) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                SmsManager.getDefault().sendTextMessage(
                    number,
                    null,
                    "🚨 ALERT! Task not completed: $title",
                    null,
                    null
                )
            }
        }
    }
}
fun sendEmergencySMS(
    context: Context,
    number: String,
    title: String
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        SmsManager.getDefault().sendTextMessage(
            number, null,
            "🚨 ALERT! Task not completed: $title",
            null, null
        )
    }
}






