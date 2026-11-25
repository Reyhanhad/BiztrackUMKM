package com.example.biztrackumkm.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AuthRoleTest {

    @Test
    fun `userRole enum has expected values`() {
        assertEquals(2, UserRole.entries.size)
        assertEquals(UserRole.OWNER, UserRole.valueOf("OWNER"))
        assertEquals(UserRole.CASHIER, UserRole.valueOf("CASHIER"))
    }

    @Test
    fun `authenticated state holds user role correctly`() {
        val ownerState = AuthScreenState.Authenticated(UserRole.OWNER)
        val cashierState = AuthScreenState.Authenticated(UserRole.CASHIER)

        assertEquals(UserRole.OWNER, ownerState.role)
        assertEquals(UserRole.CASHIER, cashierState.role)
        assertNotEquals(ownerState.role, cashierState.role)
    }

    @Test
    fun `ownerAccount default role is OWNER`() {
        val account = OwnerAccount(
            username = "pemilik1",
            passwordHash = "hash",
            passwordSalt = "salt"
        )

        assertEquals("OWNER", account.role)
    }

    @Test
    fun `cashierAccount has CASHIER role`() {
        val cashierAccount = OwnerAccount(
            username = "kasir1",
            passwordHash = "hash",
            passwordSalt = "salt",
            role = "CASHIER"
        )

        assertEquals("CASHIER", cashierAccount.role)
    }
}
