package digital.paynetics.phos.screens

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import digital.paynetics.phos.PhosApplication
import digital.paynetics.phos.R
import digital.paynetics.phos.classes.helpers.NotifierWrapper
import digital.paynetics.phos.classes.lang.PhosString
import digital.paynetics.phos.classes.lang.PhosStringProvider
import digital.paynetics.phos.sdk.security.AuditLogger
import javax.inject.Inject

class SettingsLogoutDialogFragment : DialogFragment() {

    @Inject
    lateinit var stringProvider: PhosStringProvider

    @Inject
    lateinit var notifier: NotifierWrapper

    @Inject
    lateinit var auditLogger: AuditLogger

    override fun onAttach(context: Context) {
        PhosApplication.getAppComponent().inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(requireActivity(), R.style.AppTheme_AlertDialog)
//            .setView(0)
            .setMessage(stringProvider.getString(PhosString.log_out_confirmation))
            .setPositiveButton(stringProvider.getString(PhosString.log_out)) { _, _ ->
                notifier.sendEventToReceiver(BaseActivity.NOTIFICATION_LOGOUT)
                requireActivity().finishAffinity()
                auditLogger.logAudit(
                    TAG,
                    AuditLogger.Pair("message", "User logout.")
                )
            }
            .setNegativeButton(stringProvider.getString(PhosString.cancel)) { _, _ -> }
            .setCancelable(false)
            .create()

    companion object {
        private const val TAG = "SettingsFragment"
    }
}