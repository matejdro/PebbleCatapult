package com.matejdro.catapult.bluetooth.util

import io.rebble.pebblekit2.common.model.PebbleDictionary
import io.rebble.pebblekit2.common.model.PebbleDictionaryItem

fun PebbleDictionary.requireUint(key: UInt): UInt {
   return (this[key] as PebbleDictionaryItem.UInt32).value
}

fun PebbleDictionary.requireInt(key: UInt): Int {
   return (this[key] as PebbleDictionaryItem.Int32).value
}

fun PebbleDictionary.requireBytes(key: UInt): ByteArray {
   return (this[key] as PebbleDictionaryItem.ByteArray).value
}
