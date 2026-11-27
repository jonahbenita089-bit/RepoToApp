package com.example.data.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ContactMethod(val label: String) {
    GOOGLE("Google"),
    EMAIL("Email"),
    PHONE("Phone Number");

    companion object {
        fun fromRaw(raw: String?): ContactMethod {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: GOOGLE
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val preferredContactType: String = ContactMethod.GOOGLE.name,
    val contactEmail: String? = null,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun resolvedContactMethod(): ContactMethod = ContactMethod.fromRaw(preferredContactType)
}

@Keep
@IgnoreExtraProperties
data class CloudSavedRepo(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val owner: String = "",
    val repo: String = "",
    val description: String = "",
    val defaultBranch: String = "main",
    val detectedStack: String = "",
    val avatarUrl: String = "",
    val htmlUrl: String = "",
    val supportsApk: Boolean = true,
    val supportsIpa: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    companion object {
        fun docIdFor(fullName: String): String {
            return fullName.replace(Regex("[^A-Za-z0-9_\\-]"), "_").take(128)
        }
    }
}
