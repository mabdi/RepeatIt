package mrd.android.repeatit;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class FileOpenActivity extends ListActivity {

    private static final String ITEM_KEY = "key";
    private static final String ITEM_IMAGE = "image";
    public static final String ROOT_PATH = "ROOT_PATH";
    public static final String START_PATH = "START_PATH";
    public static final String FORMAT_FILTER = "FORMAT_FILTER";
    public static final String RESULT_PATH = "RESULT_PATH";
    public static final String SELECTION_MODE = "SELECTION_MODE";
    public static final String CAN_SELECT_DIR = "CAN_SELECT_DIR";
    public static final String ONLY_SELECT_DIR = "ONLY_SELECT_DIR";
    public static final String TITLE = "TITLE";

    private String root = "/";
    private List<String> path = null;
    private TextView myPath;

    private ArrayList<HashMap<String, Object>> mList;

    private Button selectButton;

    private String parentPath;
    private String currentPath = root;


    private String[] formatFilter = null;

    private boolean canSelectDir = false;
    private boolean onlySelectDir = false;

    private File selectedFile;
    private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();
    private String startPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED, getIntent());
        setContentView(R.layout.activity_file_open);
        myPath = (TextView) findViewById(R.id.path);
        selectButton = (Button) findViewById(R.id.fdButtonSelect);
        selectButton.setEnabled(false);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedFile != null) {
                    getIntent().putExtra(RESULT_PATH, selectedFile.getPath());
                    setResult(RESULT_OK, getIntent());
                    finish();
                }
            }
        });
        setTitle(getIntent().getStringExtra(TITLE));
        formatFilter = getIntent().getStringArrayExtra(FORMAT_FILTER);
        canSelectDir = getIntent().getBooleanExtra(CAN_SELECT_DIR, false);
        onlySelectDir = getIntent().getBooleanExtra(ONLY_SELECT_DIR, false);
        if(onlySelectDir)
            canSelectDir = true;
        startPath = getIntent().getStringExtra(START_PATH);
        root = getIntent().getStringExtra(ROOT_PATH);
        startPath = startPath != null ? startPath : root;
        if (canSelectDir) {
            File file = new File(startPath);
            selectedFile = file;
            selectButton.setEnabled(true);
        }
        getDir(startPath);
    }

    private void getDir(String dirPath) {
        boolean useAutoSelection = dirPath.length() < currentPath.length();
        Integer position = lastPositions.get(parentPath);
        getDirImpl(dirPath);

        if (position != null && useAutoSelection) {
            getListView().setSelection(position);
        }
    }

    private void getDirImpl(final String dirPath) {
        currentPath = dirPath;
        final List<String> item = new ArrayList<String>();
        path = new ArrayList<String>();
        mList = new ArrayList<HashMap<String, Object>>();

        File f = new File(currentPath);
        File[] files = f.listFiles();
        if (files == null) {
            currentPath = root;
            f = new File(currentPath);
            files = f.listFiles();
        }
        myPath.setText(getText(R.string.location) + ": " + currentPath);

        if (!(currentPath.equals(root)
//                || currentPath.equals(startPath)
                )) {

            //			item.add(root);
            //			addItem(root, R.drawable.ic_root_folder);
            //			path.add(root);

            item.add(getString(R.string.nup));
            addItem(getString(R.string.nup), R.mipmap.ic_folder_up);
            path.add(f.getParent());
            parentPath = f.getParent();

        }

        TreeMap<String, String> dirsMap = new TreeMap<String, String>();
        TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
        TreeMap<String, String> filesMap = new TreeMap<String, String>();
        TreeMap<String, String> filesPathMap = new TreeMap<String, String>();
        for (File file : files) {
            if(file.isHidden())
                continue;
            if (file.isDirectory()) {
                String dirName = file.getName();
                dirsMap.put(dirName, dirName);
                dirsPathMap.put(dirName, file.getPath());
            } else {
                if(!onlySelectDir){
                    final String fileName = file.getName();
                    final String fileNameLwr = fileName.toLowerCase();
                    if (formatFilter != null) {
                        boolean contains = false;
                        for (int i = 0; i < formatFilter.length; i++) {
                            final String formatLwr = formatFilter[i].toLowerCase();
                            if (fileNameLwr.endsWith(formatLwr)) {
                                contains = true;
                                break;
                            }
                        }
                        if (contains) {
                            filesMap.put(fileName, fileName);
                            filesPathMap.put(fileName, file.getPath());
                        }
                    } else {
                        filesMap.put(fileName, fileName);
                        filesPathMap.put(fileName, file.getPath());
                    }
                }
            }
        }
        item.addAll(dirsMap.tailMap("").values());
        item.addAll(filesMap.tailMap("").values());
        path.addAll(dirsPathMap.tailMap("").values());
        path.addAll(filesPathMap.tailMap("").values());
        SimpleAdapter fileList = new SimpleAdapter(this, mList, R.layout.file_dialog_row, new String[] {
                ITEM_KEY, ITEM_IMAGE }, new int[] { R.id.fdrowtext, R.id.fdrowimage });
        for (String dir : dirsMap.tailMap("").values()) {
            addItem(dir, R.mipmap.ic_folder);
        }
        for (String file : filesMap.tailMap("").values()) {
            addItem(file, R.mipmap.ic_file);
        }
        fileList.notifyDataSetChanged();
        setListAdapter(fileList);
    }

    private void addItem(String fileName, int imageId) {
        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put(ITEM_KEY, fileName);
        item.put(ITEM_IMAGE, imageId);
        mList.add(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        getListView().setItemChecked(position, true);
        File file = new File(path.get(position));

        if (file.isDirectory()) {
            selectButton.setEnabled(false);
            if (file.canRead()) {
                lastPositions.put(currentPath, position);
                getDir(path.get(position));
                if (canSelectDir) {
                    selectedFile = file;
                    v.setSelected(true);
                    selectButton.setEnabled(true);
                }
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("[" + file.getName() + "] " + getText(R.string.cant_read_folder))
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
            }
        } else {
            selectedFile = file;
            v.setSelected(true);
            selectButton.setEnabled(true);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            selectButton.setEnabled(false);

            if (!currentPath.equals(startPath)) {
                getDir(parentPath);
            } else {
                return super.onKeyDown(keyCode, event);
            }

            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

}
