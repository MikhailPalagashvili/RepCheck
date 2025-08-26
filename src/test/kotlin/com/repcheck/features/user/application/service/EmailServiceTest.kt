package com.repcheck.features.user.application.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.*

class EmailServiceTest : StringSpec({
    "test console email service" {
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out

        try {
            // Redirect System.out to capture output
            System.setOut(PrintStream(outputStream))

            val service = ConsoleEmailService()
            val testDate = Date()
            service.sendVerificationEmail("test@example.com", "test-token-123", testDate)

            val expectedOutput = """
                Sending verification email to: test@example.com
                Verification token: test-token-123
                Expires at: $testDate
                Verification link: http://localhost:8080/api/auth/verify?token=test-token-123
            """.trimIndent()

            outputStream.toString().trim() shouldBe expectedOutput.trim()
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    "test functional interface implementation" {
        // Test data
        val testEmail = "test@example.com"
        val testToken = "test-token-456"
        val testDate = Date()

        // Variables to capture the lambda parameters
        var capturedEmail = ""
        var capturedToken = ""
        var capturedDate: Date? = null

        // Create an EmailService using the functional interface
        val service = EmailService { email, token, expiresAt ->
            capturedEmail = email
            capturedToken = token
            capturedDate = expiresAt
        }

        // When: We call the service
        service.sendVerificationEmail(testEmail, testToken, testDate)

        // Then: The captured values should match what we passed in
        capturedEmail shouldBe testEmail
        capturedToken shouldBe testToken
        capturedDate shouldBe testDate
    }
})
