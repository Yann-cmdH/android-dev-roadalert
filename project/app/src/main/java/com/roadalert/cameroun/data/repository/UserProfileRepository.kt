package com.roadalert.cameroun.data.repository

import androidx.room.withTransaction
import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.db.dao.EmergencyContactDAO
import com.roadalert.cameroun.data.db.dao.UserDAO
import com.roadalert.cameroun.data.db.entity.EmergencyContact
import com.roadalert.cameroun.data.db.entity.User
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(
    private val database: AppDatabase,
    private val userDAO: UserDAO,
    private val emergencyContactDAO: EmergencyContactDAO
) {

    // ── User operations ───────────────────────────────────

    fun getUser(): Flow<User?> {
        return userDAO.getUser()
    }

    // Synchrone — utilisé par AlertManager
    // et BootCompletedReceiver
    suspend fun getUserSync(): User? {
        return userDAO.getUserSync()
    }

    suspend fun saveUser(user: User) {
        userDAO.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDAO.updateUser(
            user.copy(updatedAt = System.currentTimeMillis())
        )
    }

    // ── isProfileComplete ─────────────────────────────────

    suspend fun isProfileComplete(): Boolean {
        val user = userDAO.getUserSync() ?: return false
        return emergencyContactDAO
            .getContactCount(user.id) > 0
    }

    // ── Sauvegarde atomique ───────────────────────────────

    suspend fun saveProfileWithContacts(
        user: User,
        contacts: List<EmergencyContact>
    ) {
        database.withTransaction {
            emergencyContactDAO.deleteContactsByUser(user.id)
            userDAO.insertUser(user)
            contacts.forEach { contact ->
                emergencyContactDAO.insertContact(contact)
            }
        }
    }

    // ── Emergency contacts operations ─────────────────────

    fun getContacts(
        userId: String
    ): Flow<List<EmergencyContact>> {
        return emergencyContactDAO.getContactsByUser(userId)
    }

    // Synchrone — utilisé par AlertManager
    // Retourne contacts triés par priorité
    suspend fun getContactsSync(
        userId: String
    ): List<EmergencyContact> {
        return emergencyContactDAO
            .getContactsByUserSync(userId)
    }

    suspend fun saveContact(contact: EmergencyContact) {
        emergencyContactDAO.insertContact(contact)
    }

    suspend fun updateContact(contact: EmergencyContact) {
        emergencyContactDAO.updateContact(contact)
    }

    suspend fun deleteContact(contactId: String) {
        emergencyContactDAO.deleteById(contactId)
    }

    suspend fun hasContacts(userId: String): Boolean {
        return emergencyContactDAO
            .getContactCount(userId) > 0
    }
}