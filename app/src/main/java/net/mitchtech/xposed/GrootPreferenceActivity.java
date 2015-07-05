
package net.mitchtech.xposed;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import net.mitchtech.xposed.groot.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class GrootPreferenceActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private static final String TAG = GrootPreferenceActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.settings);
        
        final int[] mSongs = new int[] { R.raw.groot1, R.raw.groot2, R.raw.groot3 };
        for (int i = 0; i < mSongs.length; i++) {
            try {
                String path = Environment.getExternalStorageDirectory() + "/groot";
                File dir = new File(path);
                if (dir.mkdirs() || dir.isDirectory()) {
                    String str_song_name = "groot" + (i+1) + ".mp3";
                    CopyRAWtoSDCard(mSongs[i], path + File.separator + str_song_name);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_exit:
                this.finish();
                return true;
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {       
//        if (key.contentEquals("prefGrootText")) {
//            if (sharedPreferences.getBoolean(key, true)) {
//                PreferenceScreen prefTextWidgets = (PreferenceScreen) findPreference("prefTextWidgets");
//                prefTextWidgets.setEnabled(true);
//            } else {
//                PreferenceScreen prefTextWidgets = (PreferenceScreen) findPreference("prefTextWidgets");
//                prefTextWidgets.setEnabled(false);
//            }
//        }
//        Toast.makeText(this, "Reboot to activate changes", Toast.LENGTH_SHORT).show();
    }

    public static String getVersion(Context context) {
        String version = "1.0";
        try {
            PackageInfo pi = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package name not found", e);
        }
        return version;
    }
    
    private void CopyRAWtoSDCard(int id, String path) throws IOException {
        InputStream in = getResources().openRawResource(id);
        FileOutputStream out = new FileOutputStream(path);
        byte[] buff = new byte[1024];
        int read = 0;
        try {
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
        } finally {
            in.close();
            out.close();
        }
    }
   
}
