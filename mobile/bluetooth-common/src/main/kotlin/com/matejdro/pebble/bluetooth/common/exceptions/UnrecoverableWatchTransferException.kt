package com.matejdro.pebble.bluetooth.common.exceptions

import si.inova.kotlinova.core.outcome.CauseException

class UnrecoverableWatchTransferException(
   message: String? = null,
   cause: Throwable? = null,
) : CauseException(message, cause, isProgrammersFault = true)
