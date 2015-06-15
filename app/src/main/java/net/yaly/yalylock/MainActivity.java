package net.yaly.yalylock;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


public class MainActivity extends Activity implements View.OnClickListener {

    private Context context = MainActivity.this;
    private Button main_getRoot, main_lock, main_createIco, main_deleteIco, main_uninstall;

    //设备策略服务
    private DevicePolicyManager policyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        createIco();
        lock();
    }

    /**
     * 界面初始化
     */
    private void initView() {
        main_uninstall = (Button) findViewById(R.id.main_uninstall);
        main_getRoot = (Button) findViewById(R.id.main_getRoot);
        main_lock = (Button) findViewById(R.id.main_lock);
        main_createIco = (Button) findViewById(R.id.main_createIco);
        main_deleteIco = (Button) findViewById(R.id.main_deleteIco);
        main_uninstall.setOnClickListener(this);
        main_getRoot.setOnClickListener(this);
        main_lock.setOnClickListener(this);
        main_createIco.setOnClickListener(this);
        main_deleteIco.setOnClickListener(this);
        policyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

    }

    /**
     * 点击方法（当前为点击图标直接锁屏，如果要显示界面，修改清单当前Activity的Theme）
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_getRoot:
                getRoot();
                break;
            case R.id.main_createIco:
                createIco();
                break;
            case R.id.main_lock:
                lock();
                break;
            case R.id.main_uninstall:
                uninstall();
                break;
            case R.id.main_deleteIco:
                deleteIco();
                break;
        }
    }

    /**
     * 用代码去开启管理员
     */
    public void getRoot() {
        // 创建一个Intent
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        // 我要激活谁
        ComponentName mDeviceAdminSample = new ComponentName(this, MyDeviceAdminReceiver.class);

        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdminSample);
        // 劝说用户开启管理员权限
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "需要锁屏权限才能锁屏（这并不会对手机有任何影响）");
        startActivity(intent);
    }

    /**
     * 一键锁屏
     */
    public void lock() {
        ComponentName who = new ComponentName(this, MyDeviceAdminReceiver.class);
        if (policyManager.isAdminActive(who)) {
            policyManager.lockNow();
            finish();
            System.exit(0);
        } else {
            getRoot();
            finish();
        }
    }


    /**
     * 卸载当前软件
     */

    public void uninstall() {

        // 1.先清除管理员权限
        ComponentName mDeviceAdminSample = new ComponentName(this, MyDeviceAdminReceiver.class);
        policyManager.removeActiveAdmin(mDeviceAdminSample);
        deleteIco();
        // 2.普通应用的卸载
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    /**
     * 创建图标
     * 加权限 <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />
     */
    public void createIco() {
        if (!hasIco(context)) {
            Intent intent = new Intent();
            intent.setClass(context, context.getClass());
            /*以下两句是为了在卸载应用的时候同时删除桌面快捷方式*/
            intent.setAction("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            Intent shortcutintent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            //不允许重复创建
            shortcutintent.putExtra("duplicate", false);
            //需要现实的名称
            shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getResources().getString(R.string.app_name));
            //快捷图片
            Parcelable icon = Intent.ShortcutIconResource.fromContext(context.getApplicationContext(), R.mipmap.ic_launcher);
            shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
            //点击快捷图片，运行的程序主入口
            shortcutintent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
            //发送广播。OK
            context.sendBroadcast(shortcutintent);
            Toast.makeText(context, "快捷方式创建成功", Toast.LENGTH_SHORT).show();
        } else {
//            Toast.makeText(context, "快捷方式已存在", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * 删除图标
     * <uses-permission android:name="com.android.launcher.permission.UNINSTALL_SHORTCUT"/>
     */
    public void deleteIco() {
        Intent shortcut = new Intent("com.android.launcher.action.UNINSTALL_SHORTCUT");

        // 获取当前应用名称
        String title = null;
        try {
            final PackageManager pm = context.getPackageManager();
            title = pm.getApplicationLabel(pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)).toString();
        } catch (Exception e) {
        }
        // 快捷方式名称
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        Intent shortcutIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        context.sendBroadcast(shortcut);

    }

    /**
     * 判断是否存在快捷图标
     * <uses-permission android:name="com.android.launcher.permission.READ_SETTINGS" />
     *
     * @param context
     * @return
     */
    public static boolean hasIco(Context context) {
        boolean result = false;
        // 获取当前应用名称
        String title = null;
        try {
            final PackageManager pm = context.getPackageManager();
            title = pm.getApplicationLabel(pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)).toString();
        } catch (Exception e) {
        }

        final String uriStr;
        if (android.os.Build.VERSION.SDK_INT < 8) {
            uriStr = "content://com.android.launcher.settings/favorites?notify=true";
        } else {
            uriStr = "content://com.android.launcher2.settings/favorites?notify=true";
        }
        final Uri CONTENT_URI = Uri.parse(uriStr);
        final Cursor c = context.getContentResolver().query(CONTENT_URI, null, "title=?", new String[]{title}, null);
        if (c != null && c.getCount() > 0) {
            result = true;
        }
        return result;
    }


}
