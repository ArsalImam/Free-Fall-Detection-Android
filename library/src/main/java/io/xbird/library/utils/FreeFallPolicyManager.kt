package io.xbird.library.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import org.apache.commons.lang3.math.NumberUtils
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.sqrt

class FreeFallPolicyManager {
    fun isValidFall(event: SensorEvent?): Boolean {

        if (event?.sensor?.type === Sensor.TYPE_ACCELEROMETER) {
            val loX: Double = event.values?.get(NumberUtils.INTEGER_ZERO)?.toDouble() ?: NumberUtils.DOUBLE_ZERO
            val loY: Double = event.values?.get(NumberUtils.INTEGER_ONE)?.toDouble() ?: NumberUtils.DOUBLE_ZERO
            val loZ: Double = event.values?.get(2)?.toDouble() ?: NumberUtils.DOUBLE_ZERO
            val loAccelerationReader = sqrt(
                loX.pow(2.0) + loY.pow(2.0) + loZ.pow(2.0)
            )
            val precision = DecimalFormat("0.00")
            val ldAccRound: Double = precision.format(loAccelerationReader).toDouble()
            log("ldAccRound -> $ldAccRound")
            if (ldAccRound > 0.3 && ldAccRound < 0.7) {
                return true
            }
        }
        return false
    }
}