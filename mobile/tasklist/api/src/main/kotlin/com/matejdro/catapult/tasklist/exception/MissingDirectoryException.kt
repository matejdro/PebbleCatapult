package com.matejdro.catapult.tasklist.exception

import si.inova.kotlinova.core.outcome.CauseException

class MissingDirectoryException : CauseException(isProgrammersFault = false)
