package com.khronodragon.android.chatengine.models

data class APIResponse(val success: Boolean, val response: String,
                       val confidence: Double, val error: String? = null)