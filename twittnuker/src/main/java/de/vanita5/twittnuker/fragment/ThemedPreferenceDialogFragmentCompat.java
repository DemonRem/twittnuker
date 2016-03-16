package de.vanita5.twittnuker.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.view.View;
import android.view.Window;

import com.afollestad.materialdialogs.AlertDialogWrapper;

public abstract class ThemedPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();
        final DialogPreference preference = getPreference();
        onClick(null, DialogInterface.BUTTON_NEGATIVE);
        final AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(context)
                .setTitle(preference.getDialogTitle())
                .setIcon(preference.getDialogIcon())
                .setPositiveButton(preference.getPositiveButtonText(), this)
                .setNegativeButton(preference.getNegativeButtonText(), this);
        View contentView = onCreateDialogView(context);
        if (contentView != null) {
            onBindDialogView(contentView);
            builder.setView(contentView);
        } else {
            builder.setMessage(preference.getDialogMessage());
        }
        onPrepareDialogBuilder(builder);
        // Create the dialog
        final Dialog dialog = builder.create();
        if (needInputMethod()) {
            supportRequestInputMethod(dialog);
        }
        return builder.create();
    }

    @Override
    protected final void onPrepareDialogBuilder(AlertDialog.Builder builder) {

    }

    protected void onPrepareDialogBuilder(AlertDialogWrapper.Builder builder) {

    }

    private void supportRequestInputMethod(Dialog dialog) {
        Window window = dialog.getWindow();
        window.setSoftInputMode(5);
    }

    @Override
    public final void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
    }

}