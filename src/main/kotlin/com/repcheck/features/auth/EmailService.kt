package com.repcheck.features.auth

import java.util.*

interface EmailService {
    suspend fun sendVerificationEmail(email: String, token: String, expiresAt: Date)
    suspend fun sendPasswordResetEmail(email: String, token: String, expiresAt: Date)
}

class ConsoleEmailService : EmailService {
    override suspend fun sendVerificationEmail(email: String, token: String, expiresAt: Date) {
        val verificationUrl = "${getBaseUrl()}/verify-email?token=$token"
        val message = """
            ===========================================
            Email Verification
            ===========================================
            
            Please click the following link to verify your email:
            $verificationUrl
            
            This link will expire at: $expiresAt
            
            If you didn't request this, please ignore this email.
            ===========================================
        """.trimIndent()
        
        println("Sending verification email to $email:\n$message")
    }

    override suspend fun sendPasswordResetEmail(email: String, token: String, expiresAt: Date) {
        // Implementation for password reset email
        val resetUrl = "${getBaseUrl()}/reset-password?token=$token"
        val message = """
            ===========================================
            Password Reset
            ===========================================
            
            Please click the following link to reset your password:
            $resetUrl
            
            This link will expire at: $expiresAt
            
            If you didn't request this, please ignore this email.
            ===========================================
        """.trimIndent()
        
        println("Sending password reset email to $email:\n$message")
    }
    
    private fun getBaseUrl(): String {
        // In a real app, this should come from configuration
        return "http://localhost:8080"
    }
}

class AwsSesEmailService : EmailService {
    override suspend fun sendVerificationEmail(email: String, token: String, expiresAt: Date) {
        // TODO: Implement AWS SES email sending
        // This requires AWS credentials and configuration
        throw UnsupportedOperationException("AWS SES not implemented yet")
    }

    override suspend fun sendPasswordResetEmail(email: String, token: String, expiresAt: Date) {
        // Implementation for password reset email
        throw UnsupportedOperationException("AWS SES not implemented yet")
    }
}
