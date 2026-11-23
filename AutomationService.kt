package com.autolauncher

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AutomationService : AccessibilityService() {

    companion object {
        @Volatile var instance: AutomationService? = null
        @Volatile private var worker: Thread? = null

        fun run(s: Scenario) {
            val svc = instance ?: return
            stop()
            worker = Thread { svc.execute(s) }.also { it.start() }
        }
        fun stop() { worker?.interrupt(); worker = null }
    }

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() { stop() }
    override fun onDestroy() { stop(); instance = null; super.onDestroy() }

    private fun execute(s: Scenario) {
        try {
            for (step in s.steps) {
                when (step) {
                    is Step.Wait -> Thread.sleep(step.ms)
                    is Step.Tap -> { tap(step.x, step.y); Thread.sleep(700) }
                    is Step.Type -> { typeText(step.text); Thread.sleep(500) }
                    is Step.Chat -> {
                        val (cx, cy) = Scenarios.chatButton(); tap(cx, cy); Thread.sleep(800)
                        typeText(step.text); Thread.sleep(400)
                        val (sx, sy) = Scenarios.chatSend(); tap(sx, sy); Thread.sleep(1200)
                    }
                }
            }
        } catch (_: InterruptedException) { }
    }

    private fun tap(fx: Float, fy: Float) {
        val m: DisplayMetrics = resources.displayMetrics
        val p = Path().apply { moveTo(fx * m.widthPixels, fy * m.heightPixels) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 80)).build()
        val latch = CountDownLatch(1)
        dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { latch.countDown() }
            override fun onCancelled(d: GestureDescription?) { latch.countDown() }
        }, null)
        latch.await(2, TimeUnit.SECONDS)
    }

    private fun typeText(text: String) {
        val node: AccessibilityNodeInfo? = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        node?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        })
    }
}
