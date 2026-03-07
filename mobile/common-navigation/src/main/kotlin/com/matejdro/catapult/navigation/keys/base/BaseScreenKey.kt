package com.matejdro.catapult.navigation.keys.base

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import com.matejdro.catapult.navigation.animation.PredictiveBackFadeAnimationSpec
import si.inova.kotlinova.navigation.screenkeys.ScreenKey

abstract class BaseScreenKey : ScreenKey() {
   @Suppress("MagicNumber") // Magic numbers are the whole point of this function
   override fun backAnimation(
      scope: AnimatedContentTransitionScope<*>,
      backSwipeEdge: Int?,
   ): ContentTransform {
      val scaleTransformOrigin = when (backSwipeEdge) {
         EDGE_LEFT -> TransformOrigin(pivotFractionX = 1f, pivotFractionY = 0.5f)
         EDGE_RIGHT -> TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0.5f)
         else -> TransformOrigin.Center
      }

      return (
         fadeIn(PredictiveBackFadeAnimationSpec()) +
            scaleIn(initialScale = 1.1f, transformOrigin = scaleTransformOrigin)
         ) togetherWith
         (
            fadeOut(PredictiveBackFadeAnimationSpec()) +
               scaleOut(targetScale = 0.9f, transformOrigin = scaleTransformOrigin)
            )
   }
}

/** Indicates that the edge swipe starts from the left edge of the screen */
internal const val EDGE_LEFT = 0

/** Indicates that the edge swipe starts from the right edge of the screen */
internal const val EDGE_RIGHT = 1

/**
 * Indicates that the back event was not triggered by an edge swipe back gesture. This
 * applies to cases like using the back button in 3-button navigation or pressing a hardware
 * back button.
 */
internal const val EDGE_NONE = 2
