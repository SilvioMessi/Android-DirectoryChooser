package net.rdrei.android.dirchooser;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.gu.option.Option;
import com.gu.option.UnitFunction;

import java.util.ArrayList;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

/**
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SmbDirectoryChooserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SmbDirectoryChooserFragment extends DialogFragment {
    public static final String KEY_CURRENT_DIRECTORY = "CURRENT_DIRECTORY";
    private static final String ARG_CONFIG = "CONFIG";
    private static final String TAG = DirectoryChooserFragment.class.getSimpleName();
    private String mInitialDirectory;
    private String mAddress;
    private String mDomain;
    private String mUsername;
    private String mPassword;

    private Option<OnFragmentInteractionListener> mListener = Option.none();

    private Button mBtnConfirm;
    private Button mBtnCancel;
    private ImageButton mBtnNavUp;
    private ImageButton mBtnCreateFolder;
    private TextView mTxtvSelectedFolder;
    private ListView mListDirectories;

    private ArrayAdapter<String> mListDirectoriesAdapter;
    private List<String> mFilenames;
    /**
     * The directory that is currently being shown.
     */
    private SmbFile mSmbSelectedDir;
    private SmbFile[] mSmbFilesInDir;
    private SmbDirectoryChooserConfig mConfig;

    public SmbDirectoryChooserFragment() {
        // Required empty public constructor
    }

    /**
     * To create the config, make use of the provided
     * {@link SmbDirectoryChooserConfig#builder()}.
     *
     * @return A new instance of SmbDirectoryChooserFragment.
     */
    public static SmbDirectoryChooserFragment newInstance(@NonNull final SmbDirectoryChooserConfig config) {
        final SmbDirectoryChooserFragment fragment = new SmbDirectoryChooserFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_CONFIG, config);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mSmbSelectedDir != null) {
            outState.putString(KEY_CURRENT_DIRECTORY, mSmbSelectedDir.getPath());
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null) {
            throw new IllegalArgumentException(
                    "You must create SmbDirectoryChooserFragment via newInstance().");
        }
        mConfig = getArguments().getParcelable(ARG_CONFIG);

        if (mConfig == null) {
            throw new NullPointerException("No ARG_CONFIG provided for SmbDirectoryChooserFragment " +
                    "creation.");
        }

        mInitialDirectory = mConfig.initialDirectory();
        mAddress = mConfig.address();
        mDomain = mConfig.domain();
        mUsername = mConfig.username();
        mPassword = mConfig.password();

        if (savedInstanceState != null) {
            mInitialDirectory = savedInstanceState.getString(KEY_CURRENT_DIRECTORY);
        }

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        assert getActivity() != null;
        final View view = inflater.inflate(R.layout.directory_chooser, container, false);

        mBtnConfirm = (Button) view.findViewById(R.id.btnConfirm);
        mBtnCancel = (Button) view.findViewById(R.id.btnCancel);
        mBtnNavUp = (ImageButton) view.findViewById(R.id.btnNavUp);
        mBtnCreateFolder = (ImageButton) view.findViewById(R.id.btnCreateFolder);
        mTxtvSelectedFolder = (TextView) view.findViewById(R.id.txtvSelectedFolder);
        mListDirectories = (ListView) view.findViewById(R.id.directoryList);

        mBtnCreateFolder.setVisibility(View.GONE);

        mBtnConfirm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                returnSelectedFolder();
            }
        });

        mBtnCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                mListener.foreach(new UnitFunction<OnFragmentInteractionListener>() {
                    @Override
                    public void apply(final OnFragmentInteractionListener listener) {
                        listener.onCancelChooser();
                    }
                });
            }
        });

        mListDirectories.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, final View view,
                                    final int position, final long id) {
                debug("Selected index: %d", position);
                if (mSmbFilesInDir != null && position >= 0
                        && position < mSmbFilesInDir.length) {
                    ChangeDirectory task = new ChangeDirectory();
                    task.execute(mSmbFilesInDir[position].getPath());
                }
            }
        });

        mBtnNavUp.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(final View v) {
                final String parent;
                if (mSmbSelectedDir != null
                        && (parent = mSmbSelectedDir.getParent()) != null) {
                    ChangeDirectory task = new ChangeDirectory();
                    task.execute(parent);
                }
            }
        });

        adjustResourceLightness();

        mFilenames = new ArrayList<>();
        mListDirectoriesAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, mFilenames);
        mListDirectories.setAdapter(mListDirectoriesAdapter);

        ChangeDirectory task = new ChangeDirectory();
        task.execute("smb://"+ mAddress +"/" + mInitialDirectory + "/");

        return view;
    }

    private void adjustResourceLightness() {
        // change up button to light version if using dark theme
        int color = 0xFFFFFF;
        final Resources.Theme theme = getActivity().getTheme();

        if (theme != null) {
            final TypedArray backgroundAttributes = theme.obtainStyledAttributes(
                    new int[]{android.R.attr.colorBackground});

            if (backgroundAttributes != null) {
                color = backgroundAttributes.getColor(0, 0xFFFFFF);
                backgroundAttributes.recycle();
            }
        }

        // convert to greyscale and check if < 128
        if (color != 0xFFFFFF && 0.21 * Color.red(color) +
                0.72 * Color.green(color) +
                0.07 * Color.blue(color) < 128) {
            mBtnNavUp.setImageResource(R.drawable.navigation_up_light);
        }
    }

    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnFragmentInteractionListener) {
            mListener = Option.some((OnFragmentInteractionListener) activity);
        } else {
            Fragment owner = getTargetFragment();
            if (owner instanceof OnFragmentInteractionListener) {
                mListener = Option.some((OnFragmentInteractionListener) owner);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private static void debug(final String message, final Object... args) {
        Log.d(TAG, String.format(message, args));
    }

    /**
     * Returns the selected folder as a result to the activity the fragment's attached to. The
     * selected folder can also be null.
     */
    private void returnSelectedFolder() {
        if (mSmbSelectedDir != null) {
            debug("Returning %s as result", mSmbSelectedDir.getPath());
            mListener.foreach(new UnitFunction<OnFragmentInteractionListener>() {
                @Override
                public void apply(final OnFragmentInteractionListener f) {
                    f.onSelectDirectory(mSmbSelectedDir.getPath());
                }
            });
        } else {
            mListener.foreach(new UnitFunction<OnFragmentInteractionListener>() {
                @Override
                public void apply(final OnFragmentInteractionListener f) {
                    f.onCancelChooser();
                }
            });
        }

    }

    @Nullable
    public OnFragmentInteractionListener getSmbDirectoryChooserListener() {
        return mListener.get();
    }

    public void setSmbDirectoryChooserListener(@Nullable final OnFragmentInteractionListener listener) {
        mListener = Option.option(listener);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        /**
         * Triggered when the user successfully selected their destination directory.
         */
        void onSelectDirectory(@NonNull String path);

        /**
         * Advices the activity to remove the current fragment.
         */
        void onCancelChooser();
    }

    private class ChangeDirectory extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String dirStr = params[0];

            try {
                final NtlmPasswordAuthentication credentials = new NtlmPasswordAuthentication(mDomain, mUsername, mPassword);
                final SmbFile dir = new SmbFile(dirStr, credentials);
                final SmbFile[] contents = dir.listFiles();
                if (contents != null) {
                    int numDirectories = 0;
                    for (final SmbFile f : contents) {
                        if (f.isDirectory()) {
                            numDirectories++;
                        }
                    }
                    mSmbFilesInDir = new SmbFile[numDirectories];
                    mFilenames.clear();
                    for (int i = 0, counter = 0; i < numDirectories; counter++) {
                        if (contents[counter].isDirectory()) {
                            mSmbFilesInDir[i] = contents[counter];
                            mFilenames.add(contents[counter].getName());
                            i++;
                        }
                    }
                    mSmbSelectedDir = dir;
                }
            } catch (Exception e) {
                mFilenames.clear();
                mFilenames.add(getString(R.string.access_smb_folder_error));
                return "Failed";
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.equals("Executed") && mSmbSelectedDir != null){
                mTxtvSelectedFolder.setText(mSmbSelectedDir.getPath());
            }
            else {
                mListDirectoriesAdapter.notifyDataSetChanged();
            }
        }
    }
}
