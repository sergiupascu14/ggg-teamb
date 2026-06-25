package com.example.teamb.util

import com.example.teamb.data.util.Clock

/** Mutable fixed clock for deterministic time-dependent tests. */
class FakeClock(var now: Long) : Clock {
    override fun nowMillis(): Long = now
}
