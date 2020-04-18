package com.srl.polardatacollection;

public class SupportData {
    public String getDatabaseName() {
        return "patients";
    }
    public String getApiKey() {
        //FIX
        return "UMDZcCfjpT_J7dmulkJcCXQC8-EncueV";
    }
    public String getBaseUrl()
    {
        //FIX
        return "https://54.144.114.219:27017"+getDatabaseName()+"/collections/";
    }
    public String apiKeyUrl()
    {
        return "?apiKey="+getApiKey();
    }
    public String collectionName()
    {
        return "patient";
    }
    public String buildEntriesSaveURL()
    {
        return getBaseUrl()+collectionName()+apiKeyUrl();
    }
    public String buildEntriesFetchURL()
    {
        return getBaseUrl()+collectionName()+apiKeyUrl();
    }
    public String createEntry(MyEntry entry) {
        return String.format("{\"id\": \"%s\", "+ "\"time\": \"%.8f\", " + "\"heartrate\": \"%.8f\", " + "\"accelerometerX\": \"%.8f\", " + "\"accelerometerY\": \"%.8f\", " + "\"accelerometerZ\": \"%.8f\"}",
                entry.get_id(), entry.get_time(), entry.get_heartrate(), entry.get_accelerometerX(), entry.get_accelerometerY(), entry.get_accelerometerZ());
    }
}
