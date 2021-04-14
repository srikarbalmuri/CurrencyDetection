package com.website.currencydetectandvoicecommandusd;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.io.CopyStreamAdapter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

public class CallActivity extends AppCompatActivity implements RecognitionListener, TextToSpeech.OnInitListener {

    private static final String SPEAK_NOW = "Speak now";
    private static final CharSequence CALL_CMD = "call";
    public static final String IMAGE_CAPTURE = "ImageCaptured";
    private static final int RC_CALL = 4;
    private static final String COUNTRY_CURRENCY_LABEL = "Dollar";
    private static String utteranceId;
    private String contactNameSpeech, contactPhone;
    private SpeechRecognizer speechRecog;   //stt
    private Intent intentRecog;
    private TextToSpeech tts;
    private ImageView micTapImageView;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;
    public static final String FILE_NAME = "temp.jpg";
    private static final String TAG = "TAG";
    private Bitmap resultBitmap;
    private ImageView imageSelected;
    private Dialog dialogLoader;
    private RelativeLayout llayout;
    private TextView bestGuessLabelTV;
    private boolean noInputNeeded;
    private int tts_id = 0, firstCmd = 0;
    private boolean isTTSready;
    private Runnable runnable;
    private String url;
    private boolean flowOnceStarted;
    private boolean backFromCallActivity;
    private static final int MAX_DIMENSION = 1200;
    private Bitmap origBitmap;
    private String imageURL;

