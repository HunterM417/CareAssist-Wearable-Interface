package com.srl.polardatacollection;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class WearDataLayerListenerService extends WearableListenerService {

    public static final String START_ACTIVITY_PATH = "/start/MainActivity";
    public static final String STOP_ACTIVITY_PATH = "/stop/MainActivity";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if(messageEvent.getPath().contains(START_ACTIVITY_PATH)){

            String filename = messageEvent.getPath().split("/")[3];
            String activity = messageEvent.getPath().split("/")[4];

            String[] newActivity = new String[]{filename, activity};

            if (filename.equals("")) {
                Intent newActivityIntent = new Intent("new_activity");
                newActivityIntent.putExtra("NEW_ACTIVITY", newActivity);
                sendBroadcast(newActivityIntent);
            } else {
                Intent intent = new Intent(this , MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("NEW_ACTIVITY", newActivity);
                startActivity(intent);
            }

        } else if(messageEvent.getPath().equals(STOP_ACTIVITY_PATH)) {
            Intent stopIntent = new Intent("finish_activity");
            sendBroadcast(stopIntent);
        }

    }
}