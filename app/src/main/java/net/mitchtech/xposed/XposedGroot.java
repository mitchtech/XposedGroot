
package net.mitchtech.xposed;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.io.File;
import java.util.List;
import java.util.Random;

public class XposedGroot implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static final String TAG = XposedGroot.class.getSimpleName();
    private static final String PKG_NAME = "net.mitchtech.xposed.groot";
    private static final CharSequence[] GROOT_PHRASES = {"I am Groot", "I AM Groot", "I am GROOT", "GROOT"};
    protected static final Context context = null;
    
    private XSharedPreferences prefs;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        loadPrefs();
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        
        // hook onResume for activity and play groot sounds if enabled in prefs
        if (isEnabled("prefGrootSounds")) {
            
            XC_MethodHook onResumeMethodHook = new XC_MethodHook() {
    
                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    Activity activity = (Activity) methodHookParam.thisObject;
                    // XposedBridge.log(TAG + ": " + activity.getCallingPackage());
                    playSound(activity);
                }
            };
    
            findAndHookMethod(Activity.class, "onResume", onResumeMethodHook);
        }
        
        // don't proceed further if groot text is not enabled
        if (!isEnabled("prefGrootText")) {
            return;
        }

        // don't proceed in this package, otherwise can't edit settings
        if (lpparam.packageName.equals(PKG_NAME)) {
            return;
        }        

        // don't proceed if current package is a keyguard or lock screen
        if (lpparam.packageName.contains("keyguard") || lpparam.packageName.contains("lockclock")) {
            // XposedBridge.log(TAG + "disabled. package: " + lpparam.packageName);
            return;
        }
        
        // don't proceed if current process is a keyguard or lock screen
        if (lpparam.processName.contains("keyguard") || lpparam.processName.contains("lockclock")) {
            // XposedBridge.log(TAG + "disabled. process: " + lpparam.processName);
            return;
        }
        
        // don't proceed if current package is systemui and process is a keyguard or lock screen
        if (lpparam.packageName.contains("systemui")) {
            XposedBridge.log(TAG + ": " + "systemui:" + lpparam.processName);
            if (lpparam.processName.contains("keyguard") || lpparam.packageName.contains("lockclock")) {
                // XposedBridge.log(TAG + "disabled:" + "systemui & keyguard");
                return;
            }
        }
        
//        temporarily disabled to fix lockscreen/keyguard issues    
//        // don't proceed if current package is system ui and is disabled
//        if (lpparam.packageName.contains("com.android")) {
//            if (!isEnabled("prefSystemUi")) {
//                return;
//            }
//        // don't proceed for other apps if preference is disabled
//        } else {
//            if (!isEnabled("prefAllApps")) {
//                return;
//            }
//        }

        // common method hook for textviews
        XC_MethodHook textMethodHook = new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                CharSequence actualText = (CharSequence) methodHookParam.args[0];
                // TextView textView = (TextView) methodHookParam.thisObject;
                if (actualText != null) {
                    // methodHookParam.args[0] = "I am Groot";
                    methodHookParam.args[0] = GROOT_PHRASES[new Random().nextInt(GROOT_PHRASES.length)];
                }
            }
        };

        // hook standard text views
        if (isEnabled("prefTextView")) {
            findAndHookMethod(TextView.class, "setText", CharSequence.class,
                    TextView.BufferType.class, boolean.class, int.class, textMethodHook);
            findAndHookMethod(TextView.class, "setHint", CharSequence.class, textMethodHook);
            findAndHookMethod(TextView.class, "append", CharSequence.class, textMethodHook);
            findAndHookMethod(TextView.class, "append", CharSequence.class, int.class, int.class,
                    textMethodHook);
        }

        // hook opengl text views
        if (isEnabled("prefGlText")) {
            findAndHookMethod("android.view.GLES20Canvas", null, "drawText", String.class,
                    float.class, float.class, Paint.class, textMethodHook);
        }
            
        // hook editable text views
        if (isEnabled("prefEditText")) {
            findAndHookMethod(EditText.class, "setText", CharSequence.class,
                    TextView.BufferType.class, textMethodHook);

            findAndHookConstructor(EditText.class, Context.class, new XC_MethodHook() {

                @Override
                protected void afterHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                    final EditText editText = (EditText) methodHookParam.thisObject;
                    editText.setAllCaps(true);
                    editText.setOnFocusChangeListener(new OnFocusChangeListener() {

                        @Override
                        public void onFocusChange(View view, boolean hasFocus) {
                            if (!hasFocus) {
                                editText.setAllCaps(true);
                            }
                        }
                    });
                }
            });
        }
    }

    private boolean isEnabled(String prefName) {
        prefs.reload();
        return prefs.getBoolean(prefName, false);
    }

    private void loadPrefs() {
        prefs = new XSharedPreferences(PKG_NAME);
        prefs.makeWorldReadable();
        XposedBridge.log(TAG + ": prefs loaded.");
    }

    public static void playSound(Activity activity) {
        int randomInt = new Random().nextInt(4);
        String fileName = "groot/groot" + randomInt + ".mp3";
        // XposedBridge.log(TAG + ": " + fileName);
        try {
            File mp3File = new File(Environment.getExternalStorageDirectory(), fileName);
            Uri mp3Uri = Uri.fromFile(mp3File);
            MediaPlayer mediaPlayer = MediaPlayer.create(activity, mp3Uri);
            mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });
            mediaPlayer.start();
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
    
}
