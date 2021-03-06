/*
 * IBM Confidential OCO Source Materials
 *
 * 5725-I43 Copyright IBM Corp. 2006, 2015
 *
 * The source code for this program is not published or otherwise
 * divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 *
 */
package com.ibm.mobilefirstplatform.clientsdk.android.analytics.internal;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;

import com.ibm.mobilefirstplatform.clientsdk.android.analytics.api.Analytics;
import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;
import com.ibm.mobilefirstplatform.clientsdk.android.core.internal.BaseRequest;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.LogPersister;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.api.Logger;
import com.ibm.mobilefirstplatform.clientsdk.android.logger.internal.LogPersisterDelegate;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.UUID;

/**
 * <p>
 * MFPAnalytics provides means of persistently capturing analytics data and provides a method call to send captured data to
 * the Analytics service.
 * </p>
 * <p>
 * Capture is on by default.
 * </p>
 * <p>
 * When this MFPAnalytics class's capture flag is turned on via enable method call,
 * all analytics will be persisted to file in the following JSON object format:
 * <p>
 * <pre>
 * {
 *   "timestamp"    : "17-02-2013 13:54:27:123",  // "dd-MM-yyyy hh:mm:ss:S"
 *   "level"        : "ERROR",                    // ERROR || WARN || INFO || LOG || DEBUG
 *   "package"      : "your_tag",                 // typically a class name, app name, or JavaScript object name
 *   "msg"          : "the message",              // a helpful log message
 *   "metadata"     : {"hi": "world"},            // (optional) additional JSON metadata
 *   "threadid"     : long                        // (optional) id of the current thread
 * }
 * </pre>
 * </p>
 * <p>
 * Log data is accumulated persistently to a log file until the file size is greater than FILE_SIZE_LOG_THRESHOLD.
 * At this point the log file is rolled over. Log data will only be captured once
 * {@link com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient#initialize(Context, String, String, String)} is called.  Once both files are full, the oldest log data
 * is pushed out to make room for new log data.
 * </p>
 * <p>
 * Log file data is sent to the Bluemix server when this class's send() method is called and the accumulated log
 * size is greater than zero.  When the log data is successfully uploaded, the persisted local log data is deleted.
 * </p>
 */
public class BMSAnalytics {
    protected static final Logger logger = Logger.getLogger(LogPersister.INTERNAL_PREFIX + "analytics");

    protected static String clientApiKey = null;
    protected static String appName = null;
    protected static String savvyAppKey = null;
    public static boolean isRecordingNetworkEvents = false;

    protected static String HASHED_DEFAULT_USER_ID;

    public static final String CATEGORY = "$category";
    public static final String TIMESTAMP_KEY = "$timestamp";
    public static final String APP_SESSION_ID_KEY = "$appSessionID";
    public static final String USER_ID_KEY = "$userID";
    public static final String USER_SWITCH_CATEGORY = "userSwitch";

	public static String overrideServerHost = null;

    /**
     * Initialize MFPAnalytics API.
     * This must be called before any other MFPAnalytics.* methods
     *
     * @param app Android Application to instrument with MFPAnalytics.
     * @param applicationName Application's common name.  Should be consistent across platforms.
     * @param clientApiKey The Client API Key used to communicate with your MFPAnalytics service.
     * @param contexts One or more context attributes MFPAnalytics will register event listeners for.
     */
    static public void init(Application app, String applicationName, String clientApiKey, Analytics.DeviceEvent... contexts) {
        Context context = app.getApplicationContext();

        //Initialize LogPersister
        LogPersister.setLogLevel(Logger.getLogLevel());
        LogPersister.setContext(context);

        //Instrument Logger with LogPersisterDelegate
        LogPersisterDelegate logPersisterDelegate = new LogPersisterDelegate();
        Logger.setLogPersister(logPersisterDelegate);

        Analytics.setAnalyticsDelegate(new BMSAnalyticsDelegate());

        BMSAnalytics.clientApiKey = clientApiKey;

        if(contexts != null){
            for(Analytics.DeviceEvent event : contexts){
                switch(event){
                    case LIFECYCLE:
                        MFPActivityLifeCycleCallbackListener.init(app);
                        break;
                    case ALL:
                        MFPActivityLifeCycleCallbackListener.init(app);
                        break;
                    case NONE:
                        break;
                }
            }
        }

        //Use device ID as default user ID:
        HASHED_DEFAULT_USER_ID = getDeviceID(context);

        setUserIdentity(HASHED_DEFAULT_USER_ID);

        appName = applicationName;


        //Intercept requests to add device metadata header
        BaseRequest.registerInterceptor(new MetadataHeaderInterceptor(context.getApplicationContext()));
    }

