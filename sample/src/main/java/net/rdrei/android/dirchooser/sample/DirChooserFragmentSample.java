package net.rdrei.android.dirchooser.sample;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;
import net.rdrei.android.dirchooser.SmbDirectoryChooserConfig;
import net.rdrei.android.dirchooser.SmbDirectoryChooserFragment;


public class DirChooserFragmentSample extends Activity implements DirectoryChooserFragment.OnFragmentInteractionListener,
SmbDirectoryChooserFragment.OnFragmentInteractionListener{

    private TextView mDirectoryTextView;
    private DialogFragment mDialog;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);

        mDirectoryTextView = (TextView) findViewById(R.id.textDirectory);

        findViewById(R.id.btnChoose)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                                .newDirectoryName("DialogSample")
                                .build();
                        mDialog = DirectoryChooserFragment.newInstance(config);
                        mDialog.show(getFragmentManager(), null);
                    }
                });

        findViewById(R.id.btnSmbChoose)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final SmbDirectoryChooserConfig config = SmbDirectoryChooserConfig.builder()
                                .initialDirectory("Folder name")
                                .address("192.168.1.1")
                                .username("username")
                                .password("password")
                                .domain("")
                                .build();
                        mDialog = SmbDirectoryChooserFragment.newInstance(config);

                        mDialog.show(getFragmentManager(), null);
                    }
                });
    }

    @Override
    public void onSelectDirectory(@NonNull final String path) {
        mDirectoryTextView.setText(path);
        mDialog.dismiss();
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
    }
}
