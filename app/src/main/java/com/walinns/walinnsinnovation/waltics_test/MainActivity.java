package com.walinns.walinnsinnovation.waltics_test;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.google.android.gms.plus.Plus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.walinns.walinnsinnovation.waltics_test.DataBase.ApiClient;
import com.walinns.walinnsinnovation.waltics_test.DataBase.Defaults;
import com.walinns.walinnsinnovation.waltics_test.DataBase.SharedCommon;
import com.facebook.BuildConfig;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.LoggingBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.walinns.walinnsapi.WalinnsAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {
    LinearLayout linear_g_plus,linear_fb;
    CallbackManager callbackManager;
    GoogleApiClient mGoogleApiClient;
    private static final int RC_SIGN_IN = 007;
    ProgressBar progress;
    SharedCommon sharedCommon;
    ApiClient apiClient;
    String base_url="https://api.backendless.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.walinns.walinnsinnovation.waltics_test.R.layout.activity_splashscreen);
        FacebookSdk.sdkInitialize(getApplicationContext());
        if (BuildConfig.DEBUG) {
            FacebookSdk.setIsDebugEnabled(true);
            FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
        }
        if(getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();
        Retrofit retrofit = new Retrofit.Builder().baseUrl(base_url+"/"+ Defaults.APPLICATION_ID+"/"+Defaults.API_KEY+"/")
                .addConverterFactory(GsonConverterFactory.create(gson)).build();
        apiClient = retrofit.create(ApiClient.class);
        WalinnsAPI.getInstance().initialize(MainActivity.this,"b9d2e92935000ffd585cc3092f9b03cd");
        linear_g_plus = (LinearLayout)findViewById(com.walinns.walinnsinnovation.waltics_test.R.id.linear_g_plus);
        linear_fb = (LinearLayout)findViewById(com.walinns.walinnsinnovation.waltics_test.R.id.linear_fb);
        linear_fb.setOnClickListener(this);
        linear_g_plus.setOnClickListener(this);
        progress = (ProgressBar)findViewById(com.walinns.walinnsinnovation.waltics_test.R.id.progress);
        sharedCommon = new SharedCommon(MainActivity.this);
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(
                callbackManager,
                new FacebookCallback < LoginResult > () {
                    @Override
                    public void onSuccess(final LoginResult loginResult) {
                        // Handle success
                        System.out.println("Facebook login :" + loginResult.getAccessToken().getToken());
                        if(loginResult.getAccessToken().getToken()!=null){
                            sharedCommon.save(SharedCommon.access_token,loginResult.getAccessToken().getToken());
                            WalinnsAPI.getInstance().track("Button","Login with Facebook");
                            GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

                                @Override
                                public void onCompleted(JSONObject object, GraphResponse response) {
                                    Log.i("LoginActivity", response.toString());
                                    // Get facebook data from login
                                    Bundle bFacebookData = getFacebookData(object);
                                    if(bFacebookData.getString("email")!=null&& bFacebookData.getString("first_name")!=null && bFacebookData.getString("last_name")!=null){
                                        sharedCommon.save(SharedCommon.email, bFacebookData.getString("email")+" "+bFacebookData.getString("last_name"));

                                        fb_login(bFacebookData.getString("email"), bFacebookData.getString("first_name"),bFacebookData.getString("last_name"));

                                    }
                                }
                            });
                            Bundle parameters = new Bundle();
                            parameters.putString("fields", "id, first_name, last_name, email,gender, birthday, location"); // Parámetros que pedimos a facebook
                            request.setParameters(parameters);
                            request.executeAsync();
                            Toast.makeText(getApplicationContext(),"Login successfully",Toast.LENGTH_SHORT).show();

                        }else {
                            WalinnsAPI.getInstance().track("Login","error while login with Fb");

                        }
                    }

                    @Override
                    public void onCancel() {
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        WalinnsAPI.getInstance().track("Login","error while login with Fb");

                    }
                }
        );
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

    }

    private void fb_login(String email, final String first_name, final String last_name) {


//        JSONObject filed = new JSONObject();
//        try {
//            filed.put("first_name",first_name);
//            filed.put("last_name",last_name);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//
//        JSONObject jsonObject = new JSONObject();
//        try {
//            jsonObject.put("accessToken",token);
//            jsonObject.put("fieldsMapping",filed);
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        Call<ResponseBody> user_register= apiClient.fb_login(jsonObject.toString());
//
//        System.out.println("Response :" + jsonObject.toString());
//
//        user_register.enqueue(new Callback<ResponseBody>() {
//            @Override
//            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                System.out.println("Response fb:" + response.isSuccessful());
//                JSONObject jsonObject;
//                try {
//                    if(response.isSuccessful()) {
//
//                        System.out.println("Response fb data :"+ response.body().string());
//
//
////                        sharedPref.save(SharedPref.user_token,"15B02DDD-9263-9D3A-FF0F-98755E634B00");
////                        Toast.makeText(getApplicationContext(),"Login Successfully",Toast.LENGTH_SHORT).show();
////                        sharedPref.save(SharedPref.logged_in,"logged");
////                        Intent intent = new Intent(MainActivity.this,HomeActivity.class);
////                        startActivity(intent);
////                        finish();
//
//
//                    }else {
//                        jsonObject = new JSONObject(response.errorBody().string());
//                        System.out.println("Response error :" + jsonObject.toString() +" mesg"+ jsonObject.getString("message"));
//                        Toast.makeText(getApplicationContext(),jsonObject.getString("message"),Toast.LENGTH_SHORT).show();
//
//                    }
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<ResponseBody> call, Throwable t) {
//                System.out.println("Response error :" + t.toString());
//
//            }
//        });
        Map<String, String> facebookFieldMappings = new HashMap<String, String>();
        facebookFieldMappings.put( "email", first_name );

        List<String> permissions = new ArrayList<String>();
        permissions.add( "email" );
        permissions.add("user_photos");
        permissions.add("user_birthday");
        permissions.add("public_profile");

        Backendless.UserService.loginWithFacebookSdk( this,
                facebookFieldMappings,
                permissions,
                callbackManager,
                new AsyncCallback<BackendlessUser>()
                {
                    @Override
                    public void handleResponse( BackendlessUser loggedInUser )
                    {
                        // user logged in successfully
                        System.out.println("facebook response :"+ loggedInUser.toString());
                        progress.setVisibility(View.GONE);
                        Intent intent = new Intent(MainActivity.this, HomeScreen.class);
                        intent.putExtra("Email",first_name+" "+last_name);
                        startActivity(intent);
                        finish();

                    }

                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        // failed to log in
                        System.out.println("facebook response :"+ fault.toString());

                    }
                } );
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case com.walinns.walinnsinnovation.waltics_test.R.id.linear_fb:
                progress.setVisibility(View.VISIBLE);
                LoginManager.getInstance().logInWithReadPermissions(
                        this,
                        Arrays.asList("user_photos", "email", "user_birthday", "public_profile")
                );
                break;
            case com.walinns.walinnsinnovation.waltics_test.R.id.linear_g_plus:
                progress.setVisibility(View.VISIBLE);
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }else {
            callbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    private void handleSignInResult(GoogleSignInResult result) {
        Log.d("Google sign in", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            WalinnsAPI.getInstance().track("Button","Login with Google");

            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();

            Log.e("Google sign in", "display name: " + acct.getDisplayName());

            String personName = acct.getDisplayName();
           // String personPhotoUrl = acct.getPhotoUrl().toString();
            String email = acct.getEmail();

            if(!personName.isEmpty()){
                sharedCommon.save(SharedCommon.email, personName);
            }

            Log.e("Google sign in", "Name: " + personName + ", email: " + email
                    + ", Image: " );
            Toast.makeText(getApplicationContext(),"Login successfully",Toast.LENGTH_SHORT).show();
            progress.setVisibility(View.GONE);
            Intent intent = new Intent(MainActivity.this, HomeScreen.class);
            intent.putExtra("Email",personName);
            startActivity(intent);
            finish();

        }else {
            System.out.println("Google login :"+ result.getStatus());
            WalinnsAPI.getInstance().track("Login","error while login with google");

        }
    }
    private Bundle getFacebookData(JSONObject object) {

        try {
            Bundle bundle = new Bundle();
            String id = object.getString("id");

            try {
                URL profile_pic = new URL("https://graph.facebook.com/" + id + "/picture?width=200&height=150");
                Log.i("profile_pic", profile_pic + "");
                bundle.putString("profile_pic", profile_pic.toString());

            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }

            bundle.putString("idFacebook", id);
            if (object.has("first_name"))
                bundle.putString("first_name", object.getString("first_name"));
            if (object.has("last_name"))
                bundle.putString("last_name", object.getString("last_name"));
            if (object.has("email"))
                bundle.putString("email", object.getString("email"));
            if (object.has("gender"))
                bundle.putString("gender", object.getString("gender"));
            if (object.has("birthday"))
                bundle.putString("birthday", object.getString("birthday"));
            if (object.has("location"))
                bundle.putString("location", object.getJSONObject("location").getString("name"));




            return bundle;
        }
        catch(JSONException e) {
            Log.d("Login db","Error parsing JSON");
        }
        return null;
    }

    @Override
    protected void onStart() {
        super.onStart();
//        if(sharedCommon.getValue(SharedCommon.email)!=null && !sharedCommon.getValue(SharedCommon.email).isEmpty()){
//            Intent intent = new Intent(MainActivity.this, HomeScreen.class);
//            intent.putExtra("Email",sharedCommon.getValue(SharedCommon.email));
//            startActivity(intent);
//            finish();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WalinnsAPI.getInstance().track("LoginActivity");
    }
}