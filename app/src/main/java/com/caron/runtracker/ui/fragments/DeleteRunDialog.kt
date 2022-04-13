package com.caron.runtracker.ui.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.caron.runtracker.R

class DeleteRunDialog : DialogFragment(){

    private var yesListener:(()->Unit)?=null

    fun setYesListener(listener:()->Unit){
        yesListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Delete The Run?")
            .setMessage("Are you sure you want to delete the current run and  all its data?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes"){_,_ ->
                yesListener?.let { yes ->
                    yes()

                }
            }
            .setNegativeButton("No"){dailogInterface, _ ->
                dailogInterface.cancel()

            }
            .create()

    }
}
