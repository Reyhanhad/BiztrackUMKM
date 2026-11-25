package com.example.biztrackumkm.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthRepository(
    private val ownerDao: OwnerDao
) {

    private val secureRandom = SecureRandom()

    private val registrationMutex = Mutex()

    suspend fun hasOwner(): Boolean {
        return ownerDao.getOwnerCount() > 0
    }

    suspend fun registerOwner(
        username: String,
        password: String
    ): Result<Unit> {
        return registrationMutex.withLock {
            try {
                val cleanUsername = username.trim()

                when {
                    cleanUsername.length < 3 -> {
                        Result.failure(
                            IllegalArgumentException(
                                "Username minimal 3 karakter."
                            )
                        )
                    }

                    cleanUsername.length > 30 -> {
                        Result.failure(
                            IllegalArgumentException(
                                "Username maksimal 30 karakter."
                            )
                        )
                    }

                    password.length < 8 -> {
                        Result.failure(
                            IllegalArgumentException(
                                "Password minimal 8 karakter."
                            )
                        )
                    }

                    ownerDao.getOwnerCount() > 0 -> {
                        Result.failure(
                            IllegalStateException(
                                "Akun pemilik sudah terdaftar."
                            )
                        )
                    }

                    else -> {
                        val salt = ByteArray(16)
                        secureRandom.nextBytes(salt)

                        val saltEncoded = Base64.getEncoder()
                            .encodeToString(salt)

                        val hashEncoded = hashPassword(
                            password = password,
                            salt = salt
                        )

                        val owner = OwnerAccount(
                            username = cleanUsername,
                            passwordHash = hashEncoded,
                            passwordSalt = saltEncoded,
                            role = "OWNER"
                        )

                        ownerDao.insertOwner(owner)

                        Result.success(Unit)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun registerCashier(
        username: String,
        password: String
    ): Result<Unit> {
        return registrationMutex.withLock {
            try {
                val cleanUsername = username.trim()

                when {
                    cleanUsername.length < 3 -> {
                        Result.failure(
                            IllegalArgumentException(
                                "Username minimal 3 karakter."
                            )
                        )
                    }

                    cleanUsername.length > 30 -> {
                        Result.failure(
                            IllegalArgumentException(
                                "Username maksimal 30 karakter."
                            )
                        )
                    }

                    password.length < 8 -> {
                        Result.failure(
                            IllegalArgumentException(
                                "Password minimal 8 karakter."
                            )
                        )
                    }

                    ownerDao.countByUsername(cleanUsername) > 0 -> {
                        Result.failure(
                            IllegalArgumentException(
                                "Username sudah digunakan."
                            )
                        )
                    }

                    else -> {
                        val salt = ByteArray(16)
                        secureRandom.nextBytes(salt)

                        val saltEncoded = Base64.getEncoder()
                            .encodeToString(salt)

                        val hashEncoded = hashPassword(
                            password = password,
                            salt = salt
                        )

                        val cashier = OwnerAccount(
                            username = cleanUsername,
                            passwordHash = hashEncoded,
                            passwordSalt = saltEncoded,
                            role = "CASHIER"
                        )

                        ownerDao.insertOwner(cashier)

                        Result.success(Unit)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun login(
        username: String,
        password: String
    ): Result<UserRole> {
        return try {
            val account = ownerDao.getOwnerByUsername(
                username.trim()
            ) ?: return Result.failure(
                IllegalArgumentException(
                    "Username atau password salah."
                )
            )

            val salt = Base64.getDecoder().decode(
                account.passwordSalt
            )

            val enteredHash = hashPassword(
                password = password,
                salt = salt
            )

            val savedHash = Base64.getDecoder().decode(
                account.passwordHash
            )

            val enteredHashBytes = Base64.getDecoder().decode(
                enteredHash
            )

            if (
                MessageDigest.isEqual(
                    savedHash,
                    enteredHashBytes
                )
            ) {
                val role = if (account.role == "CASHIER") {
                    UserRole.CASHIER
                } else {
                    UserRole.OWNER
                }
                Result.success(role)
            } else {
                Result.failure(
                    IllegalArgumentException(
                        "Username atau password salah."
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCashierAccounts(): List<OwnerAccount> {
        return ownerDao.getCashierAccounts()
    }

    private fun hashPassword(
        password: String,
        salt: ByteArray
    ): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            210_000,
            256
        )

        return try {
            val factory = SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA256"
            )

            val hash = factory.generateSecret(spec).encoded

            Base64.getEncoder().encodeToString(hash)
        } finally {
            spec.clearPassword()
        }
    }
}
