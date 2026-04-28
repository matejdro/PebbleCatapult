package com.matejdro.catapult.tasker

import si.inova.kotlinova.core.outcome.CauseException

class TaskerInvalidInputException(message: String) : CauseException(message, isProgrammersFault = false)
