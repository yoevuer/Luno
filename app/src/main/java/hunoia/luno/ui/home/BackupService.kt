package hunoia.luno.ui.home

import android.content.Context
import android.net.Uri
import hunoia.luno.R
import hunoia.luno.config.backup.BackupOperator

object BackupService {
    suspend fun backup(context: Context, saveTo: Uri, toast: (Int) -> Unit) {
        BackupOperator.backup(context, saveTo)
        toast(R.string.backup_success)
    }

    suspend fun restore(context: Context, restoreFrom: Uri, toast: (Int) -> Unit) {
        BackupOperator.restore(context, restoreFrom)
        toast(R.string.restore_success)
    }
}
