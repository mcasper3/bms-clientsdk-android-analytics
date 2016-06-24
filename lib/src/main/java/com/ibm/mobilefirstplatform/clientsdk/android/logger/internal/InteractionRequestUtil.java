package com.ibm.mobilefirstplatform.clientsdk.android.logger.internal;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;

import com.ibm.mobilefirstplatform.clientsdk.android.security.identity.BaseDeviceIdentity;

import org.json.JSONException;
import org.json.JSONObject;

public class InteractionRequestUtil {

    private static final String TAG = "InteractionRequestUtil";

    private InteractionRequestUtil() {

    }

    public static JSONObject getInteractionRequestPayload(JSONObject payload, final Context context) {
        BaseDeviceIdentity deviceIdentity = new BaseDeviceIdentity(context);

        try {
            payload.put("deviceID", deviceIdentity.getId());
            payload.put("deviceModel", deviceIdentity.getModel());
            payload.put("deviceBrand", deviceIdentity.getBrand());
            payload.put("deviceOS", deviceIdentity.getOS());
            payload.put("deviceOSVersion", deviceIdentity.getOSVersion());

            PackageManager packageManager = context.getPackageManager();
            PackageInfo info = packageManager.getPackageInfo(context.getPackageName(), 0);
            payload.put("appVersion", info.versionName);
            payload.put("appVersionCode", Integer.toString(info.versionCode));
            payload.put("appID", context.getPackageName());

            Resources resources = context.getResources();

            DisplayMetrics metrics = resources.getDisplayMetrics();

            int width = metrics.widthPixels;
            int height = metrics.heightPixels;

            JSONObject deviceResolution = new JSONObject();

            int currentOrientation = resources.getConfiguration().orientation;

            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                deviceResolution.put("width", width);
                deviceResolution.put("height", height);
            } else {
                deviceResolution.put("width", height);
                deviceResolution.put("height", width);
            }

            String screenDensity = getScreenDensity(metrics);

            payload.put("screenResolution", deviceResolution);
            payload.put("screenDensity", screenDensity);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to get device info");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get package info");
        }
        return payload;
    }

    private static String getScreenDensity(DisplayMetrics metrics) {
        String density;
        int densityDpi = metrics.densityDpi;

        switch (densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                density = "ldpi";
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                density = "mdpi";
                break;
            case DisplayMetrics.DENSITY_HIGH:
                density = "hdpi";
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                density = "xhdpi";
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                density = "xxhdpi";
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                density = "xxxhdpi";
                break;
            default:
                density = "unknown";
        }

        return density;
    }
}
