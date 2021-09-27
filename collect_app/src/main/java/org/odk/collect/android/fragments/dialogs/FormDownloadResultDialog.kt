package org.odk.collect.android.fragments.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.jetbrains.annotations.TestOnly
import org.odk.collect.android.R
import org.odk.collect.android.activities.FormsDownloadErrorActivity
import org.odk.collect.android.logic.FormDownloadErrorItem
import java.util.ArrayList

class FormDownloadResultDialog : DialogFragment() {
    companion object {
        const val FAILURES = "FAILURES"
        const val NUMBER_OF_ALL_FORMS = "NUMBER_OF_ALL_FORMS"
    }

    private lateinit var failures: ArrayList<FormDownloadErrorItem>
    private var numberOfAllForms = 0

    private lateinit var listener: FormDownloadResultDialogListener

    interface FormDownloadResultDialogListener {
        fun onFormDownloadResultDialogOkButtonClicked()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FormDownloadResultDialogListener) {
            listener = context
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        isCancelable = false

        failures = arguments?.getSerializable(FAILURES) as ArrayList<FormDownloadErrorItem>
        numberOfAllForms = arguments?.getInt(NUMBER_OF_ALL_FORMS)!!

        val builder = AlertDialog.Builder(requireContext())
            .setMessage(getMessage())
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                listener.onFormDownloadResultDialogOkButtonClicked()
            }

        if (failures.isNotEmpty()) {
            builder.setNegativeButton(getString(R.string.show_details)) { _, _ ->
                val intent = Intent(context, FormsDownloadErrorActivity::class.java).apply {
                    putExtra(FormsDownloadErrorActivity.FAILURES, failures)
                }
                startActivity(intent)
            }
        }

        return builder.create()
    }

    private fun getMessage(): String {
        return if (failures.isEmpty()) {
            getString(R.string.all_downloads_succeeded)
        } else {
            getString(R.string.some_downloads_failed, failures.size.toString(), numberOfAllForms.toString())
        }
    }

    @TestOnly
    fun setListener(listener: FormDownloadResultDialogListener) {
        this.listener = listener
    }
}
