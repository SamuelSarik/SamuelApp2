package sk.sarik.samuel.samuelapp2;

import android.os.Environment;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileHelper {

    private final static String TAG = FileHelper.class.getName();
    private final static String DIRECTORY = "/Pictures_Text";

    public static String readExternalStorage() {
        File root = Environment.getExternalStorageDirectory();
        File Dir = new File(root.getAbsolutePath() + DIRECTORY);
        File file = lastFileModified(Dir.toString());
        String datafromfile;
        StringBuffer stringBuffer = new StringBuffer();

        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            while ((datafromfile = bufferedReader.readLine()) != null) {
                stringBuffer.append(datafromfile + "\n");
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }

        return stringBuffer.toString();
    }

    public static File lastFileModified(String dir) {
        File fl = new File(dir);
        File[] files = fl.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        return choice;
    }

}
