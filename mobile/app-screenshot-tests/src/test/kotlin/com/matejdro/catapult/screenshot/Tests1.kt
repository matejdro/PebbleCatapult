package com.matejdro.catapult.screenshot

import com.google.testing.junit.testparameterinjector.TestParameter
import org.junit.Test
import screenshot.TestsBase

class Tests1 : TestsBase() {
   @Test
   public override fun test(
      @TestParameter(valuesProvider = PreviewProvider::class)
      @SplitIndex(0)
      testKey: TestKey,
   ) {
      super.test(testKey)
   }
}
