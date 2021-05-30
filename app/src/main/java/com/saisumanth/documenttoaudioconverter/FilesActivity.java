package com.saisumanth.documenttoaudioconverter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.common.MlKit;
import com.google.mlkit.common.sdkinternal.MlKitContext;

import java.util.ArrayList;
import java.util.List;

public class FilesActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private Adapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    public static ArrayList<Item> items;
    private FloatingActionButton floatingActionButton;
    public static ProgressDialog progressDialog;
    private TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_DocumentToAudioConverter);
        setContentView(R.layout.activity_files);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if(user == null){
            startActivity(new Intent(FilesActivity.this,LoginActivity.class));
            finish();
        }

        items = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        layoutManager = new LinearLayoutManager(FilesActivity.this);
        recyclerView.setHasFixedSize(true);
        adapter = new Adapter(items,this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout = findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        floatingActionButton = findViewById(R.id.floatingActionButton);
        progressDialog = new ProgressDialog(FilesActivity.this);
        error = findViewById(R.id.errorView);

        error.setVisibility(View.GONE);


        onLoadingRefresh();


        adapter.setOnItemClickListener(new Adapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {

                Intent intent = new Intent(FilesActivity.this,TextActivity.class);

                intent.putExtra("position",position);

                startActivity(intent);

            }
        });

        adapter.setOnItemLongClickListener(new Adapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(int position) {

                new AlertDialog.Builder(FilesActivity.this)
                        .setTitle("Delete Entry")
                        .setMessage("Are you sure do you want to delete " + items.get(position).getFilename() + " ?")
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                progressDialog.setMessage("Deleting...");
                                progressDialog.setCanceledOnTouchOutside(false);
                                progressDialog.show();

                                FirebaseFirestore.getInstance()
                                        .collection("documents")
                                        .whereEqualTo("userid",items.get(position).getUserid())
                                        .whereEqualTo("time",items.get(position).getTime())
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
                                                        String filelink = items.get(position).getLink();
                                                        storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(filelink);
                                                        storageReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Toast.makeText(getApplicationContext(),"File deleted",Toast.LENGTH_SHORT).show();
                                                                onLoadingRefresh();
                                                                progressDialog.dismiss();
                                                            }
                                                        });

                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(getApplicationContext(),"Unable to delete!",Toast.LENGTH_LONG).show();
                                                        progressDialog.dismiss();
                                                    }
                                                });


                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                        Toast.makeText(getApplicationContext(),"Unable to select documents",Toast.LENGTH_LONG).show();
                                        progressDialog.dismiss();

                                    }
                                });


                            }
                        }).setNegativeButton("NO",null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();


            }
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(FilesActivity.this, MainActivity.class));

            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.action_user){

            showDialog();

            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {

        items.clear();
        retreiveData();

    }

    private void onLoadingRefresh(){

        swipeRefreshLayout.post(
                new Runnable() {
                    @Override
                    public void run() {

                        items.clear();
                        retreiveData();

                    }
                }
        );

    }

    private void showDialog(){

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        String username = user.getDisplayName();
        String email = user.getEmail();
        String phoneNumber = user.getPhoneNumber();


        new AlertDialog.Builder(FilesActivity.this)
                .setTitle("User details")
                .setMessage("Username : " + username + "\n\nEmail : " + email + "\n\nPhone Number : " + phoneNumber)
                .setPositiveButton("SIGN OUT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        FirebaseAuth.getInstance().signOut();
                        AuthUI.getInstance().signOut(FilesActivity.this);
                        startActivity(new Intent(FilesActivity.this,LoginActivity.class));
                        finish();

                    }
                })
                .setIcon(R.drawable.ic_baseline_person_24)
                .setCancelable(true)
                .show();


    }

    private void retreiveData(){

        String userid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        swipeRefreshLayout.setRefreshing(true);
        FirebaseFirestore.getInstance().collection("documents")
                .orderBy("time", Query.Direction.DESCENDING)
                .whereEqualTo("userid",userid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                        if(task.isSuccessful()){

                            for(QueryDocumentSnapshot documents : task.getResult()){

                                items.add(new Item(documents.getString("filename"),
                                        documents.getString("link"),
                                        documents.getTimestamp("time"),
                                        documents.getString("userid")));

                                adapter.notifyDataSetChanged();

                            }


                            if(items.size() == 0){

                                error.setVisibility(View.VISIBLE);
                                adapter.notifyDataSetChanged();

                            }

                            swipeRefreshLayout.setRefreshing(false);

                        }else {
                            Log.d("This is test", "Error getting documents: ", task.getException());

                            swipeRefreshLayout.setRefreshing(false);
                        }


                    }
                });


    }

}