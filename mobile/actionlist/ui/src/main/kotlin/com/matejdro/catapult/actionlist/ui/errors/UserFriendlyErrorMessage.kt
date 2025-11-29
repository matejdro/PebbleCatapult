package com.matejdro.catapult.actionlist.ui.errors

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.matejdro.catapult.actionlist.exception.MissingDirectoryException
import com.matejdro.catapult.actionlist.ui.R
import com.matejdro.catapult.ui.errors.commonUserFriendlyMessage
import si.inova.kotlinova.core.outcome.CauseException

@Composable
fun CauseException.taskListUserFriendlyMessage(
   hasExistingData: Boolean = false,
): String {
   return if (this is MissingDirectoryException) {
      stringResource(R.string.this_directory_does_not_exist_anymore)
   } else {
      commonUserFriendlyMessage(hasExistingData)
   }
}
