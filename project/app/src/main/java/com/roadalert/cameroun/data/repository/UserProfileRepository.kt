package com.roadalert.cameroun.data.repository

import com.roadalert.cameroun.data.db.AppDatabase
import com.roadalert.cameroun.data.db.dao.EmergencyContactDAO
import com.roadalert.cameroun.data.db.dao.UserDAO
import com.roadalert.cameroun.data.db.entity.EmergencyContact
import com.roadalert.cameroun.data.db.entity.User
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(
    private val userDAO: UserDAO,
    private val emergencyContactDAO: EmergencyContactDAO
) {

    // ── User operations ──────────────────────────────────────

    fun getUser(): Flow<User?> {
        return userDAO.getUser()
    }

    suspend fun saveUser(user: User) {
        userDAO.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        userDAO.updateUser(
            user.copy(updatedAt = System.currentTimeMillis())
        )
    }

    // ── isProfileComplete — vérifie User ET Contact ───────────

    suspend fun isProfileComplete(): Boolean {
        val user = userDAO.getUserSync() ?: return false
        return emergencyContactDAO.getContactCount(user.id) > 0
    }

    // ── Sauvegarde atomique ───────────────────────────────────

    suspend fun saveProfileWithContacts(
        user: User,
        contacts: List<EmergencyContact>
    ) {
        userDAO.insertUser(user)
        contacts.forEach { contact ->
            emergencyContactDAO.insertContact(contact)
        }
    }

    // ── Emergency contacts operations ────────────────────────

    fun getContacts(userId: String): Flow<List<EmergencyContact>> {
        return emergencyContactDAO.getContactsByUser(userId)
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
        return emergencyContactDAO.getContactCount(userId) > 0
    }
}