package ru.rhanza.cams.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.hivemq.client.internal.mqtt.message.auth.MqttSimpleAuthBuilder
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5ConnAckException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit

interface DevicesServer {
    val devices: Flow<List<Device>>
    val state: Flow<Boolean>

    suspend fun takePhoto(device: Device): Bitmap
    suspend fun connect(): Boolean
    suspend fun disconnect()

    companion object {
        fun get(): DevicesServer {
            return DeviceServerImpl
        }
    }
}

object DeviceServerImpl : DevicesServer {
    private const val STATUS_TOPIC = "cams/status/#"
    private const val PHOTO_TOPIC_PREFIX = "cams/photo/"
    private const val TAKE_PHOTO_TOPIC_PREFIX = "cams/take_photo/"

    private val client: Mqtt5BlockingClient = MqttClient.builder()
        .identifier("phone/${UUID.randomUUID()}")
        .serverHost(Secret.host)
        .serverPort(8883)
        .sslWithDefaultConfig()
        .useMqttVersion5()
        .automaticReconnect()
        .initialDelay(3, TimeUnit.SECONDS)
        .maxDelay(10, TimeUnit.SECONDS)
        .applyAutomaticReconnect()
        .addConnectedListener {
            _state.value = true
        }
        .addDisconnectedListener {
            _state.value = false
        }
        .simpleAuth(
            MqttSimpleAuthBuilder.Default()
                .username(Secret.username)
                .password(Secret.pass.toByteArray())
                .build()
        )
        .buildBlocking()

    private val devicesSet: MutableSet<Device> = Collections.newSetFromMap(ConcurrentHashMap())

    override val devices: Flow<List<Device>>
        get() = callbackFlow {
            client.toAsync()
                .subscribeWith()
                .topicFilter(STATUS_TOPIC)
                .callback { publish ->
                    val isOnline = String(publish.payloadAsBytes).toBoolean()
                    val deviceName = publish.topic.levels.last()
                    val device = Device(deviceName)
                    if (isOnline) {
                        devicesSet.add(device)
                    } else {
                        devicesSet.remove(device)
                    }
                    trySendBlocking(devicesSet.sortedBy { it.name })
                }
                .send()

            awaitClose {
                client.toAsync()
                    .unsubscribeWith()
                    .topicFilter(STATUS_TOPIC)
                    .send()
            }
        }

    private val _state: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val state: Flow<Boolean> = _state

    override suspend fun connect() = withContext(Dispatchers.IO) {
        try {
            client.connect()
            true
        } catch (e: Mqtt5ConnAckException) {
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        client.disconnect()
        _state.value = false
    }

    override suspend fun takePhoto(device: Device) =
        suspendCancellableCoroutine<Bitmap> { continuation ->
            client.toAsync()
                .subscribeWith()
                .topicFilter(PHOTO_TOPIC_PREFIX + device.name)
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback {
                    client.toAsync().unsubscribeWith()
                        .topicFilter(PHOTO_TOPIC_PREFIX + device.name)

                    if (continuation.isActive) {
                        continuation.resumeWith(
                            Result.success(
                                BitmapFactory.decodeByteArray(
                                    it.payloadAsBytes,
                                    0,
                                    it.payloadAsBytes.size
                                )
                            )
                        )
                    }
                }
                .send()

            client.publishWith()
                .topic(TAKE_PHOTO_TOPIC_PREFIX + device.name)
                .qos(MqttQos.AT_LEAST_ONCE)
                .send()

            continuation.invokeOnCancellation {
                client.toAsync().unsubscribeWith()
                    .topicFilter(PHOTO_TOPIC_PREFIX + device.name)
            }
        }
}