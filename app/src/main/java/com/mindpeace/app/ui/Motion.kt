package com.mindpeace.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

object Motion {
    const val TWEEN_MS = 320
    val tweenSpec = tween<Float>(durationMillis = TWEEN_MS)
    val tweenInt = tween<IntOffset>(durationMillis = TWEEN_MS)
    val springOffset = spring<IntOffset>(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy,
    )
    val springFloat = spring<Float>(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioNoBouncy,
    )

    fun navEnter() = fadeIn(tweenSpec) + slideInHorizontally(springOffset) { it / 10 }
    fun navExit() = fadeOut(tween(280))
    fun navPopEnter() = fadeIn(tweenSpec)
    fun navPopExit() = fadeOut(tween(240)) + slideOutHorizontally(springOffset) { it / 10 }

    fun paneTransform() = (
        fadeIn(tweenSpec) + scaleIn(initialScale = 0.96f, animationSpec = tweenSpec)
        ).togetherWith(fadeOut(tween(220)) + scaleOut(targetScale = 0.98f, animationSpec = tween(220)))
}