    private ProgressDialog pd;
    private FTPClient mFTPClient;
    private static final int RC_PERM = 107;
    private static final int RC_FILE_GET = 10;
    private CopyStreamAdapter streamListener;
    private String fPath;
    private String extension;
    private String destUploadedPath;
    private String getImageUrl;
    private static final String MY_KEY = "9d393810c19a4865b2ee2d513fdc9152";
    private static final String UPLOADPATH = "AOrganDon/currencyd/";
    final String API_URL =
            "https://knowmycurrency.cognitiveservices.azure.com/customvision/v3.0/Prediction/62d9926e-ef8b-4763-9891-8aab7b6cd9b9/classify/iterations/Iteration4/url";
    final String API_URL_IMG =
            "https://knowmycurrency.cognitiveservices.azure.com/customvision/v3.0/Prediction/62d9926e-ef8b-4763-9891-8aab7b6cd9b9/classify/iterations/Iteration4/image";
    private boolean enableScanCurrencyBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        Toolbar toolbar = findViewById(R.id.toolbar);
        //toolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material);
        setSupportActionBar(toolbar);
        getSupportActionBar().setBackgroundDrawable(null);
        getSupportActionBar().setTitle("Know Your Currency");
        init();
    }

    //checks for the internet if available else will show the dialog saying "looks like you're offline..


    // internet check

    private void internetCheck() {
        if (!isConnectedToInternet()) {
            speakOut("Looks like you're offline, Connect to the internet!", true);
            AlertDialog.Builder builder = new AlertDialog.Builder(CallActivity.this);
            builder.setTitle("Looks like you're offline");
            builder.setCancelable(false);
            builder.setMessage("No internet connection");
            builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.show();
        } else {
            isTTSready = true;
            //Log.i(TAG, "internetCheck: isTTSready " + isTTSready);
            if (PermissionUtils.requestPermission(CallActivity.this, 0, Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA)
                    && tts_id == 0) {
                speakOut("Do you want to Detect currency or make a call? Say 1 to make a call, say 2 to detect currency.", false);
                flowOnceStarted = true;
                firstCmd = 0;
            }
        }
    }


    // returns true if connected to internet
    private boolean isConnectedToInternet() {
        ConnectivityManager mgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return mgr.getActiveNetworkInfo() != null && mgr.getActiveNetworkInfo().isConnected();
    }

    //AsyncTask matching spoken contactname from your address book in device
    private void ContactMatchTask(String s) {
        ContactsTask task = new ContactsTask();
        task.execute(s);
    }

    //initializing the android UI widgets and texttospeech
    private void init() {
        micTapImageView = findViewById(R.id.iv_mic);
        tts = new TextToSpeech(this, this);
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {

            @Override
            public void onStart(String utteranceId) {
                //Log.i(TAG, "onStart utteranceId: " + utteranceId);
            }

            @Override
            public void onDone(String utteranceId) {
                //pause or some indication to inputnext
                if (!noInputNeeded) {
                    CallActivity.utteranceId = utteranceId;
                    runOnUiThread(runnable);

                    //Log.i(TAG, "onDone: utteranceId " + utteranceId);
                }
            }

            @Override
            public void onError(String utteranceId) {
                //Log.i(TAG, "onError: utteranceId" + utteranceId);
            }
        });
        runnable = () -> onMicTapped(utteranceId);
        imageSelected = findViewById(R.id.iv_currency_display);
        llayout = findViewById(R.id.ll);
        bestGuessLabelTV = findViewById(R.id.tv_bestguesslabel);
    }

    //speaks the String s
    private void speakOut(String s, boolean noInputNeeded) {
        if (tts.isSpeaking())
            tts.stop();
        tts_id++;
        this.noInputNeeded = noInputNeeded;
        HashMap<String, String> mapTTSid = new HashMap<>();
        mapTTSid.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(tts_id));
        //Log.i(TAG, "speakOut: isSpeaking " + tts.isSpeaking());
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, mapTTSid);
    }

    /*@Override
    protected void onStop() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecog != null) {
            speechRecog.destroy();
        }
        super.onStop();
    }
*/
    //when mic image is tapped start speech recognizing
    public void onMicTapped(View view) {
        //recognise speech
        //if (SpeechRecognizer.isRecognitionAvailable(this)) {
        //Log.i(TAG, "onMicTapped: ");
        if (speechRecog == null) {
            if (SpeechRecognizer.isRecognitionAvailable(CallActivity.this)) {
                Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show();
                return;
            }   //TEST
            speechRecog = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecog.setRecognitionListener(this);
            intentRecog = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intentRecog.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intentRecog.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
            intentRecog.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        }
        if (!PermissionUtils.requestPermission(CallActivity.this, 0, Manifest.permission.RECORD_AUDIO))
            return;
        if (!tts.isSpeaking()) {
            speechRecog.stopListening();
            speechRecog.startListening(intentRecog);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecog != null) {
            speechRecog.destroy();
        }
        super.onDestroy();
    }

    //method of ContactMatch Asyctask
    private String matchContact(String contactNameSpeech) {
        Cursor cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null,
                null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String hasPhone, contactName, contactId;
            while (!cursor.isAfterLast()) {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                //matches?
                //Log.i(TAG, "matchContact: " + contactName + contactNameSpeech);
                if (contactNameSpeech.equalsIgnoreCase(contactName)) {
                    hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                    //Log.i(TAG, "matchContact: " + hasPhone);
                    // if (Integer.parseInt(hasPhone) > 0) {
                    //Log.d(TAG, "matchContact: hasPhone" + hasPhone);
                    Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
                    while (phones.moveToNext()) {
                        String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        phones.close();
                        //Log.i(TAG, "matchContact matched: " + phoneNumber);
                        return phoneNumber;
                   /*     }
                    } else if (Integer.parseInt(hasPhone) == 0) {
                        return null;*/
                    }
                }
                cursor.moveToNext();
            }
            cursor.close();
        }
        return null;
    }

    @Override
    public void onInit(int status) {
        //takes 3s
        if (status == TextToSpeech.SUCCESS) {
            int resultLang = tts.setLanguage(Locale.getDefault());
            if (resultLang == TextToSpeech.LANG_MISSING_DATA ||
                    resultLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                // missing data, install it
                Intent install = new Intent();
                install.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(install);
            } else {
                internetCheck();
            }
        }
        //Log.d(TAG, "onInit() called with: status = [" + status + "]");
    }

    //when upload image is clicked the ImageAnalysisTask is executed for Microsoftcognitive custom vision api service
   /* public void uploadImage() {
        //ftp upload and image renamed as temp.jpg or jpg or what format, keep the same format for upload but name
        uploadFTP();
    }*/

    public void uploadImage(View view) {
        if (!enableScanCurrencyBtn){
            Toast.makeText(this, "Please click to select image", Toast.LENGTH_SHORT).show();
            return;
        }
       /* if (resultBitmap != null)
            uploadImage();*/
        if (origBitmap != null && origBitmap.getByteCount() > 4_000000) {
            //resize and send image
            //Log.i(TAG, "onActivityResult: >4mb");
            new ImageAnalysisTaskImage().execute();
        } else {
            //upload from url
          /*  uploadFTP();
            imageSelected.setImageBitmap(resultBitmap);*/
          //server down [[//TEST
            //Log.i(TAG, "onActivityResult: <4mb");
            new ImageAnalysisTaskImage().execute();
        }
    }

    //AsyncTask for matching contact spoken
    private class ContactsTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            return matchContact(strings[0]);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String phoneNum) {
            super.onPostExecute(phoneNum);
            //make a call
            //Log.i(TAG, "onPostExecute: " + contactNameSpeech + phoneNum);
            if (phoneNum != null) {
                speakOut("calling " + contactNameSpeech, true);
                Intent intentCall = new Intent(Intent.ACTION_CALL);
                intentCall.setData(Uri.parse("tel:" + phoneNum));
                if (ActivityCompat.checkSelfPermission(CallActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivityForResult(intentCall, RC_CALL);
            } else {
                speakOut("CONTACT " + contactNameSpeech + " not found ,\n " + askAgainToCallString(), false);
                //Log.i(TAG, "onPostExecute: " + contactNameSpeech);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        permission();
        if (backFromCallActivity) {
            //Log.d(TAG, "onResume() called" + isTTSready);
            speakOut("\n " + askAgainToCallString(), false);
            backFromCallActivity = false;
        }
    }

    //dynamic permissions are asked checked and asked from user to grant them
    private void permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                //Log.d(TAG, "permission() called inside");
                PermissionUtils.requestPermission(CallActivity.this, 0, Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA);
            } else {
                //Log.d(TAG, "permission() called in else");
            }
        } else {
            //Log.d(TAG, "permission() called ");
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        //play a sound
        //Log.d(TAG, "onReadyForSpeech() called with: params = [" + params + "]");
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_in_out);
        //animation.setRepeatCount(3);
        micTapImageView.setAnimation(animation);
    }

    @Override
    public void onBeginningOfSpeech() {
        //Log.d(TAG, "onBeginningOfSpeech() called");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        //Log.d(TAG, "onRmsChanged() called with: rmsdB = [" + rmsdB + "]");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        //Log.d(TAG, "onBufferReceived() called with: buffer = [" + buffer + "]");
    }

    //restart
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.restart) {
            restart();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEndOfSpeech() {
        //Log.d(TAG, "onEndOfSpeech() called");
    }

    @Override
    protected void onPause() {
        //Log.i(TAG, "onPause: before" + firstCmd + flowOnceStarted);
        super.onPause();
        /*if (!flowOnceStarted) {
            speakOut("Do you want to Detect currency or make a call? Say 1 to make a call, say 2 to detect currency.", false);
            firstCmd = 0;
        }*/
        //Log.i(TAG, "onPause: after" + firstCmd);
    }

    @Override
    public void onError(int error) {
        //Log.d(TAG, "onError() called with: error = [" + error + "]");
    }

    @Override
    public void onResults(Bundle results) {
        //play a sound
        //isSpeechRecognized = 0;
        String command = Objects.requireNonNull(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)).get(0);
        ArrayList<String> cmdList = new ArrayList<>();
        cmdList = Objects.requireNonNull(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
        //Log.d(TAG, "onResults: firstcmd = " + firstCmd + "command : " + command + ", " + Arrays.asList(cmdList));
        if (firstCmd == 0) {
            for (int i = 0; i < cmdList.size(); i++) {
                String cmd = cmdList.get(i);
                if (cmd.equalsIgnoreCase("1") || cmd.equalsIgnoreCase("one") || cmd.equalsIgnoreCase("CALL")) {
                    firstCmd = 1;
                    //Log.d(TAG, "onResults: To make a call to your contact, say ");
                    speakOut("To make a call to your contact, say \"CALL CONTACT NAME\"", false);
                    break;
                } else if (cmd.equalsIgnoreCase("2") || cmd.equalsIgnoreCase("two")|| cmd.equalsIgnoreCase("to") || cmd.equalsIgnoreCase("too") || cmd.equalsIgnoreCase("DETECT")) {
                    firstCmd = 1;
                    speakOut("Starting camera.. Picture will be taken in next 3 seconds", true);
                    //Log.d(TAG, "onResults: Please Choose an Image to Analyse");
                    startCamera();
                    break;
                } else if (cmd.equalsIgnoreCase("EXIT")) {
                    firstCmd = 1;
                    finish();
                    break;
                }
            }
            if (firstCmd != 1) {
                speakOut("Didn't catch that, please try again", false);
            }
        } else if (command.contains(CALL_CMD)) {
            //Log.i(TAG, "onResults: before replace " + command);
            contactNameSpeech = command.replace("call ", "");
            ContactMatchTask(contactNameSpeech.trim());
            //Log.i(TAG, "onResults: replaced " + command);
        } else {
            //no match
            speakOut("Didn't catch that, please try again", false);
        }
        //Log.d(TAG, "onResults() called with: results = [" + results + "]");
        //speechRecog.stopListening();
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        //Log.d(TAG, "onPartialResults() called with: partialResults = [" + partialResults + "]");
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        //Log.d(TAG, "onEvent() called with: eventType = [" + eventType + "], params = [" + params + "]");
    }

    public void onMicTapped(String utteranceId) {
        //recognise speech
        if (CallActivity.utteranceId.equals(utteranceId)) {
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                //Log.i(TAG, "onMicTapped: ");
                if (speechRecog == null) {
                    speechRecog = SpeechRecognizer.createSpeechRecognizer(this);
                    speechRecog.setRecognitionListener(this);
                    intentRecog = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intentRecog.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intentRecog.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
                    intentRecog.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                }
                if (!PermissionUtils.requestPermission(CallActivity.this, 0, Manifest.permission.RECORD_AUDIO))
                    return;
                if (!tts.isSpeaking()) {
                    speechRecog.stopListening();
                    speechRecog.startListening(intentRecog);
                }
            } else {
                //Log.e(TAG, "Speech Recognition not available on this device.");
            }
        }
    }

    //DETECT CURRENCY
    public void onCurrencyClicked(View view) {
        speakOut("Starting camera.. Picture will be taken in next 3 seconds", true);
        startCamera();
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.CAMERA)) {
        Intent intentCam = new Intent(CallActivity.this, MainActivity.class);
       /* Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider",
                getCameraFile());
        intentCam.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        intentCam.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);*/
        startActivityForResult(intentCam, CAMERA_IMAGE_REQUEST);

        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            //Log.i(TAG, "onActivityResult: " + data);
            Uri uri = Uri.fromFile(new File(data.getStringExtra(Utils.CAM_RESULT_OK_KEY)));
            //Log.i(TAG, "onActivityResult: uri" + uri);
            Bitmap bt = ImageHelper.loadSizeLimitedBitmapFromUri(uri, getContentResolver());
            //Log.i(TAG, "onActivityResult: bt" + bt);
           /* Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName()
                    + ".provider", getCameraFile());*/
            //Log.i(TAG, "onActivityResult: " + photoUri.getPath());
            if (data != null)
                fPath = data.getStringExtra(Utils.CAM_RESULT_OK_KEY);
            //Log.i(TAG, "onActivityResult: getData" + data.getData().getLastPathSegment() + data.getType());
            getImageUrl = ImagePath_MarshMallow.getPath(CallActivity.this, uri);
            // if (getImageUrl != null) {
            //Log.i(TAG, "onActivityResult: getImageUrl "+getImageUrl);
            speakOut("processing the image".toUpperCase(), true);
           // getImageUrl = uri.getPath();
            //setImg(Uri.parse(fPath));
            setImg(uri);
            extension = Uri.parse(fPath).getLastPathSegment();
            /* String ext[] = extension.split(".");*/
            imageURL = "temp.jpg";
            //Log.i(TAG, "onActivityResult: " + extension);
            if (origBitmap != null && origBitmap.getByteCount() > 4_000000) {
                //resize and send image
                //Log.i(TAG, "onActivityResult: >4mb");
                new ImageAnalysisTaskImage().execute();
            } else {
                //upload from url
                /*uploadFTP();
                imageSelected.setImageBitmap(resultBitmap);*/
                //[[TEST
                //Log.i(TAG, "onActivityResult: <4mb");
                new ImageAnalysisTaskImage().execute();
            }
            enableScanCurrencyBtn = true;
            // uploadImage();
            //}
        } else if (requestCode == RC_CALL) {
            //Log.i(TAG, "onActivityResult: RC_CALL" + RC_CALL);
            backFromCallActivity = true;
        }
    }

    //start the text to speech commands again
    private void restart() {
        firstCmd = 0;
        //Log.d(TAG, "onrestart() called with firstCmd=" + firstCmd);
        if (!tts.isSpeaking()) {
            if (isTTSready) {
                //Log.d(TAG, "onrestart() called isttsready=" + isTTSready);
                speakOut("Do you want to Detect currency or make a call? Say 1 to make a call, say 2 to detect currency.", false);
            }
        } else {
            tts.stop();
            if (isTTSready) {
                //Log.d(TAG, "onrestart() called isttsready=" + isTTSready);
                speakOut("Do you want to Detect currency or make a call? Say 1 to make a call, say 2 to detect currency.", false);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            speakOut("Do you want to Detect currency or make a call? Say 1 to make a call, say 2 to detect currency.", false);
            flowOnceStarted = true;
            firstCmd = 0;
        }
    }


    public void setImg(Uri uri) {
        if (uri != null) {
            //Log.i(TAG, "setImg: " + uri.getPath());
            try {
                // scale the image to save on bandwidth
                origBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                resultBitmap = scaleBitmapDown(
                        origBitmap,
                        MAX_DIMENSION);
                imageSelected.setImageBitmap(resultBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                //Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            //Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    //image size if too large is reduced
    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private void stopAnimation() {
        if (dialogLoader.isShowing())
            dialogLoader.cancel();
    }

    private void startAnimation() {
        dialogLoader = new Dialog(CallActivity.this, R.style.AppTheme_NoActionBar);
        dialogLoader.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#8D000000")));
        final View view = CallActivity.this.getLayoutInflater().inflate(R.layout.custom_dialog_loader, null);
        LottieAnimationView animationView = view.findViewById(R.id.loader);
        animationView.playAnimation();
        dialogLoader.setContentView(view);
        dialogLoader.setCancelable(false);
        dialogLoader.show();
    }

    public byte[] getImage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        //Log.i(TAG, "byte[] getImage: " + resultBitmap);
        if (origBitmap.getByteCount() < 4_000_000) {
            origBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            //Log.i(TAG, "getImage: origBitmap Bytes:<4Mb" + origBitmap.getByteCount());
        } else
            resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        final byte[] byteArray = stream.toByteArray();
        return byteArray;
//        return Base64.encodeToString(byteArray,Base64.DEFAULT);
    }

    class ImageAnalysisTaskImage extends AsyncTask<String, Void, BufferedReader> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            startAnimation();
        }

        @Override
        protected BufferedReader doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader br = null;

            try {
                URL url = new URL(API_URL_IMG);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestProperty("Prediction-Key", MY_KEY);
                urlConnection.setRequestProperty("Content-Type", "application/octet-stream");

                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);

                OutputStream os = urlConnection.getOutputStream();
                os.write(getImage());
                os.close();

                urlConnection.connect();


                if (200 <= urlConnection.getResponseCode() && urlConnection.getResponseCode() <= 299) {
                    br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                }

            } catch (Exception e) {

                //Log.d("ERROR", "ERROR " + e.toString());//IO Exception Prints in log cat not recognizing URL
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }

            return br;
        }

        @Override
        protected void onPostExecute(BufferedReader s) {
            super.onPostExecute(s);
            if (s != null) {
                //  Toast.makeText(CallActivity.this, "not null", Toast.LENGTH_SHORT).show();

                try {
                    StringBuilder sb = new StringBuilder();
                    String output;
                    while ((output = s.readLine()) != null) {
                        sb.append(output);
                    }

                    String result = sb.toString();
                    //Log.d("REPLY", result);

                    JSONObject jsonObject = new JSONObject(result);
                    if (result != null) {
                        parseResultJson(jsonObject);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(CallActivity.this, "EXP:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(CallActivity.this, "null", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void parseResultJson(JSONObject response) {
        try {
            JSONObject jo = response;
            JSONArray jarr = jo.getJSONArray("predictions");
            float prediction = 0;
            String Tag = "";
            DecimalFormat dFormat = new DecimalFormat("#.##");
            boolean foundUSDtag = false;
            for (int i = 0; i < jarr.length(); i++) {
                prediction = 10.0f;   //should be greaterthan 10% probability
                JSONObject jobj = jarr.getJSONObject(i);
                float f = Float.parseFloat(jobj.getString("probability"));
                f = f * 100;
                //Log.i(TAG, "onResponse: " + jobj.getString("tagName"));
                //Log.i(TAG, "onResponse: " + f + ">," + prediction);
                if (f > prediction) {
                    String[] tagName = jobj.getString("tagName").split("_");
                    if (tagName[0].equalsIgnoreCase(COUNTRY_CURRENCY_LABEL)) {
                        foundUSDtag = true;
                        if (f >= 30) {
                            Tag = getTagFiltered(jobj.getString("tagName"));
                            bestGuessLabelTV.setText(Tag + " (probability " + String.valueOf(dFormat.format(f)) + ")");
                        } else {
                            Tag = "Best Guess is " + getTagFiltered(jobj.getString("tagName")) + " with a Low Probability";
                            bestGuessLabelTV.setText("Best Guess - " + getTagFiltered(jobj.getString("tagName")) + " (probability " + String.valueOf(dFormat.format(f)) + ")");
                        }
                        break;
                    }
                    else{
                        Tag = "not a currency";
                        bestGuessLabelTV.setText(Tag);
                        //Log.i(TAG, "onResponse: " + Tag + " - " + response.toString());
                        break;
                    }
                } else {
                    Tag = "not a currency";
                    bestGuessLabelTV.setText(Tag);
                    //Log.i(TAG, "onResponse: " + Tag + " - " + response.toString());
                    break;
                }
            }
            if (!foundUSDtag) {
                Tag = "not a currency";
                bestGuessLabelTV.setText(Tag);
            }
            stopAnimation();
            //Float.valueOf(dFormat.format(prediction));
            speakOut(Tag + "\n " + askAgainForCurrencyString(), false);
        } catch (Exception e) {
            //Log.e(TAG, "onResponse: " + e.getMessage());
            stopAnimation();
            Toast.makeText(CallActivity.this, "Error in fetching prediction" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public String getTagFiltered(String s) {
        String temp[] = s.split(" ");
        String res = temp[0].replaceAll("_", " ").toUpperCase();
        if (res.contains("BACK"))
            res = res.replace("BACK","");
        if (res.contains("FRONT"))
            res = res.replace("FRONT","");
        return res;
    }

    private String askAgainForCurrencyString() {
        firstCmd = 0;
        return "Do you want to Detect currency or make a call? Say CALL to make a call or say DETECT to detect currency or say EXIT to exit.";
    }

    private String askAgainToCallString() {
        firstCmd = 0;
        return "Do you want to Detect currency or make a call? Say DETECT to detect currency or say CALL to make a callor say EXIT to exit.";
    }

    //FTP upload for link
    private void uploadFTP() {
        if (extension != null) {
            destUploadedPath = UPLOADPATH + imageURL;
            UploadFTPTask uploadFTP = new UploadFTPTask();
            uploadFTP.execute(getImageUrl, destUploadedPath);
            //Log.i(TAG, "UploadFTPCall: " + getImageUrl + ", " + destUploadedPath);
        }
    }

    class ImageAnalysisTask extends AsyncTask<String, Void, BufferedReader> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            startAnimation();
        }

        @Override
        protected BufferedReader doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader br = null;

            try {
                URL url = new URL(API_URL);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestProperty("Prediction-Key", "9d393810c19a4865b2ee2d513fdc9152");
                urlConnection.setRequestProperty("Content-Type", "application/json");

                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("Url", "http://aorgandon.hostoise.com/currencyd/" + imageURL);
                //{"Url": "http://aorgandon.hostoise.com/currencyd/naira10.jpeg"}
                //ftp://Hostoise@182.50.132.7/AOrganDon/currencyd/naira10.jpeg
                OutputStreamWriter os = new OutputStreamWriter(urlConnection.getOutputStream());
                os.write(jsonObject.toString());
                os.close();

                urlConnection.connect();


                if (200 <= urlConnection.getResponseCode() && urlConnection.getResponseCode() <= 299) {
                    br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                }

            } catch (Exception e) {
                //Log.d("ERROR", "ERROR " + e.toString());//IO Exception Prints in log cat not recognizing URL
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
            }

            return br;
        }

        @Override
        protected void onPostExecute(BufferedReader s) {
            super.onPostExecute(s);
            if (s != null) {
                //  Toast.makeText(CallActivity.this, "not null", Toast.LENGTH_SHORT).show();
                try {
                    StringBuilder sb = new StringBuilder();
                    String output;
                    while ((output = s.readLine()) != null) {
                        sb.append(output);
                    }

                    String result = sb.toString();
                    //Log.d("REPLY", result);

                    JSONObject jsonObject = new JSONObject(result);
                    /*JSONParser parser = new JSONParser();*/
                    if (result != null) {
                        parseResultJson(jsonObject);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(CallActivity.this, "EXP:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(CallActivity.this, "null", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //srcpath/destpath
    public class UploadFTPTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(CallActivity.this);
            pd.requestWindowFeature(Window.FEATURE_NO_TITLE);
            pd.setMessage("Uploading...");
            pd.setIndeterminate(false);
            pd.setCancelable(false);
            pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pd.setProgress(0);
            pd.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String ans = "";
            try {
                mFTPClient = new FTPClient();
                mFTPClient.connect("182.50.132.7");

                if (mFTPClient.login("Hostoise", "ys73!e2P")) {
                    mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);

                    BufferedInputStream buffIn = null;
                    final File file = new File(params[0]);
                    buffIn = new BufferedInputStream(new FileInputStream(file));
                    mFTPClient.enterLocalPassiveMode();
                    streamListener = new CopyStreamAdapter() {

                        @Override
                        public void bytesTransferred(long totalBytesTransferred,
                                                     int bytesTransferred, long streamSize) {

                            int percent = (int) (totalBytesTransferred * 100 / file.length());
                            pd.setProgress(percent);
                            publishProgress();

                            if (totalBytesTransferred == file.length()) {
                                System.out.println("100% transfered");

                                removeCopyStreamListener(streamListener);
                            }
                        }
                    };
                    mFTPClient.setCopyStreamListener(streamListener);
                    Boolean status = mFTPClient.storeFile(params[1], buffIn);
                    if (status) {
                        ans = "true";
                    } else {
                        ans = "false";
                    }

                    buffIn.close();
                    mFTPClient.logout();
                    mFTPClient.disconnect();

                }
            } catch (FileNotFoundException e) {
                ans = "U-file not found-" + "\n" + e.getMessage();
            } catch (SocketException e) {
                ans = "U-socket-" + "\n" + e.getMessage();
            } catch (IOException e) {
                ans = "U-IO-" + "\n" + e.getMessage();
            }
            return ans;
        }

        protected void onProgressUpdate(String... values) {
            pd.setProgress(Integer.parseInt(values[0]));
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            pd.cancel();
            if (s.compareTo("true") == 0) {
                //if upload completes then give that link to analysis
                analyseImage();
                //Log.i("TAG", "onPostExecute: " + s);
            } else {
                Toast.makeText(CallActivity.this, s, Toast.LENGTH_SHORT).show();
                //Log.e("TAG", "onPostExecute: " + s);
            }
        }
    }

    private void analyseImage() {
        new ImageAnalysisTask().execute();
    }

}