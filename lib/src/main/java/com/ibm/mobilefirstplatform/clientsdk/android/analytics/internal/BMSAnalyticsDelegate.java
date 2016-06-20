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

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.ResponseListener;

import org.json.JSONObject;

public class BMSAnalyticsDelegate implements AnalyticsDelegate{
    @Override
    public void enable() {
        BMSAnalytics.enable();
    }

    @Override
    public void disable() {
        BMSAnalytics.disable();
    }

    @Override
    public boolean isEnabled() {
        return BMSAnalytics.isEnabled();
    }

    @Override
    public void send() {
        BMSAnalytics.send();
    }

    @Override
    public void send(Object responseListener) {
        BMSAnalytics.send((ResponseListener)responseListener);
    }

    @Override
    public void log(JSONObject eventMetadata) {
        BMSAnalytics.log(eventMetadata);
    }

    @Override
    public void logInteraction(JSONObject eventMetadata) {
        BMSAnalytics.logInteraction(eventMetadata);
    }

    @Override
    public void setUserIdentity(String username) {
        BMSAnalytics.setUserIdentity(username);
    }

    @Override
    public void clearUserIdentity() {
        BMSAnalytics.clearUserIdentity();
    }

    @Override
    public String getClientAPIKey() {
        return BMSAnalytics.getClientApiKey();
    }

    @Override
    public String getAppName() {
        return BMSAnalytics.getAppName();
    }
}
