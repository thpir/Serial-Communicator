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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import com.felhr.usbserial.UsbSerialInterface.UsbReadCallback
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var mUsbManager: UsbManager
    var mDevice: UsbDevice? = null
    var mSerial: UsbSerialDevice? = null
    var mConnection: UsbDeviceConnection? = null
    private var dataSteam = ""
    private var newData = false
    private lateinit var tv1: TextView
    private lateinit var tv2: TextView
    private lateinit var tv3: TextView
    private lateinit var tv4: TextView
    private lateinit var tv5: TextView
    private lateinit var tv6: TextView
    private lateinit var tv7: TextView
    private lateinit var tv8: TextView
    private lateinit var tv9: TextView
    private lateinit var tv10: TextView
    private lateinit var spinner: Spinner
    private lateinit var textToTransmit: EditText
    lateinit var mainHandler: Handler
    private val list: MutableList<String> = MutableList(10) { "" }
    private val availableDevices = mutableListOf<String>()

    val permission = "permission"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setLogo(R.drawable.ic_usb)
        setSupportActionBar(toolbar)

        mUsbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        val filter = IntentFilter()
        filter.addAction(permission)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(broadcastReceiver, filter)

        mainHandler = Handler(Looper.getMainLooper())

        val send: Button = findViewById(R.id.buttonSend)
        val sendDateTime: Button = findViewById(R.id.buttonSendDateTime)
        val disconnect: Button = findViewById(R.id.buttonDisconnect)
        val connect: Button = findViewById(R.id.buttonConnect)
        val scan: Button = findViewById(R.id.buttonScan)
        textToTransmit = findViewById(R.id.editTextSend)
        tv1 = findViewById(R.id.textView1)
        tv2 = findViewById(R.id.textView2)
        tv3 = findViewById(R.id.textView3)
        tv4 = findViewById(R.id.textView4)
        tv5 = findViewById(R.id.textView5)
        tv6 = findViewById(R.id.textView6)
        tv7 = findViewById(R.id.textView7)
        tv8 = findViewById(R.id.textView8)
        tv9 = findViewById(R.id.textView9)
        tv10 = findViewById(R.id.textView10)
        spinner = findViewById(R.id.spinnerVendorId)
        // Create an ArrayAdapter using the string array and a default spinner layout
        populateSpinner(availableDevices)

        send.setOnClickListener {
            if (mUsbManager.openDevice(mDevice) != null) {
                val transmission: String = textToTransmit.text.toString()
                if (transmission != "") {
                    sendData(transmission)
                    textToTransmit.setText("")
                } else {
                    Log.i("serial", "No transmission text available")
                    Toast.makeText(this, "No transmission text available", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.i("serial", "No device connected")
                Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show()
            }
        }

        sendDateTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val time = calendar.timeInMillis
            if (mUsbManager.openDevice(mDevice) != null) {
                sendData(time.toString())
            } else {
                Log.i("serial", "No device connected")
                Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show()
            }
        }

        disconnect.setOnClickListener { disconnect() }

        connect.setOnClickListener { startUsbConnecting() }

        scan.setOnClickListener { startScanning() }
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(thread)
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(thread)
    }

    private fun populateSpinner(availableDevices: MutableList<String>) {
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            availableDevices
        ).also { arrayAdapter ->
            // Specify the layout to use when the list of choices appears
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = arrayAdapter
        }

    }

    private fun startScanning() {
        val usbDevices: HashMap<String, UsbDevice>? = mUsbManager.deviceList
        if (!usbDevices?.isEmpty()!!) {
            usbDevices.forEach{ entry ->
                mDevice = entry.value
                val deviceVendorId: Int? = mDevice?.vendorId
                Log.i("serial", "vendorId:  $deviceVendorId")
                Toast.makeText(this, "vendorId: $deviceVendorId", Toast.LENGTH_SHORT).show()
                // Add vendorId to mutableList
                availableDevices.add(deviceVendorId.toString())
            }
        } else {
            Log.i("serial", "no usb device connected")
            Toast.makeText(this, "no usb device connected", Toast.LENGTH_SHORT).show()
        }
        populateSpinner(availableDevices)
    }

    private fun startUsbConnecting() {
        val usbDevices: HashMap<String, UsbDevice>? = mUsbManager.deviceList
        if (!usbDevices?.isEmpty()!!) {
            var selectedItem = ""
            var selectedItemId = 0
            if (availableDevices.isNotEmpty()) {
                selectedItem = spinner.selectedItem.toString()
                selectedItemId = spinner.selectedItemPosition
            } else {
                Log.i("serial", "No device selected, scan for devices first")
                Toast.makeText(this, "No device selected, scan for devices first", Toast.LENGTH_SHORT).show()
            }
            var keep = true
            var i = 0
            usbDevices.forEach{ entry ->
                mDevice = entry.value
                val deviceVendorId: Int? = mDevice?.vendorId
                Log.i("serial", "verdorId: $deviceVendorId")
                Toast.makeText(this, "vendorId: $deviceVendorId", Toast.LENGTH_SHORT).show()
                if (deviceVendorId.toString() == selectedItem && i == selectedItemId) {
                    val intent: PendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.getBroadcast(this, 0, Intent(permission),
                            FLAG_MUTABLE)
                    } else {
                        PendingIntent.getBroadcast(this, 0, Intent(permission),
                            0)
                    }
                    mUsbManager.requestPermission(mDevice, intent)
                    keep = false
                    Log.i("serial", "connection successful")
                    Toast.makeText(this, "connection successful", Toast.LENGTH_SHORT).show()
                } else {
                    mConnection = null
                    mDevice = null
                    Log.i("serial", "unable to connect")
                    Toast.makeText(this, "unable to connect", Toast.LENGTH_SHORT).show()
                }
                if (!keep) {
                    return
                }
                i++
            }
        } else {
            Log.i("serial", "no usb device connected")
            Toast.makeText(this, "no usb device connected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendData(input: String) {
        mSerial?.write(input.toByteArray())
        Log.i("serial", "sending data: "+input.toByteArray())
        Toast.makeText(this, "sending data: "+input.toByteArray(), Toast.LENGTH_SHORT).show()
    }

    private fun disconnect() {
        mSerial?.close()
        mDevice = null
        Log.i("serial", "Disconnected")
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
        availableDevices.clear()
        populateSpinner(availableDevices)
    }

    private fun updateTv(dataStr: String) {
        list[9] = list[8]
        list[8] = list[7]
        list[7] = list[6]
        list[6] = list[5]
        list[5] = list[4]
        list[4] = list[3]
        list[3] = list[2]
        list[2] = list[1]
        list[1] = list[0]
        list[0] = dataStr
        tv1.text = list[0]
        tv2.text = list[1]
        tv3.text = list[2]
        tv4.text = list[3]
        tv5.text = list[4]
        tv6.text = list[5]
        tv7.text = list[6]
        tv8.text = list[7]
        tv9.text = list[8]
        tv10.text = list[9]
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action!! == permission) {
                val granted: Boolean = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                if (granted) {
                    mConnection = mUsbManager.openDevice(mDevice)
                    mSerial = UsbSerialDevice.createUsbSerialDevice(mDevice, mConnection)
                    if (mSerial != null) {
                        if (mSerial!!.open()) {
                            mSerial!!.setBaudRate(9600)
                            mSerial!!.setDataBits(UsbSerialInterface.DATA_BITS_8)
                            mSerial!!.setStopBits(UsbSerialInterface.STOP_BITS_1)
                            mSerial!!.setParity(UsbSerialInterface.PARITY_NONE)
                            mSerial!!.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                            mSerial!!.read(mCallback)
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

    var mCallback = UsbReadCallback { data: ByteArray? ->
        val dataStr = String(data!!)
        if (dataStr != "") {
            Log.i("serial", "Data received: $dataStr")
            dataSteam = dataStr
            newData = true
            //updateTv(dataStr)
        }
    }

    private val thread = object : Runnable {
        override fun run() {
            if (newData) {
                updateTv(dataSteam)
                newData = false
            }
            mainHandler.postDelayed(this, 1000)
        }
    }
}