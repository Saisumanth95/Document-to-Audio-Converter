package com.saisumanth.documenttoaudioconverter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.mlkit.common.MlKit;
import com.google.mlkit.common.sdkinternal.MlKitContext;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final int PDF_SELECT = 1;
    private final int PHOTO_SELECT = 2;

    public Uri pdfUri;
    public Uri photoUri;
    ImageButton selectPDF,selectPhoto;
    public Bitmap photo;
    Button convertToText;
    public static ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_DocumentToAudioConverter);
        setContentView(R.layout.activity_main);

        selectPDF = findViewById(R.id.selectpdf);
        selectPhoto = findViewById(R.id.selectphoto);
        convertToText = findViewById(R.id.convert_to_text);
        progressDialog = new ProgressDialog(MainActivity.this);
        pdfUri = null;
        progressDialog.setMessage("Converting...");
        progressDialog.setCanceledOnTouchOutside(false);

        convertToText.setEnabled(false);

        selectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isPermissionRequested(MainActivity.this)){

                    Intent photoPick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                    startActivityForResult(photoPick,PHOTO_SELECT);

                }


            }
        });



        selectPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isPermissionRequested(MainActivity.this)){

                    Intent intent = new Intent();
                    intent.setType("application/pdf");
                    intent.setAction(intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent,PDF_SELECT);

                }else{

                    Toast.makeText(getApplicationContext(),"Permission Required",Toast.LENGTH_SHORT).show();

                }

            }
        });


        convertToText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progressDialog.show();

                if(pdfUri != null){

                    convertToText();

                    startActivity(new Intent(MainActivity.this,DisplayActivity.class));
                    finish();

                }else if(photoUri != null){

                    convertBitmapToText(photo);

                    startActivity(new Intent(MainActivity.this,DisplayActivity.class));
                    finish();

                }else{

                    Toast.makeText(getApplicationContext(),"Select PDF or Image",Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();

                }


            }
        });



    }

    public void convertToText(){

        File file = null;

        try {
            file = FileUtil.from(MainActivity.this,pdfUri);
            Log.d("file", "File...:::: uti - "+file .getPath()+" file -" + file + " : " + file .exists());

        } catch (IOException e) {
            e.printStackTrace();
        }

        pdfToBitmap(file);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if(requestCode==PDF_SELECT && resultCode==RESULT_OK && data!=null && data.getData()!=null){
            pdfUri = data.getData();
            //selectPDF.setImageResource(R.drawable.pdfselected);
            selectPDF.setBackground(getDrawable(R.drawable.pdfselected));
            convertToText.setEnabled(true);
            selectPhoto.setEnabled(false);
            //Toast.makeText(getApplicationContext(),pdfUri.toString(),Toast.LENGTH_SHORT).show();

        }else if(requestCode==PHOTO_SELECT && resultCode==RESULT_OK && data!=null && data.getData()!=null){

            photoUri = data.getData();

            selectPhoto.setBackground(getDrawable(R.drawable.photo_selected));

            try {
                photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            convertToText.setEnabled(true);
            selectPDF.setEnabled(false);

        }else{
            Toast.makeText(getApplicationContext(),"Error in selecting pdf or Image",Toast.LENGTH_LONG).show();
        }

    }


    public void convertBitmapToText(Bitmap bitmap){

        InputImage image = InputImage.fromBitmap(bitmap,0);

        TextRecognizer recognizer = TextRecognition.getClient();

        Task<Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                // Task completed successfully

                                DisplayActivity.multitext.append(visionText.getText() + "\n");

                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...

                                        Toast.makeText(getApplicationContext(),"Failed " + e.getMessage(),Toast.LENGTH_LONG).show();

                                    }
                                });

    }

    private void pdfToBitmap(File pdfFile){

        try {
            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));

            Bitmap bitmap;
            final int pageCount = renderer.getPageCount();
            for (int i = 0; i < pageCount; i++) {

                PdfRenderer.Page page = renderer.openPage(i);

                int width = getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth();
                int height = getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight();
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                convertBitmapToText(bitmap);

                // close the page
                page.close();

            }

            // close the renderer
            renderer.close();
        } catch (Exception ex) {
            ex.printStackTrace();

            Toast.makeText(getApplicationContext(), ex.getMessage(),Toast.LENGTH_LONG).show();

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults[0] == PackageManager.PERMISSION_GRANTED){

            Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show();

        }else if(grantResults[0] == PackageManager.PERMISSION_DENIED){
            Toast.makeText(this,"Permission Required",Toast.LENGTH_SHORT).show();
        }

    }

    public boolean isPermissionRequested(Activity activity){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

            if(ContextCompat.checkSelfPermission(activity,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                return true;
            }else{

                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

                return false;

            }

        }else{

            return true;

        }

    }


}