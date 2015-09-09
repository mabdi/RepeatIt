package mrd.android.repeatit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    private TextView mFilename;
    private ImageButton mNext;
    private ImageButton mPrev;
    private CheckBox mListen;
    private CheckBox mRecord;
    private CheckBox mRepeat;
    private String openedFile = null;
    private String RECORD_FILE = null;

    private static final int PICK_FILE_RESULT_CODE = 311;

    private MediaPlayer mPlayerListen;
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayerRepeat;
    private SharedPreferences mSharedPreferences;


    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFilename = (TextView)view.findViewById(R.id.filename);
        mNext = (ImageButton)view.findViewById(R.id.next);
        mPrev = (ImageButton)view.findViewById(R.id.prev);
        mListen = (CheckBox)view.findViewById(R.id.listen);
        mRecord = (CheckBox)view.findViewById(R.id.record);
        mRepeat = (CheckBox)view.findViewById(R.id.repeat);
        mListen.setOnCheckedChangeListener(this);
        mRecord.setOnCheckedChangeListener(this);
        mRepeat.setOnCheckedChangeListener(this);
        mNext.setOnClickListener(this);
        mPrev.setOnClickListener(this);
        mFilename.setOnClickListener(this);
        File folder = new File(Environment.getExternalStorageDirectory() + "/repeatit");
        if (!folder.exists()) {
            folder.mkdir();
        }
        RECORD_FILE = Environment.getExternalStorageDirectory() + "/repeatit/last_recorded.pcm";
        mSharedPreferences = getActivity().getSharedPreferences(
                "repeatit.db", Context.MODE_PRIVATE);
        getSelectedFile();
    }

    private void openFile() {
        Intent intent = new Intent(getActivity(), FileOpenActivity.class);
        intent.putExtra(FileOpenActivity.START_PATH, Environment.getExternalStorageDirectory().
                getParentFile().getAbsolutePath());
        intent.putExtra(FileOpenActivity.FORMAT_FILTER, new String[]{"mp3", "m4a"});
        intent.putExtra(FileOpenActivity.ONLY_SELECT_DIR, false);
        intent.putExtra(FileOpenActivity.TITLE, getString(R.string.folder_selector_title));
        startActivityForResult(intent, PICK_FILE_RESULT_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode) {
            case PICK_FILE_RESULT_CODE: {
                if (resultCode== Activity.RESULT_OK && data!=null) {
                    String filePath = data.getStringExtra(FileOpenActivity.RESULT_PATH);
                    if(filePath!= null){
                        setSelectedFile(filePath);
                    }
                }
                break;
            }
        }
    }

    private String getSelectedFile(){
        if(openedFile==null){
            openedFile = mSharedPreferences.getString("file",null);
            if(openedFile == null){
                mFilename.setText("Tap to select file");
            }else{
                mFilename.setText(new File(openedFile).getName());
            }
        }
        return openedFile;
    }

    private void setSelectedFile(String file){
        mSharedPreferences.edit().putString("file",file).commit();
        openedFile = null;
        getSelectedFile();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == mNext.getId()){
            loadNextMusic();
        }else if(v.getId() == mPrev.getId()){
            loadPrevMusic();
        }else if (v.getId() == mFilename.getId()){
            openFile();
        }
    }

    private void loadPrevMusic() {
        File file = new File(getSelectedFile());
        File[] files = file.getParentFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".mp3")||
                        pathname.getName().endsWith("m4a");
            }
        });
        for (int i = 0; i < files.length; i++) {
            if(files[i].getName().equals(file.getName())){
                if(i>0){
                    setSelectedFile(files[i-1].getAbsolutePath());
                }else{
                    Toast.makeText(getActivity(),"No Prev File",Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    private void loadNextMusic() {
        File file = new File(getSelectedFile());
        File[] files = file.getParentFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".mp3")||
                        pathname.getName().endsWith("m4a");
            }
        });
        for (int i = 0; i < files.length; i++) {
            if(files[i].getName().equals(file.getName())){
                if(i < files.length-1){
                    setSelectedFile(files[i+1].getAbsolutePath());
                }else{
                    Toast.makeText(getActivity(),"No Next File",Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView.getId() == mListen.getId()) {
            if (isChecked) {
                doListen();
                doStopRecord();
                doStopRepeat();
            }else{
                doStopListen();
            }
        } else if (buttonView.getId() == mRecord.getId()) {
            if (isChecked) {
                doRecord();
                doStopListen();
                doStopRepeat();
            }else{
                doStopRecord();
            }
        } else if (buttonView.getId() == mRepeat.getId()) {
            if (isChecked) {
                doRepeat();
                doStopListen();
                doStopRecord();
            }else{
                doStopRepeat();
            }
        }
    }

    private void doListen() {
        mPlayerListen = new MediaPlayer();
        try {
            mPlayerListen.setDataSource(getSelectedFile());
            mPlayerListen.prepare();
            mPlayerListen.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mListen.setChecked(false);
                }
            });
            mPlayerListen.start();
        } catch (IOException e) {
            Log.e("mrd", "prepare() failed");
        }
    }

    private void doStopListen(){
        if(mPlayerListen!= null) {
            mPlayerListen.release();
            mPlayerListen = null;
        }
    }

    private void doRecord() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(RECORD_FILE);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e("mrd", "prepare() failed");
        }

        mRecorder.start();
    }

    private void doStopRecord() {
        if(mRecorder!= null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    private void doRepeat() {
        mPlayerRepeat = new MediaPlayer();
        try {
            mPlayerRepeat.setDataSource(RECORD_FILE);
            mPlayerRepeat.prepare();
            mPlayerRepeat.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mRepeat.setChecked(false);
                }
            });
            mPlayerRepeat.start();
        } catch (IOException e) {
            Log.e("mrd", "prepare() failed");
        }
    }

    private void doStopRepeat() {
        if(mPlayerRepeat!= null) {
            mPlayerRepeat.release();
            mPlayerRepeat = null;
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayerListen != null) {
            mPlayerListen.release();
            mPlayerListen = null;
        }

        if (mPlayerRepeat != null) {
            mPlayerRepeat.release();
            mPlayerRepeat = null;
        }
    }
}