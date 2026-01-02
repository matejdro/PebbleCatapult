@file:Suppress("NOTHING_TO_INLINE")

package com.matejdro.pebble.bluetooth.common.util

import okio.BufferedSink
import okio.BufferedSource
import okio.IOException

// Adapted from the https://github.com/square/okio/pull/536/files

@Throws(IOException::class)
inline fun <S : BufferedSink> S.writeUByte(b: UByte): S {
   writeByte(b.toByte().toInt())
   return this
}

@Throws(IOException::class)
inline fun <S : BufferedSink> S.writeUShort(b: UShort): S {
   writeShort(b.toShort().toInt())
   return this
}

@Throws(IOException::class)
inline fun <S : BufferedSink> S.writeUShortLe(b: UShort): S {
   writeShortLe(b.toShort().toInt())
   return this
}

@Throws(IOException::class)
inline fun <S : BufferedSink> S.writeUInt(b: UInt): S {
   writeInt(b.toInt())
   return this
}

@Throws(IOException::class)
inline fun <S : BufferedSink> S.writeUIntLe(b: UInt): S {
   writeIntLe(b.toInt())
   return this
}

@Throws(IOException::class)
inline fun <S : BufferedSink> S.writeULong(b: ULong): S {
   writeLong(b.toLong())
   return this
}

@Throws(IOException::class)
inline fun <S : BufferedSink> S.writeULongLe(b: ULong): S {
   writeLongLe(b.toLong())
   return this
}

@Throws(IOException::class)
inline fun BufferedSource.readUByte(): UByte {
   return readByte().toUByte()
}

@Throws(IOException::class)
inline fun BufferedSource.readUShort(): UShort {
   return readShort().toUShort()
}

@Throws(IOException::class)
inline fun BufferedSource.readUShortLe(): UShort {
   return readShortLe().toUShort()
}

@Throws(IOException::class)
inline fun BufferedSource.readUInt(): UInt {
   return readInt().toUInt()
}

@Throws(IOException::class)
inline fun BufferedSource.readUIntLe(): UInt {
   return readIntLe().toUInt()
}

@Throws(IOException::class)
inline fun BufferedSource.readULong(): ULong {
   return readLong().toULong()
}

@Throws(IOException::class)
inline fun BufferedSource.readULongLe(): ULong {
   return readLongLe().toULong()
}
