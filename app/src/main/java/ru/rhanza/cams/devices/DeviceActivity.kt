package ru.rhanza.cams.devices

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.rhanza.cams.data.Device
import ru.rhanza.cams.data.DevicesServer
import ru.rhanza.cams.databinding.ActivityDeviceBinding
import ru.rhanza.cams.databinding.ActivityMainBinding

class DeviceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeviceBinding

    private lateinit var devicesServer: DevicesServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        devicesServer = DevicesServer.get()
        val device = checkNotNull(intent.getParcelableExtra<Device>(DEVICE_KEY))

        binding.takePhoto.setOnClickListener {
            lifecycleScope.launch {
                binding.progressBar.isVisible = true
                val bitmap = devicesServer.takePhoto(device)
                binding.image.setImageBitmap(bitmap)
                binding.progressBar.isVisible = false
            }
        }


    }

    companion object {
        private const val DEVICE_KEY = "device"

        fun newIntent(context: Context, device: Device): Intent =
            Intent(context, DeviceActivity::class.java).apply {
                putExtra(DEVICE_KEY, device)
            }
    }
}