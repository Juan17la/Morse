package com.morse.wearos.transport.socket

import com.morse.wearos.transport.Envelope
import com.morse.wearos.transport.Protocol
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.Socket

/** A socket that speaks one [Envelope] per line. Blocking; call from Dispatchers.IO. */
class EnvelopeSocket(private val socket: Socket) {
    private val reader: BufferedReader = socket.getInputStream().bufferedReader()
    private val writer: BufferedWriter = socket.getOutputStream().bufferedWriter()
    private val writeLock = Mutex()

    val remoteAddress: String get() = socket.inetAddress?.hostAddress ?: "?"

    /** Returns null when the peer closed the connection. */
    fun read(): Envelope? {
        val line = reader.readLine() ?: return null
        return Protocol.decode(line)
    }

    suspend fun write(envelope: Envelope) {
        writeLock.withLock {
            writer.write(Protocol.encode(envelope))
            writer.newLine()
            writer.flush()
        }
    }

    fun close() {
        runCatching { socket.close() }
    }
}
