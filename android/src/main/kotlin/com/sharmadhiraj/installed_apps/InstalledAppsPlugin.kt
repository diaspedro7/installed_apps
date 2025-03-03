package com.sharmadhiraj.installed_apps;

import android.content.Context;
import android.content.Intent;
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
import android.widget.Toast;
import android.widget.Toast.LENGTH_LONG;
import android.widget.Toast.LENGTH_SHORT;
import com.sharmadhiraj.installed_apps.Util.Companion.convertAppToMap;
import com.sharmadhiraj.installed_apps.Util.Companion.getPackageManager;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import java.util.List;
import java.util.Locale.ENGLISH;
import java.util.stream.Collectors;

public class InstalledAppsPlugin implements MethodCallHandler, FlutterPlugin, ActivityAware {

    private MethodChannel channel;
    private Context context;

    @Override
    public void onAttachedToEngine(FlutterPlugin.FlutterPluginBinding binding) {
        context = binding.getApplicationContext();
        channel = new MethodChannel(binding.getBinaryMessenger(), "installed_apps");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(FlutterPlugin.FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding activityPluginBinding) {
        context = activityPluginBinding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {}

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding activityPluginBinding) {
        context = activityPluginBinding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {}

    @Override
    public void onMethodCall(MethodCall call, MethodChannel.Result result) {
        if (context == null) {
            result.error("ERROR", "Context is null", null);
            return;
        }
        switch (call.method) {
            case "getInstalledApps":
                boolean excludeSystemApps = call.argument("exclude_system_apps") != null ? call.argument("exclude_system_apps") : true;
                boolean withIcon = call.argument("with_icon") != null ? call.argument("with_icon") : false;
                String packageNamePrefix = call.argument("package_name_prefix") != null ? call.argument("package_name_prefix") : "";
                String platformTypeName = call.argument("platform_type") != null ? call.argument("platform_type") : "";
                
                new Thread(() -> {
                    List<Map<String, Object>> apps = getInstalledApps(excludeSystemApps, withIcon, packageNamePrefix, PlatformType.fromString(platformTypeName));
                    result.success(apps);
                }).start();
                break;

            default:
                result.notImplemented();
        }
    }

    private List<Map<String, Object>> getInstalledApps(boolean excludeSystemApps, boolean withIcon, String packageNamePrefix, PlatformType platformType) {
        PackageManager packageManager = getPackageManager(context);
        List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(0);
        
        if (excludeSystemApps) {
            installedApps = installedApps.stream().filter(app -> !isSystemApp(packageManager, app.packageName)).collect(Collectors.toList());
        }
        
        if (!packageNamePrefix.isEmpty()) {
            installedApps = installedApps.stream().filter(app -> app.packageName.startsWith(packageNamePrefix.toLowerCase(ENGLISH))).collect(Collectors.toList());
        }
        
        installedApps = installedApps.stream().filter(app -> isLaunchable(packageManager, app.packageName)).collect(Collectors.toList());
        
        return installedApps.stream().map(app -> convertAppToMap(packageManager, app, withIcon, platformType)).collect(Collectors.toList());
    }

    private boolean isSystemApp(PackageManager packageManager, String packageName) {
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            return (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isLaunchable(PackageManager packageManager, String packageName) {
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        return intent != null;
    }
}
