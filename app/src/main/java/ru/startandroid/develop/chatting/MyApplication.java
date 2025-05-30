package ru.startandroid.develop.chatting;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dhkm0ntgu");
        config.put("api_key", "743559914413969");
        config.put("api_secret", "RvMHimqIPksEbvpX5rg0FFegQHU");
        config.put("secure", "true");

        MediaManager.init(this, config);
    }
}


