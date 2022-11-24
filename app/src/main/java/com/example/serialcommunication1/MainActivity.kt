package com.example.serialcommunication1

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface

class MainActivity : AppCompatActivity() {

    lateinit var m_usbManager: UsbManager
    var m_device: UsbDevice? = null
    var m_serial: UsbSerialDevice? = null
    var m_connection: UsbDeviceConnection? = null

    val ACTION_USB_PERMISSION = "permission"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        m_usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(broadcastReceiver, filter)

        val on = findViewById<Button>(R.id.on)
        val off = findViewById<Button>(R.id.off)
        val disconnect = findViewById<Button>(R.id.disconnect)
        val connect = findViewById<Button>(R.id.connect)
        on.setOnClickListener() { sendData("o") }
        off.setOnClickListener() { sendData("x") }
        disconnect.setOnClickListener() { disconnect() }
        connect.setOnClickListener() { startUsbConnecting() }
    }

    private fun startUsbConnecting() {
        val usbDevices: HashMap<String, UsbDevice>? = m_usbManager.deviceList
        if (!usbDevices?.isEmpty()!!) {
            var keep = true
            usbDevices.forEach{ entry ->
                m_device = entry.value
                val deviceVendorId: Int? = m_device?.vendorId
                Log.i("serial", "verdorId: "+deviceVendorId)
                Toast.makeText(this, "vendorId: "+deviceVendorId, Toast.LENGTH_SHORT).show()
                if (deviceVendorId == 9025) {
                    val intent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION),
                            FLAG_MUTABLE)
                    } else {
                        PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION),
                            0)
                    }
                    m_usbManager.requestPermission(m_device, intent)
                    keep = false
                    Log.i("serial", "connection successful")
                    Toast.makeText(this, "connection successful", Toast.LENGTH_SHORT).show()
                } else {
                    m_connection = null
                    m_device = null
                    Log.i("serial", "unable to connect")
                    Toast.makeText(this, "unable to connect", Toast.LENGTH_SHORT).show()
                }
                if (!keep) {
                    return
                }
            }
        } else {
            Log.i("serial", "no usb device connected")
            Toast.makeText(this, "no usb device connected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendData(input: String) {
        m_serial?.write(input.toByteArray())
        Log.i("serial", "sending data: "+input.toByteArray())
        Toast.makeText(this, "sending data: "+input.toByteArray(), Toast.LENGTH_SHORT).show()
    }

    private fun disconnect() {
        m_serial?.close()
        Log.i("serial", "Disconnected")
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action!! == ACTION_USB_PERMISSION) {
                val granted: Boolean = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    m_connection = m_usbManager.openDevice(m_device)
                    m_serial = UsbSerialDevice.createUsbSerialDevice(m_device, m_connection)
                    if (m_serial != null) {
                        if (m_serial!!.open()) {
                            m_serial!!.setBaudRate(9600)
                            m_serial!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                            m_serial!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
                            m_serial!!.setParity(UsbSerialInterface.PARITY_NONE)
                            m_serial!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                        } else {
                            Log.i("Serial", "port not open")
                            Toast.makeText(applicationContext, "port not open", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.i("Serial", "port is null")
                        Toast.makeText(applicationContext, "port is null", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.i("Serial", "permission not granted")
                    Toast.makeText(applicationContext, "permission not granted", Toast.LENGTH_SHORT).show()
                }
            } else if (intent.action == UsbManager.ACTION_USB_ACCESSORY_ATTACHED){
                startUsbConnecting()
            } else if (intent.action == UsbManager.ACTION_USB_ACCESSORY_DETACHED) {
                disconnect()
            }
        }
    }
}