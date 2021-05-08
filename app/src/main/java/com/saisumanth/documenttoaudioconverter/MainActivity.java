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

    public Uri pdfUri;
    Button selectPDF;
    Button convertToText;
    public static ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectPDF = findViewById(R.id.selectpdf);
        convertToText = findViewById(R.id.convert_to_text);
        progressDialog = new ProgressDialog(MainActivity.this);
        pdfUri = null;
        progressDialog.setMessage("Converting...");
        progressDialog.setCanceledOnTouchOutside(false);



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


                if(pdfUri == null){

                    Toast.makeText(getApplicationContext(),"Select PDF",Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();


                }else{

                    convertToText();

                    startActivity(new Intent(MainActivity.this,DisplayActivity.class));
                    finish();


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
            selectPDF.setText("PDF Selected");
            //Toast.makeText(getApplicationContext(),pdfUri.toString(),Toast.LENGTH_SHORT).show();

        }else{
            Toast.makeText(getApplicationContext(),"Error in selecting pdf",Toast.LENGTH_LONG).show();
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

                                for(Text.TextBlock text : visionText.getTextBlocks()){

                                    DisplayActivity.multitext.append(text.getText() + " ");

                                }

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