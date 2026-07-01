package com.onesec.interceptor.data.local

import androidx.room.TypeConverter
import com.onesec.interceptor.data.local.entity.InterceptOutcome

class Converters {
    @TypeConverter
    fun fromOutcome(outcome: InterceptOutcome): String = outcome.name

    @TypeConverter
    fun toOutcome(value: String): InterceptOutcome = InterceptOutcome.valueOf(value)
}
