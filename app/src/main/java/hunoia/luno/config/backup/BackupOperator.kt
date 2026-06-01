package hunoia.luno.config.backup
import hunoia.luno.config.ConfigProvider

import android.content.Context
import android.net.Uri
import hunoia.luno.core.Paths
import hunoia.luno.core.JsonSerializer
import hunoia.luno.config.model.Backup
import android.util.Base64
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

object BackupOperator {

    private const val ZIP_BACKUP = "backup"
    private const val ZIP_IMAGES = "images"

    private val backupDir = "${Paths.AppCache}/backup"
    private val restoreDir = "${Paths.AppCache}/restore"

    private val backupItemFilePath = "$backupDir/$ZIP_BACKUP"
    private val zipImagePath = "$backupDir/$ZIP_IMAGES"
    private val zipFilePath = "$backupDir/zip"
    private val restoreFilePath = "$restoreDir/restore"

    suspend fun backup(context: Context, saveTo: Uri) {
        try {
            File(backupDir).mkdirs()

            val backupItemBytes = getBackupItemBytes()
            val backupItemFile = File(backupItemFilePath).also {
                it.delete()
                it.createNewFile()
                it.appendBytes(backupItemBytes)
            }

            val zipImageDirFile = File(zipImagePath).also {
                it.delete()
                it.createNewFile()
            }
            val imageFiles = File(Paths.Image).listFiles()?.toList() ?: emptyList()
            ZipOutputStream(FileOutputStream(zipImageDirFile)).use { zos ->
                for (file in imageFiles) {
                    zos.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            val zipFile = File(zipFilePath).also {
                it.delete()
                it.createNewFile()
            }
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for (file in listOf(backupItemFile, zipImageDirFile)) {
                    zos.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }

            context.contentResolver.openOutputStream(saveTo)!!.use { outputStream ->
                val zipFileBytes = zipFile.readBytes()
                outputStream.write(zipFileBytes)
                outputStream.flush()
            }
        } catch (ex: Exception) {
            throw ex
        } finally {
            File(backupDir).deleteRecursively()
        }
    }

    suspend fun restore(context: Context, restoreFrom: Uri) {
        try {
            context.contentResolver.openInputStream(restoreFrom)!!.use { inputStream ->
                val input = inputStream.readBytes()
                val restoreDirFile = File(restoreDir).also {
                    it.mkdirs()
                }
                val restoreFile = File(restoreFilePath).also {
                    it.delete()
                    it.createNewFile()
                    it.appendBytes(input)
                }
                val extracted = unzipFile(restoreFile, restoreDirFile)
                var restored = false
                var imagesRestored = false
                for (file in extracted) {
                    when (file.name) {
                        ZIP_BACKUP -> {
                            restoreBackupFromBytes(context, file.readBytes())
                            restored = true
                        }
                        ZIP_IMAGES -> {
                            File(Paths.Image).deleteRecursively()
                            File(Paths.Image).mkdirs()
                            unzipFile(file, restoreDirFile).forEach { imageFile ->
                                val destFile = File("${Paths.Image}/${imageFile.name}")
                                imageFile.copyTo(destFile, overwrite = true)
                            }
                            imagesRestored = true
                        }
                    }
                }
                if (!restored) throw IllegalStateException("restore failed: no backup entry found in zip")
                if (!imagesRestored) throw IllegalStateException("restore failed: no images entry found in zip")
            }
        } catch (ex: Exception) {
            throw ex
        } finally {
            File(restoreDir).deleteRecursively()
        }
    }

    private suspend fun getBackupItemBytes(): ByteArray {
        val backup = ConfigProvider.snapshotAll()
        val json = JsonSerializer.encodeToString(backup)
        return Base64.encode(json.toByteArray(), Base64.NO_WRAP)
    }

    private suspend fun restoreBackupFromBytes(context: Context, bytes: ByteArray) {
        val decoded = Base64.decode(bytes, Base64.NO_WRAP)
        val backup = JsonSerializer.decodeFromString<Backup>(String(decoded))
        if (backup.initialSettings == null && backup.advancedSettings == null &&
            backup.gestureSettings == null && backup.actionSettings == null &&
            backup.gestureButtons == null &&
            backup.quickAppLauncherSettings == null && backup.frozenAppSettings == null &&
            backup.subGestureSettings == null
        ) {
            throw IllegalStateException("restore failed: backup contains no settings")
        }
        val installedPackages = queryInstalledPackageNames(context)
        val sanitizedFrozenSettings = sanitizeFrozenAppSettings(
            backup.frozenAppSettings,
            installedPackages
        )
        val modifiedBackup = if (sanitizedFrozenSettings != null) {
            backup.copy(frozenAppSettings = sanitizedFrozenSettings)
        } else {
            backup
        }
        ConfigProvider.restoreAll(modifiedBackup)
        val verified = ConfigProvider.snapshotAll()
        if (verified.initialSettings != modifiedBackup.initialSettings) {
            throw IllegalStateException("restore failed: initialSettings mismatch after restoreAll")
        }
    }

    private fun sanitizeFrozenAppSettings(
        settings: hunoia.luno.config.model.FrozenAppSettings?,
        installedPackages: Set<String>
    ): hunoia.luno.config.model.FrozenAppSettings? {
        settings ?: return null
        val oneKey = settings.oneKeyPackageNames
            .filterTo(mutableSetOf()) { it in installedPackages }
        return settings.copy(oneKeyPackageNames = oneKey)
    }

    private fun queryInstalledPackageNames(context: Context): Set<String> {
        val pm = context.packageManager
        return try {
            val apps = pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            apps.map { it.packageName }.filter { it.isNotBlank() }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun unzipFile(zipFile: File, destDir: File): List<File> {
        val extracted = mutableListOf<File>()
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val target = File(destDir, entry.name)
                if (!entry.isDirectory) {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { fos -> zis.copyTo(fos) }
                }
                extracted.add(target)
                entry = zis.nextEntry
            }
        }
        return extracted
    }
}
