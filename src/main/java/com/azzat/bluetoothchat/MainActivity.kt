package com.azzat.bluetoothchat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BluetoothChatApp(this)
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BluetoothChatApp(context: Context) {

    val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    val bluetoothAdapter = bluetoothManager.adapter

    var bluetoothEnabled by remember {
        mutableStateOf(bluetoothAdapter?.isEnabled == true)
    }

    var scanning by remember {
        mutableStateOf(false)
    }

    var selectedDevice by remember {
        mutableStateOf<BluetoothDevice?>(null)
    }

    val devices = remember {
        mutableStateListOf<BluetoothDevice>()
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            val connectGranted =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    result[Manifest.permission.BLUETOOTH_CONNECT] == true
                } else {
                    true
                }

            if (connectGranted) {
                bluetoothEnabled = bluetoothAdapter?.isEnabled == true
            }
        }

    fun requestBluetoothPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )

        } else {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    DisposableEffect(Unit) {

        val receiver = object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                when (intent?.action) {

                    BluetoothDevice.ACTION_FOUND -> {

                        val device =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(
                                    BluetoothDevice.EXTRA_DEVICE,
                                    BluetoothDevice::class.java
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(
                                    BluetoothDevice.EXTRA_DEVICE
                                )
                            }

                        if (device != null &&
                            devices.none { it.address == device.address }
                        ) {
                            devices.add(device)
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        scanning = false
                    }

                    BluetoothAdapter.ACTION_STATE_CHANGED -> {

                        bluetoothEnabled =
                            bluetoothAdapter?.isEnabled == true
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(Unit) {

        if (bluetoothAdapter != null) {

            try {

                val bonded = bluetoothAdapter.bondedDevices

                devices.clear()
                devices.addAll(bonded)

            } catch (_: SecurityException) {
            }
        }
    }

    val background = Brush.verticalGradient(
        listOf(
            Color(0xFF050816),
            Color(0xFF0A1025),
            Color(0xFF050816)
        )
    )

    MaterialTheme {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background)
            ) {

                DecorativeBackground()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {

                    Spacer(modifier = Modifier.height(35.dp))

                    Header()

                    Spacer(modifier = Modifier.height(24.dp))

                    StatusCard(
                        enabled = bluetoothEnabled,
                        scanning = scanning
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {

                            requestBluetoothPermissions()

                            if (bluetoothAdapter == null) return@Button

                            if (!bluetoothEnabled) {

                                try {
                                    context.startActivity(
                                        Intent(
                                            BluetoothAdapter.ACTION_REQUEST_ENABLE
                                        )
                                    )
                                } catch (_: Exception) {
                                }

                            } else {

                                try {

                                    if (bluetoothAdapter.isDiscovering) {
                                        bluetoothAdapter.cancelDiscovery()
                                    }

                                    devices.clear()

                                    bluetoothAdapter.startDiscovery()

                                    scanning = true

                                } catch (_: SecurityException) {
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1688FF)
                        )
                    ) {

                        Text(
                            text = if (scanning) {
                                "🔎  جارٍ البحث عن الأجهزة..."
                            } else {
                                "⚡  البحث عن أجهزة Bluetooth"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "الأجهزة القريبة",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AnimatedVisibility(
                        visible = scanning,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {

                        Text(
                            text = "يتم البحث بطريقة آمنة عن الأجهزة المتاحة...",
                            color = Color(0xFF8EA7C7),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (devices.isEmpty()) {

                        EmptyDevices()

                    } else {

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            items(
                                items = devices,
                                key = { it.address }
                            ) { device ->

                                DeviceCard(
                                    device = device,
                                    selected = selectedDevice?.address ==
                                            device.address,
                                    onClick = {
                                        selectedDevice = device
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedDevice != null) {

                    ConnectionBottomCard(
                        device = selectedDevice!!,
                        onClose = {
                            selectedDevice = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Header() {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1688FF),
                            Color(0xFF6A35FF)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "ᛒ",
                color = Color.White,
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {

            Text(
                text = "عزت السراء",
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Bluetooth Chat",
                color = Color(0xFF7EA9D9),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun StatusCard(
    enabled: Boolean,
    scanning: Boolean
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xCC101A32)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled)
                            Color(0x3325E6A4)
                        else
                            Color(0x33FF5B6E)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = if (enabled) "✓" else "!",
                    color =
                        if (enabled)
                            Color(0xFF25E6A4)
                        else
                            Color(0xFFFF5B6E),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text =
                        if (enabled)
                            "Bluetooth متصل"
                        else
                            "Bluetooth غير مفعّل",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        if (scanning)
                            "البحث جارٍ الآن..."
                        else
                            "جاهز للاتصال بالأجهزة",
                    color = Color(0xFF8195B5),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceCard(
    device: BluetoothDevice,
    selected: Boolean,
    onClick: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = Color(0xFF1688FF),
                shape = RoundedCornerShape(19.dp)
            ),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected)
                    Color(0x332288FF)
                else
                    Color(0xCC111B30)
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0x221688FF)),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "ᛒ",
                    color = Color(0xFF3DA3FF),
                    fontSize = 27.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = try {
                        device.name ?: "جهاز Bluetooth"
                    } catch (_: SecurityException) {
                        "جهاز Bluetooth"
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = try {
                        device.address
                    } catch (_: SecurityException) {
                        "عنوان غير متاح"
                    },
                    color = Color(0xFF7185A7),
                    fontSize = 11.sp
                )
            }

            Text(
                text = if (selected) "✓" else "›",
                color = Color(0xFF3DA3FF),
                fontSize = 27.sp
            )
        }
    }
}

@Composable
fun EmptyDevices() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 45.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0x221688FF)),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "ᛒ",
                color = Color(0xFF3DA3FF),
                fontSize = 48.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "لا توجد أجهزة حتى الآن",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = "اضغط على زر البحث للعثور على أجهزة Bluetooth",
            color = Color(0xFF7185A7),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ConnectionBottomCard(
    device: BluetoothDevice,
    onClose: () -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF121E36)
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            )
