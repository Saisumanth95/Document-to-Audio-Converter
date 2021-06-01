package com.saisumanth.documenttoaudioconverter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

public class EditActivity extends AppCompatActivity {

    EditText fileContent, fileName;
    Button save;
    public int pos;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_DocumentToAudioConverter);
        setContentView(R.layout.activity_edit);

        fileContent = findViewById(R.id.edit_file_content);
        fileName = findViewById(R.id.edit_file_name);
        save = findViewById(R.id.edit_save_to_file);
        progressDialog = new ProgressDialog(EditActivity.this);

        Intent intent = getIntent();

        pos = intent.getIntExtra("position",0);

        fileName.setText(FilesActivity.items.get(pos).getFilename());

        new Thread(new Runnable(){

            public void run(){

                String text = "";

                try {

                    URL url = new URL(FilesActivity.items.get(pos).getLink());

                    HttpURLConnection conn=(HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(60000);
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String str;
                    while ((str = in.readLine()) != null) {
                        text += str;
                    }
                    in.close();
                } catch (Exception e) {
                    Log.d("Connection",e.toString());
                }

                String finalText = text;
                EditActivity.this.runOnUiThread(new Runnable(){
                    public void run(){
                        fileContent.setText(finalText);
                    }
                });

            }
        }).start();


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                progressDialog.setMessage("Uploding....");
                progressDialog.setCanceledOnTouchOutside(false);

                if(fileName.getText().toString() == null || fileName.getText().toString() == ""){

                    Toast.makeText(getApplicationContext(),"Enter file name",Toast.LENGTH_SHORT).show();
                    return;

                }

                if(fileContent.getText().toString() == null || fileContent.getText().toString() == ""){

                    Toast.makeText(getApplicationContext(),"Enter file content",Toast.LENGTH_SHORT).show();
                    return;

                }

                progressDialog.show();

                deleteFile();

                File file = null;

                try {

                    File outputDir = getApplicationContext().getCacheDir();

                    file = File.createTempFile(fileName.getText().toString(),".txt",outputDir);

                    FileWriter writer = new FileWriter(file);

                    writer.write(fileContent.getText().toString());

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
                            String userid = FilesActivity.items.get(pos).getUserid();

                            String title = fileName.getText().toString();

                            Item item = new Item(title,link, FilesActivity.items.get(pos).getTime(),userid);

                            FirebaseFirestore.getInstance().collection("documents").add(item)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {

                                            Toast.makeText(getApplicationContext(),"File uploaded!",Toast.LENGTH_SHORT).show();

                                            progressDialog.dismiss();

                                            Intent intent = new Intent(EditActivity.this, FilesActivity.class);
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

    public void deleteFile(){

        FirebaseFirestore.getInstance()
                .collection("documents")
                .whereEqualTo("userid",FilesActivity.items.get(pos).getUserid())
                .whereEqualTo("time",FilesActivity.items.get(pos).getTime())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        List<DocumentSnapshot> snapshotList = queryDocumentSnapshots.getDocuments();

                        WriteBatch batch = FirebaseFirestore.getInstance().batch();

                        for(DocumentSnapshot snapshot : snapshotList){
                            batch.delete(snapshot.getReference());
                        }

                        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {

                                StorageReference storageReference;
                                String filelink = FilesActivity.items.get(pos).getLink();
                                storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(filelink);
                                storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(getApplicationContext(),"File deleted",Toast.LENGTH_SHORT).show();
                                    }
                                });

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(),"Unable to delete!",Toast.LENGTH_LONG).show();
                            }
                        });


                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Toast.makeText(getApplicationContext(),"Unable to select documents",Toast.LENGTH_LONG).show();

            }
        });


    }

}