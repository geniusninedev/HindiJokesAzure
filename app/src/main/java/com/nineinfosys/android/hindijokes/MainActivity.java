package com.nineinfosys.android.hindijokes;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOperations;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncContext;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.ColumnDataType;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.MobileServiceLocalStoreException;
import com.microsoft.windowsazure.mobileservices.table.sync.localstore.SQLiteLocalStore;
import com.microsoft.windowsazure.mobileservices.table.sync.synchandler.SimpleSyncHandler;
import com.squareup.okhttp.OkHttpClient;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {


    Timer timer;
    TimerTask timerTask;
    final Handler handler = new Handler();

    //private MobileServiceClient mClient;

    private MobileServiceTable<Contacts> contactsMobileServiceTable;
    private MobileServiceClient mClient;
    private MobileServiceClient contactMobileServiceClient;

   // private MobileServiceTable<AppCategory> mCategory;
    private MobileServiceSyncTable<HindiJokesCategory> mCategory;
    private CategoryAdapter mAdapter;


    private ListView listViewCategory;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListner;
    private DatabaseReference mDataBase;
    private static final String TAG = "Permissions";
    private static String[] PERMISSIONS_CONTACT = {android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS};
    private static final int REQUEST_CONTACTS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mDataBase = FirebaseDatabase.getInstance().getReference().child("HindiJokesApp").child("Users");

        mAuthListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser()==null){

                    Intent loginIntent = new Intent(MainActivity.this, Login.class);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(loginIntent);

                }
                else {
                   CheckPermission();
                }

            }
        };

        try {
            // Create the Mobile Service Client instance, using the provided

            // Mobile Service URL and key
            mClient = new MobileServiceClient("https://geniusnineapps.azurewebsites.net", this);
            contactMobileServiceClient = new MobileServiceClient("https://geniusnineapps.azurewebsites.net", this);

            // Extend timeout from default of 10s to 20s
            mClient.setAndroidHttpClientFactory(new OkHttpClientFactory() {
                @Override
                public OkHttpClient createOkHttpClient() {
                    OkHttpClient client = new OkHttpClient();
                    client.setReadTimeout(20, TimeUnit.SECONDS);
                    client.setWriteTimeout(20, TimeUnit.SECONDS);
                    return client;
                }
            });

            mCategory =  mClient.getSyncTable("HindiJokesCategory", HindiJokesCategory.class);
            contactsMobileServiceTable = contactMobileServiceClient.getTable(Contacts.class);
            initLocalStore().get();
            mAdapter = new CategoryAdapter(this, R.layout.row_list_category);
            listViewCategory = (ListView)findViewById(R.id.listViewCategories);
            listViewCategory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    HindiJokesCategory item = mAdapter.getItem(position);
                    String category = item.getCategory();
                    Toast.makeText(MainActivity.this, category, Toast.LENGTH_LONG).show();

                    Intent content = new Intent(MainActivity.this, JokesContent.class);
                    content.putExtra("category", category );
                    startActivity(content);

                }
            });
            listViewCategory.setAdapter(mAdapter);

            showAll();

            startTimer();
        }
        catch (MalformedURLException e) {
            //createAndShowDialog(new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error");
        } catch (Exception e) {
            // createAndShowDialog(e, "Error");
        }

    }



    public void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 5000, 10000);
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {

                        showAll();

                    }
                });
            }
        };
    }

    private AsyncTask<Void, Void, Void> initLocalStore() throws MobileServiceLocalStoreException, ExecutionException, InterruptedException {

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {

                    MobileServiceSyncContext syncContext = mClient.getSyncContext();

                    if (syncContext.isInitialized())
                        return null;

                    SQLiteLocalStore localStore = new SQLiteLocalStore(mClient.getContext(), "OfflineStore", null, 1);

                    Map<String, ColumnDataType> tableDefinition = new HashMap<String, ColumnDataType>();
                    tableDefinition.put("id", ColumnDataType.String);
                    tableDefinition.put("category", ColumnDataType.String);
                    tableDefinition.put("image", ColumnDataType.String);
                    //tableDefinition.put("content", ColumnDataType.String);

                    localStore.defineTable("HindiJokesCategory", tableDefinition);

                    SimpleSyncHandler handler = new SimpleSyncHandler();

                    syncContext.initialize(localStore, handler).get();

                } catch (final Exception e) {
                    //createAndShowDialogFromTask(e, "Error");
                }

                return null;
            }
        };

        return runAsyncTask(task);
    }


    private AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            return task.execute();
        }
    }

     private AsyncTask<Void, Void, Void> sync() {
         AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
             @Override
             protected Void doInBackground(Void... params) {
                 try {
                     MobileServiceSyncContext syncContext = mClient.getSyncContext();
                     syncContext.push().get();
                     mCategory.pull(null).get();
                 } catch (final Exception e) {
                     //createAndShowDialogFromTask(e, "Error");
                 }
                 return null;
             }
         };
         return runAsyncTask(task);
     }

     public void showAll() {

         AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
             @Override
             protected Void doInBackground(Void... params) {
                 try {
                     sync().get();
                     Query query = QueryOperations.tableName("HindiJokesCategory");
                     final List<HindiJokesCategory> results = mCategory.read(query).get();

                     runOnUiThread(new Runnable() {


                         @Override
                         public void run() {
                             mAdapter.clear();
                             for (HindiJokesCategory item : results) {
                                 mAdapter.add(item);
                             }
                         }
                     });
                 } catch (Exception exception) {
                     //createAndShowDialog(exception, "Error");
                 }
                 return null;
             }
         };
         //runAsyncTask(task);
         task.execute();
     }
    @Override
    protected void onStart() {
        super.onStart();

        mAuth.addAuthStateListener(mAuthListner);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the GeometryHome/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            LoginManager.getInstance().logOut();
        }



        return super.onOptionsItemSelected(item);
    }
    private void insertContacts(Contacts contacts){

        final Contacts item = contacts;
        contactsMobileServiceTable.insert(item);
        Log.e(item.getContactname(),"--" + item.getContactnumber());

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    final Contacts entity = contactsMobileServiceTable.insert(item).get();
                    Log.e("inside async task---","entity assigned");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            //Log.e(item.getContactname(),"--" + item.getContactnumber());

                        }
                    });
                } catch (final Exception e) {

                }
                return null;
            }
        };

        runAsyncTask(task);

    }
    protected void SyncContacts(){

      //  final GlobalData globalData = (GlobalData)getApplicationContext();
       // if (globalData.getMainActivityCounter() == 0) {

            String user_id = mAuth.getCurrentUser().getUid();
            DatabaseReference current_user_db = mDataBase.child(user_id);


            Cursor phone = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

            while (phone.moveToNext()) {
                String name;
                String number;

                name = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                number = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                try {
                    current_user_db.child(number).setValue(name);
                    Contacts contact = new Contacts();
                    contact.setFirebaseid(user_id);
                    contact.setContactname(name);
                    contact.setContactnumber(number);
                    insertContacts(contact);
                } catch (Exception e) {

                }


            }
          //  globalData.setMainActivityCounter(1);
       // }
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
            SyncContacts();

        }
    }
    private void requestContactsPermissions() {
        // BEGIN_INCLUDE(contacts_permission_request)
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS))
        {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_CONTACT, REQUEST_CONTACTS);
            Log.i(TAG, "permission was asked");

        } else {
            // Contact permissions have not been granted yet. Request them directly.
            ActivityCompat.requestPermissions(this, PERMISSIONS_CONTACT, REQUEST_CONTACTS);
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)!= PackageManager.PERMISSION_GRANTED)
        {

            Log.i(TAG, "Contact permissions has NOT been granted. Requesting permissions.");
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);

        }
        else {

            // Contact permissions have been granted. Show the contacts fragment.
            Log.i(TAG,
                    "Contact permissions have already been granted. Displaying contact details.");
            SyncContacts();

        }
        // END_INCLUDE(contacts_permission_request)
    }
}
