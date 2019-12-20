package com.aia;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

//import com.android.volley.RequestQueue;
//import com.android.volley.*;
//import com.android.volley.toolbox.BasicNetwork;
//import com.android.volley.toolbox.DiskBasedCache;
//import com.android.volley.toolbox.HurlStack;
//import com.android.volley.toolbox.JsonObjectRequest;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import java.lang.reflect.Array;
import java.util.HashMap;
import com.aia.R;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.gson.Gson;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthDataService;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionResult;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.FileOutputStream;
import java.io.File;


public class RNAiaHealthKitModule extends ReactContextBaseJavaModule {
    private static final String TAG = "RNAiaHealthKitModule";

    private ReactApplicationContext reactContext;
    private HealthDataStore mStore;
    private StepCountReporter mReporter;
    private Boolean isSupportHealthKit;
    private Boolean isConnectedHealthKit;
    private ReadableMap configOptions;
    private TimeZone defaultTimeZone = TimeZone.getTimeZone("Asia/Hong_Kong");
//    private RequestQueue mRequestQueue;
    private static Activity mainActivity;

    public static void setMainActiviey(Activity _mainActivity) {
        mainActivity = _mainActivity;
    }

//    public static RequestQueue getRequestQueue(ReactApplicationContext reactContext){
//        RequestQueue mRequestQueue;
//        Cache cache = new DiskBasedCache(reactContext.getCacheDir(), 1024 * 1024); // 1MB cap
//        Network network = new BasicNetwork(new HurlStack());
//        mRequestQueue = new RequestQueue(cache, network);
//        mRequestQueue.start();
//        return mRequestQueue;
//    }

