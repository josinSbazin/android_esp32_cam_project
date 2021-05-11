package ru.rhanza.cams.devices

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
        title = device.name

        binding.takePhoto.setOnClickListener {
            lifecycleScope.launch {
                binding.progressBar.isVisible = true
                binding.takePhoto.isEnabled = false
                try {
                    val bitmap = withTimeout(TEN_SEC) {
                        devicesServer.takePhoto(device)
                    }
                    binding.imageSize.text = "${bitmap.width}x${bitmap.height}"
                    binding.image.setImageBitmap(bitmap)
                    binding.progressBar.isVisible = false
                    binding.takePhoto.isEnabled = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        binding.root.context,
                        "Take a photo failed!",
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.progressBar.isVisible = false
                    binding.takePhoto.isEnabled = true
                }

            }
        }

        lifecycleScope.launch {
            devicesServer.devices.collect {
                switchEnabled(device in it)
            }
        }

        lifecycleScope.launch {
            devicesServer.state.collect {
                switchEnabled(it)
            }
        }
    }

    private fun switchEnabled(isEnabled: Boolean) {
        if (isEnabled) {
            binding.takePhoto.text = "Take photo"
            binding.takePhoto.isEnabled = true
        } else {
            binding.takePhoto.text = "Not connected"
            binding.takePhoto.isEnabled = false
        }
        binding.progressBar.isVisible = false
    }

    companion object {
        private const val TEN_SEC = 10000L
        private const val DEVICE_KEY = "device"

        fun newIntent(context: Context, device: Device): Intent =
            Intent(context, DeviceActivity::class.java).apply {
                putExtra(DEVICE_KEY, device)
            }
    }
}