    /**
     * Initialize Savvy API if it is being used. This must be called before interaction data can be sent
     *
     * @param appKey The key provided the the application developer from the Savvy Dashboard
     */
    public static void initSavvy(String appKey) {
        BMSAnalytics.savvyAppKey = appKey;
    }

    static protected String getDeviceID(Context context) {
        String uuid = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        return UUID.nameUUIDFromBytes(uuid.getBytes()).toString();
    }

    /**
     * Enable persistent capture of analytics data.  Enable, and thus capture, is the default.
     */
    public static void enable () {
        LogPersister.setAnalyticsCapture(true);
    }

    /**
     * Disable persistent capture of analytics data.
     */
    public static void disable () {
        LogPersister.setAnalyticsCapture(false);
    }

    /**
     * Determine if the capture of analytics events is enabled.
     * @return true if capture of analytics is enabled
     */
    public static boolean isEnabled() {
        return LogPersister.getAnalyticsCapture();
    }

    /**
     * Send the accumulated log data when the persistent log buffer exists and is not empty.  The data
     * accumulates in the log buffer from the use of {@link BMSAnalytics} with capture
     * (see {@link BMSAnalytics#enable()}) turned on.
     *
     */
    public static void send () {
        LogPersister.sendAnalytics(null);
    }

    /**
     * See {@link BMSAnalytics#send()}
     *
     * @param listener RequestListener which specifies an onSuccess callback and an onFailure callback (see {@link ResponseListener})
     */
    public static void send(ResponseListener listener) {
        LogPersister.sendAnalytics(listener);
    }

    /**
     * Send the accumulated user interaction data when the persistent log buffer exists and is not empty.
     * The data accumulates in the log buffer form the use of {@link BMSAnalytics} with capture
     * (see {@link BMSAnalytics#enable()}) turned on.
     */
    public static void sendInteractions() {
        LogPersister.sendInteractions(null);
    }

    /**
     * See {@link BMSAnalytics#sendInteractions()}
     *
     * @param responseListener RequestListener which specifies an onSuccess callback and an onFailure
     *                         callback (see {@link ResponseListener})
     */
    public static void sendInteractions(ResponseListener responseListener) {
        LogPersister.sendInteractions(responseListener);
    }

    /**
     * Log an analytics event.
     *
     * @param eventDescription An object that contains the description for the event
     */
    public static void log (final JSONObject eventDescription) {
        logger.analytics("", eventDescription);
    }

    /**
     * Log a user interaction with the application
     *
     * @param interactionDescription An object that contains the description of the interaction
     */
    public static void logInteraction(final JSONObject interactionDescription) {
        logger.interactions(interactionDescription);
    }

    /**
     * Specify current application user.  This value will be hashed to ensure privacy.
     *
     * @param user User User id for current app user.
     */
    public static void setUserIdentity(final String user) {

        // Create metadata object to log
        JSONObject metadata = new JSONObject();

        String hashedUserID = UUID.nameUUIDFromBytes(user.getBytes()).toString();

        try {
            metadata.put(CATEGORY, USER_SWITCH_CATEGORY);
            metadata.put(TIMESTAMP_KEY, (new Date()).getTime());
            metadata.put(APP_SESSION_ID_KEY, MFPAnalyticsActivityLifecycleListener.getAppSessionID());
            metadata.put(USER_ID_KEY, hashedUserID);
        }
        catch (JSONException e) {
            logger.debug("JSONException encountered logging change in user context: " + e.getMessage());
        }

        log(metadata);
    }

    /**
     * Reset user id to default value.
     * Use this when user explicitly logs out or is no longer active.
     */
    public static void clearUserIdentity() {
        setUserIdentity(HASHED_DEFAULT_USER_ID);
    }

    public static String getClientApiKey(){
        return clientApiKey;
    }

    public static String getAppName(){
        return appName;
    }

    public static String getSavvyAppKey() {
        return savvyAppKey;
    }

    /**
     * Implements the android life cycle callbacks to be registered with the application.
     *
     * Implemented as a singleton so that application callbacks can only be registered once.
     */
    private static class MFPActivityLifeCycleCallbackListener implements Application.ActivityLifecycleCallbacks {
        private static MFPActivityLifeCycleCallbackListener instance;

        public static void init(Application app) {
            if (instance == null) {
                instance = new MFPActivityLifeCycleCallbackListener();

		 app.registerActivityLifecycleCallbacks(instance);
		 MFPAnalyticsActivityLifecycleListener.getInstance().onResume();
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
            MFPAnalyticsActivityLifecycleListener.getInstance().onResume();
        }

        @Override
        public void onActivityPaused(Activity activity) {
            MFPAnalyticsActivityLifecycleListener.getInstance().onPause();
        }

        // we do not currently instrument any other lifecycle callbacks
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }

}
