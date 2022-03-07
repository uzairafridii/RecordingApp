package com.example.recordingapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements HBRecorderListener {

    FloatingActionButton fabGuide, fabMessage, fabWhatsapp, fabHelp, fabScreenshot;
    boolean isFABOpen = false;
    TextView timerAudio;
    ImageView image;
    List<Uri> imageUriList, audioRecordList;
    AppCompatButton stopRecording;
    CountDownTimer countDownTimer;
    String audioPath, videoPath;

    // audio recording
    MediaRecorder mediaRecorder;
    // screen capture
    HBRecorder hbRecorder;
    ContentResolver resolver;
    ContentValues contentValues;
    Uri mUri;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        if (!isPermissionEnabled()) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1001);
        }
    }

    // media recorder setup
    private void setUpMediaRecorder() {

        audioPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/"
                + "AudioRecording" + new Random().nextInt() + ".3gp";
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioPath);

        Log.d("savingPath", "setUpMediaRecorder: " + audioPath);
    }

    // init views
    private void initViews() {
        // hb recorder
        hbRecorder = new HBRecorder(MainActivity.this, this);
        setOutputPath(); // output file path for hb recorder
        // views
        timerAudio = findViewById(R.id.timerAudio);
        stopRecording = findViewById(R.id.stopRecording);
        imageUriList = new ArrayList<>();
        audioRecordList = new ArrayList<>();
        image = findViewById(R.id.imageScreenshot);
        // fab menus
        fabScreenshot = findViewById(R.id.fabScreenShot);
        fabHelp = findViewById(R.id.fabHelp);
        fabGuide = findViewById(R.id.fabGuide);
        fabMessage = findViewById(R.id.fabMessage);
        fabWhatsapp = findViewById(R.id.fabWhatsapp);

        // click on whatsapp message button
        fabWhatsapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String phoneNumberWithCountryCode = "+923047901748";
                String message = "Message from target support";

                startActivity(
                        new Intent(Intent.ACTION_VIEW,
                                Uri.parse(
                                        String.format("https://api.whatsapp.com/send?phone=%s&text=%s", phoneNumberWithCountryCode, message)
                                )
                        )
                );
            }
        });
        // fab guide click to open guide dialog
        fabGuide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View mView = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.user_guide_dialog, null);
                AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                alert.setView(mView);
                Dialog dialog = alert.create();
                dialog.setCanceledOnTouchOutside(false);

                // init dialog views
                AppCompatButton closeBtn = mView.findViewById(R.id.closeBtn);
                closeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                dialog.show();

            }
        });
        // fab help click to expand fab menus
        fabHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isFABOpen) {
                    showFABMenu();
                } else {
                    closeFABMenu();
                }

            }
        });
        // click on fab message button
        fabMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMessageDialog();
            }
        });
        // fab screenshot click
        fabScreenshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveScreenshot();
            }
        });
        // click on stop recording button
        stopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaRecorder.stop();
                // invisible stop recording button
                if (stopRecording.getVisibility() == View.VISIBLE)
                    stopRecording.setVisibility(View.GONE);

                // cancel timer and list the audio file path
                countDownTimer.cancel();
                audioRecordList.add(Uri.parse(audioPath));
                showMessageDialog();
                Toast.makeText(MainActivity.this, "Stop Recording", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // save screenshot
    private void saveScreenshot() {
        fabScreenshot.setVisibility(View.GONE);
        // get root view
        View rootView = getWindow().getDecorView().findViewById(R.id.rootContainer);
        View screenView = rootView.getRootView();
        screenView.setDrawingCacheEnabled(true);
        // get bitmap
        Bitmap bitmap = Bitmap.createBitmap(screenView.getDrawingCache());
        screenView.setDrawingCacheEnabled(false);

        // get image uri
        Uri uri = getImageUri(MainActivity.this, bitmap);
        image.setImageURI(uri);
        imageUriList.add(uri);
        Toast.makeText(this, "" + uri, Toast.LENGTH_LONG).show();
        // show dialog
        showMessageDialog();

        // show help icon after screenshot save
        fabHelp.setVisibility(View.VISIBLE);

    }

    // get image Uri
    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Screenshot" + new Random().nextInt(), null);
        return Uri.parse(path);
    }

    // message dialog
    private void showMessageDialog() {
        View mView = LayoutInflater.from(MainActivity.this)
                .inflate(R.layout.dialog_message, null);
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setView(mView);
        Dialog dialog = alert.create();
        dialog.setCanceledOnTouchOutside(false);

        EditText edMessage;
        ImageView cameraIcon, videoIcon, voiceIcon, textIcon;
        AppCompatButton goBtn;
        TextView screenshotCounts, videoCounts, voiceCount;

        screenshotCounts = mView.findViewById(R.id.screenshotCounts);
        videoCounts = mView.findViewById(R.id.videoCounts);
        voiceCount = mView.findViewById(R.id.voiceCounts);

        if (imageUriList.size() > 0)
            screenshotCounts.setText("" + imageUriList.size());
        if (audioRecordList.size() > 0)
            voiceCount.setText("" + audioRecordList.size());


        goBtn = mView.findViewById(R.id.btnGo);
        cameraIcon = mView.findViewById(R.id.cameraIcon);
        videoIcon = mView.findViewById(R.id.videoIcon);
        voiceIcon = mView.findViewById(R.id.voiceIcon);
        textIcon = mView.findViewById(R.id.textIcon);

        // go button click
        goBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imageUriList.clear();
                audioRecordList.clear();
                dialog.dismiss();
                // close fab menu
                if (isFABOpen)
                    closeFABMenu();

            }
        });

        // click on camera icon to take screenshot
        cameraIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                fabHelp.setVisibility(View.GONE);
                fabScreenshot.setVisibility(View.VISIBLE);
                closeFABMenu();
                dialog.dismiss();

            }
        });

        // click on recording
        voiceIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                setUpMediaRecorder();
                Toast.makeText(MainActivity.this, "Recording Started", Toast.LENGTH_SHORT).show();
                stopRecording.setVisibility(View.VISIBLE);
                try {
                    mediaRecorder.prepare();
                    mediaRecorder.start();

                    // timer for one second
                    countDownTimer = new CountDownTimer((60000 / 3), 1000) {
                        public void onTick(long millisUntilFinished) {
                            // Used for formatting digit to be in 2 digits only
                            NumberFormat f = new DecimalFormat("00");
                            long min = (millisUntilFinished / 60000) % 60;
                            long sec = (millisUntilFinished / 1000) % 60;
                            timerAudio.setText(f.format(min) + ":" + f.format(sec));
                        }

                        // When the time limit is completed stop the recorder
                        public void onFinish() {
                            countDownTimer.cancel();
                            mediaRecorder.stop();
                            mediaRecorder.release();
                        }
                    }.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        // click on video icon
        videoIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecordingScreen();
            }
        });

        dialog.show();
    }

    // show fab menu
    private void showFABMenu() {
        isFABOpen = true;
        fabGuide.animate().translationY(-getResources().getDimension(R.dimen.standard_55));
        fabMessage.animate().translationY(-getResources().getDimension(R.dimen.standard_105));
        fabWhatsapp.animate().translationY(-getResources().getDimension(R.dimen.standard_155));
    }

    // hide fab menu
    private void closeFABMenu() {
        isFABOpen = false;
        fabGuide.animate().translationY(0);
        fabMessage.animate().translationY(0);
        fabWhatsapp.animate().translationY(0);
    }


    // check for permission
    private boolean isPermissionEnabled() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    // hb recording call back methods and start screen capturing dialog
    private void startRecordingScreen() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
        screenCaptureIntentResult.launch(permissionIntent);
    }

    private ActivityResultLauncher<Intent> screenCaptureIntentResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                    if (result.getResultCode() == RESULT_OK) {
                        hbRecorder.startScreenRecording(result.getData(), result.getResultCode(), MainActivity.this);
                        new CountDownTimer(30000, 1000) {

                            @Override
                            public void onTick(long l) {

                            }

                            @Override
                            public void onFinish() {
                                hbRecorder.stopScreenRecording();
                            }
                        }.start();
                    } else {
                        Toast.makeText(MainActivity.this, "Error in Activity Result", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    public void HBRecorderOnStart() {
        Toast.makeText(this, "Screen Recording Started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void HBRecorderOnComplete() {
        Toast.makeText(this, "Screen Recording Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        Toast.makeText(this, "Error : " + reason, Toast.LENGTH_SHORT).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setOutputPath() {
        String title = "ScreenRecorder" + new Random().nextInt();
        // for android 11 and below save file to storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = getContentResolver();
            contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies");
            contentValues.put(MediaStore.Video.Media.TITLE, title);
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, title);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            hbRecorder.setFileName(title);
            hbRecorder.setOutputUri(mUri);
        } else {
            videoPath = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
            hbRecorder.setOutputPath(videoPath);
        }
    }
}