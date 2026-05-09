package com.klaenerealestate.accountgateway

/**
 * One-time interactive Monarch Money authentication entry point.
 *
 * Replaced in Stage 2 with the real login flow (email/password/MFA → session JSON).
 * For Stage 1's scaffold, this is a stub that just prints "not implemented yet" so the
 * `:api:bootstrapMonarch` gradle task's `mainClass` reference resolves at compile time.
 */
object MonarchBootstrapMain {
    @JvmStatic
    fun main(args: Array<String>) {
        println("MonarchBootstrapMain: not implemented yet (Stage 2 wires the real login flow).")
    }
}
