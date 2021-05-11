package ru.rhanza.cams

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ru.rhanza.cams.data.DevicesServer
import ru.rhanza.cams.databinding.ActivityMainBinding
import ru.rhanza.cams.devices.DevicesAdapter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DevicesAdapter
    private lateinit var devicesServer: DevicesServer
    private lateinit var router: Router

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        router = Router(this)

        adapter = DevicesAdapter(router)
        binding.recyclerView.adapter = adapter

        devicesServer = DevicesServer.get()

        lifecycleScope.launch {
            connect()
        }

        lifecycleScope.launch {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            devicesServer.disconnect()
        }
    }

    private suspend fun connect() {
        binding.connectionStatus.text = "Connecting"
        binding.progress.isVisible = true

        if (!devicesServer.connect()) {
            binding.connectionStatus.text = "Connecting error!"
            binding.progress.isVisible = false
        }

        lifecycleScope.launch {
            devicesServer.state.collect {
                if (it) {
                    binding.connectionStatus.text = "Connected"
                } else {
                    binding.connectionStatus.text = "Disconnected or reconnecting"
                }
                binding.progress.isVisible = false
            }
        }

        lifecycleScope.launch {
            devicesServer.devices.collect {
                adapter.submitList(it)
            }
        }
    }
}