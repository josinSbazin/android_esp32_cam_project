package ru.rhanza.cams.devices

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.rhanza.cams.Router
import ru.rhanza.cams.data.Device
import ru.rhanza.cams.databinding.ItemDeviceBinding

class DevicesAdapter(private val router: Router) :
    ListAdapter<Device, DevicesAdapter.ViewHolder>(Callback) {

    inner class ViewHolder(private val binding: ItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun setData(data: Device) {
            binding.name.text = data.name
            binding.root.setOnClickListener {
                router.toDevicePage(data)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemDeviceBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(getItem(position))
    }
}

object Callback : DiffUtil.ItemCallback<Device>() {
    override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
        return oldItem == newItem
    }
}