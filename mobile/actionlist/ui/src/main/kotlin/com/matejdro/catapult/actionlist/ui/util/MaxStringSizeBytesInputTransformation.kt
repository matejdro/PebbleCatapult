package com.matejdro.catapult.actionlist.ui.util

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.placeCursorAtEnd
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets

class MaxStringSizeBytesInputTransformation(private val maxBytes: Int) : InputTransformation {
   private val buffer = ByteBuffer.allocate(maxBytes)

   private val utf8Encoder = StandardCharsets.UTF_8.newEncoder()
   private val utf8Decoder = StandardCharsets.UTF_8.newDecoder()
   override fun TextFieldBuffer.transformInput() {
      val charBuffer = CharBuffer.wrap(this.toString())
      utf8Encoder.encode(charBuffer, buffer, true)
      buffer.rewind()
      val trimmedString = utf8Decoder.decode(buffer)

      if (length > trimmedString.length) {
         this.delete(trimmedString.length, length)
         this.placeCursorAtEnd()
      }
   }
}
