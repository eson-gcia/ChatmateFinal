package ru.startandroid.develop.chatting.Fragments;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import ru.startandroid.develop.chatting.Notifications.MyResponse;
import ru.startandroid.develop.chatting.Notifications.Sender;

public interface APiService {

    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=e288e8e17b32206df1312364280edff80aa47514"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
