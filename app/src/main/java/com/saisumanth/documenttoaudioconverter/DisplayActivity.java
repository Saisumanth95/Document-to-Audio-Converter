package com.saisumanth.documenttoaudioconverter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.IntentCompat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class DisplayActivity extends AppCompatActivity {


    public EditText multitext;

    public static ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_DocumentToAudioConverter);
        setContentView(R.layout.activity_display);

        multitext = findViewById(R.id.file_content);

        EditText filename = findViewById(R.id.file_name);

        Button save = findViewById(R.id.save_to_file);

        progressDialog = new ProgressDialog(DisplayActivity.this);

        MainActivity.progressDialog.dismiss();

        multitext.setText(MainActivity.convertedText);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                progressDialog.setMessage("Uploding....");
                progressDialog.setCanceledOnTouchOutside(false);

                progressDialog.show();

                if(filename.getText().toString() == null || filename.getText().toString() == ""){

                    Toast.makeText(getApplicationContext(),"Enter file name",Toast.LENGTH_SHORT).show();

                    return;

                }

                if(multitext.getText().toString() == null || multitext.getText().toString() == ""){

                    Toast.makeText(getApplicationContext(),"Enter file content",Toast.LENGTH_SHORT).show();

                    return;

                }

                File file = null;

                try {

                    File outputDir = getApplicationContext().getCacheDir();

                    file = File.createTempFile(filename.getText().toString(),".txt",outputDir);

                    FileWriter writer = new FileWriter(file);

                    writer.write(multitext.getText().toString());

                    writer.close();

                } catch (IOException e) {
                    e.printStackTrace();

                    Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();

                    return;
                }


                FirebaseStorage storage = FirebaseStorage.getInstance();

                StorageReference storageReference = storage.getReference();

                Uri uploadFile = Uri.fromFile(file);

                String randomKey = UUID.randomUUID().toString();

                final StorageReference ref = storageReference.child("files/" + randomKey);

                UploadTask uploadTask = ref.putFile(uploadFile);

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                        if(!task.isSuccessful()){
                            throw task.getException();
                        }


                        Log.d("This is test", "then: " + ref.getDownloadUrl());

                        return ref.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {

                        if(task.isSuccessful()){

                            String link = task.getResult().toString();
                            String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                            String title = filename.getText().toString();

                            Item item = new Item(title,link, Timestamp.now(),userid);

                            FirebaseFirestore.getInstance().collection("documents").add(item)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {

                                            Toast.makeText(getApplicationContext(),"File uploaded!",Toast.LENGTH_SHORT).show();

                                            progressDialog.dismiss();

                                            Intent intent = new Intent(DisplayActivity.this, FilesActivity.class);
                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                            startActivity(intent);

                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getApplicationContext(),"Error Uploading!",Toast.LENGTH_LONG).show();
                                    progressDialog.dismiss();
                                }
                            });

                        }else{

                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(),"Unable to upload file",Toast.LENGTH_SHORT).show();

                        }



                    }

                });

            }
        });

    }

}
