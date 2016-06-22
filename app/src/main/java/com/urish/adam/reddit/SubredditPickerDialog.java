package com.urish.adam.reddit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

/**
 * Created by student on 6/21/2016.
 */
public  class SubredditPickerDialog extends DialogFragment {
    EditText editText;
    public interface DialogListener{
        public void onPosClick(DialogFragment dialogFragment);
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        builder.setView(layoutInflater.inflate(R.layout.subreddit_picker_dialog,null))
                .setPositiveButton("Enter", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        listener.onPosClick(SubredditPickerDialog.this);
                    }
                });
        return builder.create();
    }
    DialogListener listener;
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            listener = (DialogListener) getActivity();
        }
        catch (ClassCastException e)
        {
            e.printStackTrace();
        }
    }
}