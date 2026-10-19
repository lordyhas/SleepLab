package com.lordyhas.sleeplab.data.repository

import android.content.Context
import com.lordyhas.sleeplab.data.local.dao.SnoreEventDao
import java.io.File

/**
 * Snore audio clips: files under `filesDir/clips/<sessionId>/` (private, excluded from backup),
 * linked to their event through `snore_events.clipPath`.
 */
class ClipRepository(
    context: Context,
    private val snoreEventDao: SnoreEventDao,
    private val settings: SettingsRepository
) {
    private val root = File(context.filesDir, CLIPS_DIR)

    fun newClipFile(sessionId: Int, snoreStartMs: Long): File =
        File(File(root, sessionId.toString()), "$snoreStartMs.m4a")

    suspend fun attach(eventId: Int, file: File) = snoreEventDao.updateClipPath(eventId, file.absolutePath)

    /** Deletes one clip and unlinks it from its event. */
    suspend fun detach(eventId: Int, path: String?) {
        path?.let(::deleteFile)
        snoreEventDao.updateClipPath(eventId, null)
    }

    /** Removes all clip files of a night (its events are deleted with the night). */
    fun deleteSessionClips(sessionId: Int) {
        File(root, sessionId.toString()).deleteRecursively()
    }

    suspend fun deleteAll() {
        root.deleteRecursively()
        snoreEventDao.clearAllClipPaths()
    }

    /** Applies the auto-delete setting; returns how many clips were removed. */
    suspend fun purgeExpired(now: Long = System.currentTimeMillis()): Int {
        val days = settings.clipRetentionDays.value
        if (days <= 0) return 0
        val expired = snoreEventDao.getClipsOlderThan(now - days * DAY_MS)
        expired.forEach { deleteFile(it.clipPath) }
        if (expired.isNotEmpty()) snoreEventDao.clearClipPaths(expired.map { it.id })
        // Drop now-empty night folders
        root.listFiles()?.filter { it.isDirectory && it.list().isNullOrEmpty() }?.forEach { it.delete() }
        return expired.size
    }

    fun storageBytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    /** Only ever deletes inside the clips folder, whatever path the database holds. */
    private fun deleteFile(path: String) {
        val file = File(path)
        if (file.canonicalPath.startsWith(root.canonicalPath + File.separator)) file.delete()
    }

    private companion object {
        const val CLIPS_DIR = "clips"
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
