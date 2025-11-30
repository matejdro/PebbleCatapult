package com.matejdro.catapult.bluetooth.exceptions

import si.inova.kotlinova.core.outcome.CauseException

class UnrecoverableWatchTransferException(
   message: String? = null,
   cause: Throwable? = null,
) : CauseException(message, cause, isProgrammersFault = true)
