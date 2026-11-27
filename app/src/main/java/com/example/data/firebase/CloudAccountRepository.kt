package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudAccountRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id),
        ),
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                },
        )
    }

    suspend fun upsertUserProfile(
        displayName: String,
        preferredContactType: ContactMethod,
        contactEmail: String?,
        phoneNumber: String?,
        photoUrl: String?,
    ): Result<Unit> {
        var path = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().ifBlank { "RepoForge Developer" }.take(80)
            val cleanEmail = contactEmail?.trim()?.takeIf { it.isNotEmpty() }?.take(120)
            val cleanPhone = phoneNumber?.trim()?.takeIf { it.isNotEmpty() }?.take(30)
            val cleanPhoto = photoUrl?.trim()?.takeIf { it.isNotEmpty() }?.take(500)

            val docRef = db.collection("users").document(uid)
            path = docRef.path
            val existingSnap = docRef.get().await()

            if (!existingSnap.exists()) {
                val createPayload = mapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "preferredContactType" to preferredContactType.name,
                    "contactEmail" to cleanEmail,
                    "phoneNumber" to cleanPhone,
                    "photoUrl" to cleanPhoto,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ).filterValues { it != null }
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to cleanName,
                    "preferredContactType" to preferredContactType.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                if (cleanEmail != null) {
                    updatePayload["contactEmail"] = cleanEmail
                } else if (existingSnap.contains("contactEmail")) {
                    updatePayload["contactEmail"] = FieldValue.delete()
                }
                if (cleanPhone != null) {
                    updatePayload["phoneNumber"] = cleanPhone
                } else if (existingSnap.contains("phoneNumber")) {
                    updatePayload["phoneNumber"] = FieldValue.delete()
                }
                if (cleanPhoto != null) {
                    updatePayload["photoUrl"] = cleanPhoto
                }
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeSavedRepos(userId: String): Flow<List<CloudSavedRepo>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch." }
        val path = "users/$uid/saved_repos"
        emitAll(
            db.collection("users").document(uid).collection("saved_repos")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudSavedRepo::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                },
        )
    }

    suspend fun upsertSavedRepo(repo: CloudSavedRepo): Result<Unit> {
        var path = "users/unauthenticated/saved_repos"
        return try {
            val uid = requireUserId()
            val docId = CloudSavedRepo.docIdFor(repo.fullName)
            require(docId.isNotBlank()) { "Repository ID must not be blank." }
            val docRef = db.collection("users").document(uid)
                .collection("saved_repos").document(docId)
            path = docRef.path

            val cleanFullName = repo.fullName.trim().take(140)
            val cleanOwner = repo.owner.trim().take(60)
            val cleanRepo = repo.repo.trim().take(80)
            val cleanDesc = repo.description.trim().take(500)
            val cleanBranch = repo.defaultBranch.trim().ifBlank { "main" }.take(100)
            val cleanStack = repo.detectedStack.trim().ifBlank { "CUSTOM_MOBILE" }.take(40)
            val cleanAvatar = repo.avatarUrl.trim().take(500)
            val cleanHtml = repo.htmlUrl.trim().take(500)

            val existing = docRef.get().await()
            if (!existing.exists()) {
                val createPayload = mapOf<String, Any>(
                    "fullName" to cleanFullName,
                    "owner" to cleanOwner,
                    "repo" to cleanRepo,
                    "description" to cleanDesc,
                    "defaultBranch" to cleanBranch,
                    "detectedStack" to cleanStack,
                    "avatarUrl" to cleanAvatar,
                    "htmlUrl" to cleanHtml,
                    "supportsApk" to repo.supportsApk,
                    "supportsIpa" to repo.supportsIpa,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf<String, Any>(
                    "description" to cleanDesc,
                    "defaultBranch" to cleanBranch,
                    "detectedStack" to cleanStack,
                    "avatarUrl" to cleanAvatar,
                    "htmlUrl" to cleanHtml,
                    "supportsApk" to repo.supportsApk,
                    "supportsIpa" to repo.supportsIpa,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSavedRepo(fullName: String): Result<Unit> {
        var path = "users/unauthenticated/saved_repos"
        return try {
            val uid = requireUserId()
            val docId = CloudSavedRepo.docIdFor(fullName)
            val docRef = db.collection("users").document(uid)
                .collection("saved_repos").document(docId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