    public RNAiaHealthKitModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.isSupportHealthKit = false;
//        this.mRequestQueue = getRequestQueue(reactContext);
    }

    @ReactMethod
    public void getLastSyncDateInTime(Promise promise) {
        Date data = this.getLastSyncDateTime();
        if (data != null) {
            promise.resolve("" + data.getTime());
        }else{
            promise.resolve(false);
        }
    }

    private Date getLastSyncDateTime() {
        String lastUpdate =  this.getLastSyncDate();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        TimeZone chongqing = TimeZone.getTimeZone("Asia/Chongqing");
        format.setTimeZone(chongqing);
        Date lastUpdatedate;
        if (lastUpdate != null) {
            try {
                lastUpdatedate = format.parse(lastUpdate);
                return lastUpdatedate;
            } catch (ParseException var11) {
                System.out.println(var11.getMessage());
                return null;
            }
        }
        return null;
    }

    @ReactMethod
    public void initHealthKit(String env, ReadableMap options, Promise promise) {
        //SHealth
        configOptions = options;
        HealthDataService healthDataService = new HealthDataService();
        try {
            healthDataService.initialize(reactContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initSDK(promise, null);
    }

    private void initSDK(final Promise promise, StepCountListener listener) {
        if (mStore == null) {
            // Create a HealthDataStore instance and set its listener
            mStore = new HealthDataStore(reactContext, new HealthDataStore.ConnectionListener() {

                @Override
                public void onConnected() {
                    isSupportHealthKit = true;
                    isConnectedHealthKit = true;
                    promise.resolve(true);

                }

                @Override
                public void onConnectionFailed(HealthConnectionErrorResult error) {
                    //Log.d(APP_TAG, "Health data service is not available.");
                    if (promise != null)
                        promise.resolve(genarateErrorObjectWithMessage(getErrorMessageFromResult(error)));
                }

                @Override
                public void onDisconnected() {
                    isConnectedHealthKit = false;
                    //Log.d(APP_TAG, "Health data service is disconnected.");
                    if (reactContext.getCurrentActivity().isFinishing()) {
                        mStore.connectService();
                    }
                    mStore = null;
                }
            });
            // Request the connection to the health data store
            try{
                mStore.connectService();
            }catch (Exception e){
                e.printStackTrace();
                promise.reject(e);
            }

            return;
        }

    }

    /**
     * Below is used for Samsung Health
     * This is used to schedule rendering of the component.
     */

    interface StepCountListener {
        void onStepCount(ArrayList list);
    }

    private void requestStepCount(final StepCountListener stepCountListener) {
        mReporter = new StepCountReporter(mStore, configOptions);
        StepCountReporter.StepCountObserver stepCountObserver = count -> {
            Log.d(TAG, "Step reported : " + count);
            String deviceId = Settings.Secure.getString(this.reactContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            String DEVICE_ID = deviceId + "_MYAIA";
            for (int i = 0; i< ((ArrayList<HashMap<String, HashMap<String, Object>>>)count).size(); i++) {
                HashMap<String, HashMap<String, Object>> item = count.get(i);
                HashMap<String, Object> deviceItem = item.get("device");
                deviceItem.put("deviceId", DEVICE_ID);

                // Set the
                TimeZone tz = TimeZone.getDefault();
                deviceItem.put("device_timezone", tz.getID() + " "  + tz.getDisplayName(false, TimeZone.SHORT));
            }

            if (stepCountListener != null) {
                stepCountListener.onStepCount(count);
            }
        };
        mReporter.start(stepCountObserver, getLastSyncDate());
    }


    // @ReactMethod
    // public void uploadData(String url, ReadableMap headers, ReadableMap data, final Promise promise) {
    //     Gson gson = new Gson();
    //     String json = gson.toJson(data.toHashMap());
    //     JSONObject dataJSON = new JSONObject();
    //     try {
    //         dataJSON = new JSONObject(json);
    //     } catch (JSONException e) {
    //         e.printStackTrace();
    //     }

    //     JsonObjectRequest request = new JsonObjectRequest(
    //             Request.Method.POST,
    //             url,
    //             dataJSON,
    //             new Response.Listener<JSONObject>() {
    //                 @Override
    //                 public void onResponse(JSONObject response) {
    //                     promise.resolve(Arguments.createMap());
    //                 }
    //             },
    //             new Response.ErrorListener() {
    //                 @Override
    //                 public void onErrorResponse(VolleyError error) {
    //                     if (error.networkResponse != null){
    //                         promise.reject("" + error.networkResponse.statusCode, new String(error.networkResponse.data), error);
    //                     }else{
    //                         promise.reject(error);
    //                     }
    //                 }
    //             }
    //     ){
    //         @Override
    //         public Map<String, String> getHeaders() {
    //             Map<String, String> params = new HashMap<String, String>();
    //             ReadableMapKeySetIterator keySet = headers.keySetIterator();
    //             while (keySet.hasNextKey()){
    //                 String key = keySet.nextKey();
    //                 params.put(key, headers.getString(key));
    //             }
    //             return params;
    //         }
    //     };
    //     mRequestQueue.add(request);
    // }

    @ReactMethod
    public void uploadData(String url, ReadableMap jsonMap, ReadableArray arrayHealthData, final Promise promise) {

        if (    jsonMap == null ||
                jsonMap.getString("Authorization") == null ||
                jsonMap.getString("Authorization").equals("") ||
                jsonMap.getString("Accept") == null ||
                jsonMap.getString("Accept").equals("") ||
                jsonMap.getString("Content-Type") == null ||
                jsonMap.getString("Content-Type").equals("") ||
                jsonMap.getString("X-Vitality-Legal-Entity-Id") == null ||
                jsonMap.getString("X-Vitality-Legal-Entity-Id").equals("") ||
                jsonMap.getString("X-AIA-Request-Id") == null ||
                jsonMap.getString("X-AIA-Request-Id").equals("") ||
                jsonMap.getString("Access-Control-Allow-Headers") == null ||
                jsonMap.getString("Access-Control-Allow-Headers").equals("") ||
                jsonMap.getString("Access-Control-Allow-Methods") == null ||
                jsonMap.getString("Access-Control-Allow-Methods").equals("") ||
                jsonMap.getString("Origin") == null ||
                jsonMap.getString("Origin").equals("")
                ) {
            return;
        }
        if (arrayHealthData != null) {
            new Thread() {
                public void run() {
                    int flag = 0;
                    for (int i = 0; i < arrayHealthData.size(); i++) {
                        try {
                            HashMap<String, Object> mapHealthData = (HashMap<String, Object>)arrayHealthData.toArrayList().get(i);
                            if (mapHealthData != null && mapHealthData.get("header") != null) {
                                if (((HashMap<String, String>)mapHealthData.get("header")).get("tenantId") != null) {
                                    ((HashMap<String, String>)mapHealthData.get("header")).put("tenantId", jsonMap.getString("X-Vitality-Legal-Entity-Id").toString());
                                }
                            }


                            String jsonString = new Gson().toJson(mapHealthData);
                            okhttp3.MediaType JSON = MediaType.get("application/json; charset=utf-8");
                            OkHttpClient client = new OkHttpClient();
                            RequestBody body = RequestBody.create(JSON, jsonString);
                            Request request = new Request.Builder()
                                .addHeader("Accept", jsonMap.getString("Accept"))
                                .addHeader("Content-Type", jsonMap.getString("Content-Type"))
                                .addHeader("X-Vitality-Legal-Entity-Id", jsonMap.getString("X-Vitality-Legal-Entity-Id"))
                                .addHeader("X-AIA-Request-Id", jsonMap.getString("X-AIA-Request-Id"))
                                .addHeader("Authorization", jsonMap.getString("Authorization"))
                                .addHeader("Origin",jsonMap.getString("Origin"))
                                .url(url)
                                .post(body)
                                .build();

                            Response response = client.newCall(request).execute();
                            if (response.code() == 200) {
                                System.out.println("Upload success");
                                flag++;
                            }
                            response.body().string();

                            String filename = reactContext.getExternalCacheDir().getAbsolutePath()  + File.separator + "FileName";
                            FileOutputStream outputStream = new FileOutputStream(filename, true);
                            outputStream.write(response.toString().getBytes());
                            outputStream.write("\r\n".getBytes());
                            outputStream.close();
                        } catch (Exception e) {
                            System.out.print("Exception");

                        }
                    }
                    if (flag == arrayHealthData.size()) {
                        promise.resolve(Arguments.createMap());
                    }

                }
            }.start();
        }

    }


    @ReactMethod
    public void isHealthSDKAvailable(Promise promise) {
        promise.resolve(isSupportHealthKit);
    }

    @ReactMethod
    public void readHealthDataFromDevice(Promise promise) {
        if (mStore == null) {
            initSDK(promise, new StepCountListener() {
                @Override
                public void onStepCount(ArrayList list) {
                    promise.resolve(Arguments.makeNativeArray(list));
                }
            });
            return;
        }

        mReporter = new StepCountReporter(mStore, configOptions);
        requestStepCount(new StepCountListener() {
            @Override
            public void onStepCount(ArrayList list) {
                promise.resolve(Arguments.makeNativeArray(list));
            }
        });
    }

    public String getLastSyncDate() {
        SharedPreferences sharedPreferences = reactContext.getSharedPreferences("myaia", Context.MODE_PRIVATE);
        String name = sharedPreferences.getString("lastSyncDate", null);
        return name;
    }

    private Set<PermissionKey> generatePermissionKeySet() {
        Set<PermissionKey> pmsKeySet = new HashSet<>();
        pmsKeySet.add(new PermissionKey(HealthConstants.StepCount.HEALTH_DATA_TYPE, PermissionType.READ));
        pmsKeySet.add(new PermissionKey(HealthConstants.Exercise.HEALTH_DATA_TYPE, PermissionType.READ));
        return pmsKeySet;
    }

    private boolean isPermissionAcquired() {
        HealthPermissionManager pmsManager = new HealthPermissionManager(this.mStore);

        try {
            Map<PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(this.generatePermissionKeySet());
            Boolean StepCount = false;
            Boolean Exercise = false;
            Boolean HeartRate = false;
            Iterator var6 = resultMap.entrySet().iterator();

            while(var6.hasNext()) {
                Map.Entry<PermissionKey, Boolean> entry = (Map.Entry)var6.next();
                PermissionKey key = (PermissionKey)entry.getKey();
                Boolean value = (Boolean)entry.getValue();
                if ("com.samsung.health.step_count".equals(key.getDataType())) {
                    StepCount = value;
                } else if ("com.samsung.health.exercise".equals(key.getDataType())) {
                    Exercise = value;
                }
            }

            if (!StepCount && !Exercise) {
                return false;
            } else {
                return true;
            }
        } catch (Exception var10) {
            var10.printStackTrace();
            Log.e("", "Permission request fails." + var10.getMessage());
            return false;
        }
    }

    @ReactMethod
    public void hasHealthDataPermission(final Promise promise) {
        promise.resolve(isPermissionAcquired());
    }

    @ReactMethod
    public void requestHealthPermission(final Promise promise) {

        if (mStore == null) {
            initSDK(promise, null);
            promise.resolve(false);
            return;
        }

        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            final HealthResultHolder.ResultListener<PermissionResult> mPermissionListener =
                    new HealthResultHolder.ResultListener<PermissionResult>() {

                        @Override
                        public void onResult(PermissionResult result) {
                            
                            Map<PermissionKey, Boolean> resultMap = result.getResultMap();
                            Boolean StepCount = false;
                            Boolean Exercise = false;
                            Boolean HeartRate = false;
                            Iterator var7 = resultMap.entrySet().iterator();

                            while(var7.hasNext()) {
                                Map.Entry<PermissionKey, Boolean> entry = (Map.Entry)var7.next();
                                PermissionKey key = (PermissionKey)entry.getKey();
                                Boolean value = (Boolean)entry.getValue();
                                if ("com.samsung.health.step_count".equals(key.getDataType())) {
                                    StepCount = value;
                                } else if ("com.samsung.health.exercise".equals(key.getDataType())) {
                                    Exercise = value;
                                }
                            }

                            if (!StepCount && !Exercise) {
                                promise.resolve(false);
                            } else {
                                promise.resolve(true);
                            }
                        }
                    };
            pmsManager.requestPermissions(generatePermissionKeySet(), reactContext.getCurrentActivity())
                    .setResultListener(mPermissionListener);
        } catch (Exception e) {
            if (promise != null) {
                promise.resolve(false);
            }
            Log.e(TAG, "Permission setting fails.", e);
        }
    }


    private String getErrorMessageFromResult(HealthConnectionErrorResult error) {
        String deviceMan = android.os.Build.MANUFACTURER;
        if (error.hasResolution()) {
            String errorMessage = "";
            switch (error.getErrorCode()) {
                case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:{}
                errorMessage = reactContext.getString(R.string.msg_req_install);
                break;
                case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                    errorMessage = reactContext.getString(R.string.msg_req_upgrade);
                    break;
                case HealthConnectionErrorResult.PLATFORM_DISABLED:
                    errorMessage = reactContext.getString(R.string.msg_req_enable);
                    break;
                case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                    errorMessage =  reactContext.getString(R.string.msg_req_agree);
                    break;
                case HealthConnectionErrorResult.OLD_VERSION_SDK:
                    errorMessage =  reactContext.getString(R.string.msg_old_sdk);
                    break;
                case HealthConnectionErrorResult.PLATFORM_SIGNATURE_FAILURE:
                    errorMessage =  reactContext.getString(R.string.msg_singature_fail);
                    break;
                case HealthConnectionErrorResult.TIMEOUT:
                    errorMessage =  reactContext.getString(R.string.msg_time_out);
                    break;
                default:
                    errorMessage = reactContext.getString(R.string.msg_req_available);
            }
            if (reactContext.getCurrentActivity() != null) {
//                showConnectionFailureDialog(error);
            }
            return errorMessage;
        } else {
            return reactContext.getString(R.string.msg_conn_not_available);
        }
    }

    private void showConnectionFailureDialog(final HealthConnectionErrorResult error) {

        if (mainActivity == null)
            return;
        AlertDialog.Builder alert = new AlertDialog.Builder(mainActivity);

        if (error.hasResolution()) {
            switch (error.getErrorCode()) {
                case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                    alert.setMessage(R.string.msg_req_install);
                    break;
                case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                    alert.setMessage(R.string.msg_req_upgrade);
                    break;
                case HealthConnectionErrorResult.PLATFORM_DISABLED:
                    alert.setMessage(R.string.msg_req_enable);
                    break;
                case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                    alert.setMessage(R.string.msg_req_agree);
                    break;
                default:
                    alert.setMessage(R.string.msg_req_available);
                    break;
            }
        } else {
            alert.setMessage(R.string.msg_conn_not_available);
        }

        alert.setPositiveButton(R.string.ok, (dialog, id) -> {
            if (error.hasResolution()) {
                error.resolve(mainActivity);
            }
        });

        if (error.hasResolution()) {
            alert.setNegativeButton(R.string.cancel, null);
        }

        alert.show();
    }


    @Override
    public String getName() {
        return "RNAiaHealthKit";
    }

    private WritableArray genarateWritableArrayFromList(ArrayList list) {
        for (Object obj: list) {

        }
        return null;
    }

    private WritableMap genarateErrorObjectWithMessage(String message) {
        WritableMap map =  Arguments.createMap();
        map.putString("error_message", message);
        map.putInt("error_code", 500);
        map.putBoolean("error", true);
        return map;
    }

    // SDK V_3.1 function.

}