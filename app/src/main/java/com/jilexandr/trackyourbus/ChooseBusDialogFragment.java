package com.jilexandr.trackyourbus;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;

import java.util.ArrayList;
import java.util.List;

interface ConfirmResult {
    public void on(List<BusRoute> selectedItems);
}

class BusRoute {
    public String id;
    public String title;
}

public class ChooseBusDialogFragment extends DialogFragment {

    final ArrayList selectedItems = new ArrayList();
    private ConfirmResult confirmResultCallback = null;
    List<BusRoute> busRouteList = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final CharSequence[] items = {"№9", "№11", "№21", "№22", "№29", "№36"};

        builder
                .setPositiveButton(R.string.subscribe, new PositiveButtonClickListener(selectedItems))
                .setView(inflater.inflate(R.layout.choose_bus, null))
                .setTitle(R.string.title_modal_choose_bus)
                .setMultiChoiceItems(items, null, new MyOnMultiChoiceClickListener(selectedItems, items))
                .setNegativeButton(R.string.cancel, new MyOnClickListener());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(new MyOnShowListener());

        return dialog;
    }

    public void setOnConfirmResult(ConfirmResult confirmResultCallback) {
        this.confirmResultCallback = confirmResultCallback;
    }

    private static class MyOnShowListener implements DialogInterface.OnShowListener {
        @Override
        public void onShow(DialogInterface dialog) {
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
    }

    private static class MyOnClickListener implements DialogInterface.OnClickListener {
        public void onClick(DialogInterface dialog, int id) {
            // User cancelled the dialog
        }
    }

    private static class MyOnMultiChoiceClickListener implements DialogInterface.OnMultiChoiceClickListener {
        private final ArrayList selectedItems;
        private final CharSequence[] items;

        public MyOnMultiChoiceClickListener(ArrayList selectedItems, CharSequence[] items) {
            this.selectedItems = selectedItems;
            this.items = items;
        }

        @Override
        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
            if (isChecked) {
                selectedItems.add(items[which]);
            } else {
                selectedItems.remove(items[which]);
            }
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!selectedItems.isEmpty());
        }
    }

    private class PositiveButtonClickListener implements DialogInterface.OnClickListener {
        private final ArrayList selectedItems;

        public PositiveButtonClickListener(ArrayList selectedItems) {
            this.selectedItems = selectedItems;
        }

        public void onClick(DialogInterface dialog, int id) {
            String listString = TextUtils.join(", ", selectedItems.toArray());
            Log.d("listString", listString);
            if (ChooseBusDialogFragment.this.confirmResultCallback != null) {
                ChooseBusDialogFragment.this.busRouteList.addAll(selectedItems);
                ChooseBusDialogFragment.this.confirmResultCallback.on(selectedItems);
            }
        }
    }
}
