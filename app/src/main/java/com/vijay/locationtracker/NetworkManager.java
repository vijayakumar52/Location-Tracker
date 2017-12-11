package com.vijay.locationtracker;

import com.vijay.androidutils.AsyncTaskListener;
import com.vijay.androidutils.NetworkRequest;

import java.util.HashMap;


public class NetworkManager {
    private static NetworkManager networkManager = new NetworkManager();
    public static NetworkManager getInstance(){
        return networkManager;
    }
    public void addRecord(String time, String latitude, String longitude,AsyncTaskListener listener){
        String url = "https://creator.zoho.com/api/vijayakumar12/json/vehicle-tracker/form/Vehicle_Trace/record/add/";
        String authToken = "6185ed36adc4f3fa03f66305cc720789";
        String scope = "creatorapi";

        HashMap<String,String> postParams = new HashMap<>();
        postParams.put("authtoken",authToken);
        postParams.put("scope",scope);
        postParams.put("time",time);
        postParams.put("latitude",latitude);
        postParams.put("longitude",longitude);

        new NetworkRequest(url,postParams,listener).execute();

    }
    public void deleteRecord(AsyncTaskListener listener){
        String url = "https://creator.zoho.com/api/vijayakumar12/json/vehicle-tracker/form/Vehicle_Trace/record/delete/";
        String authToken = "6185ed36adc4f3fa03f66305cc720789";
        String scope = "creatorapi";

        HashMap<String,String> postParams = new HashMap<>();
        postParams.put("authtoken",authToken);
        postParams.put("scope",scope);
        postParams.put("criteria","time != 0");

        new NetworkRequest(url,postParams,listener).execute();
    }

}
