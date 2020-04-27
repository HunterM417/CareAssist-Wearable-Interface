package com.srl.polardatacollection;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateOptions;
import com.mongodb.stitch.core.services.mongodb.remote.RemoteUpdateResult;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class DataLayerListenerService extends WearableListenerService {

    public static StitchAppClient client =
            Stitch.initializeDefaultAppClient("careassiststitchapp-owlqs");

    public static RemoteMongoClient mongoClient =
            client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");

    public static RemoteMongoCollection<Document> coll =
            mongoClient.getDatabase("careAssist").getCollection("locations");

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if("/MESSAGE".equals(messageEvent.getPath())) {
            String lon = MainActivity.lonTextView.getText().toString();
            String lat = MainActivity.latTextView.getText().toString();
            postLocations(lon, lat);
        }
    }

    public void postLocations(String longitude, String latitude) {
        client.getAuth().loginWithCredential(new AnonymousCredential()).continueWithTask(
                new Continuation<StitchUser, Task<RemoteUpdateResult>>() {
                    @Override
                    public Task<RemoteUpdateResult> then(@NonNull Task<StitchUser> task) throws Exception {
                        if (!task.isSuccessful()) {
                            Log.e("STITCH", "Login failed!");
                            throw task.getException();
                        }

                        Document filterDoc = new Document().append("UID", Integer.toString(MainActivity.PATIENT_ID));
                        Document updateDoc = new Document();
                        RemoteUpdateOptions upsertDoc = new RemoteUpdateOptions();
                        upsertDoc.upsert(true);

                        //insertDoc.put("patient_id", MainActivity.PATIENT_ID);
                        updateDoc.put("owner_id", task.getResult().getId());
                        updateDoc.put("UID", Integer.toString(MainActivity.PATIENT_ID));
                        updateDoc.put("longitude", longitude);
                        updateDoc.put("latitude", latitude);
                        return coll.updateOne(filterDoc, updateDoc, upsertDoc);
                    }
                }
        ).continueWithTask(new Continuation<RemoteUpdateResult, Task<List<Document>>>() {
            @Override
            public Task<List<Document>> then(@NonNull Task<RemoteUpdateResult> task) throws Exception {
                if (!task.isSuccessful()) {
                    Log.e("STITCH", "Update failed!");
                    throw task.getException();
                }
                List<Document> docs = new ArrayList<>();
                return coll
                        .find(new Document("owner_id", 0))
                        .into(docs);
            }
        }).addOnCompleteListener(new OnCompleteListener<List<Document>>() {
            @Override
            public void onComplete(@NonNull Task<List<Document>> task) {
                if (task.isSuccessful()) {
                    Log.d("STITCH", "Location push successful");
                    return;
                }
                Log.e("STITCH", "Error: " + task.getException().toString());
                task.getException().printStackTrace();
            }
        });
    }
}
