package com.saisumanth.documenttoaudioconverter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    public final int AUTH_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();

        Button loginButton = findViewById(R.id.login_button);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<AuthUI.IdpConfig> provider = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build(),
                        new AuthUI.IdpConfig.GoogleBuilder().build(),
                        new AuthUI.IdpConfig.PhoneBuilder().build()
                );

                Intent intent = AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(provider)
                        .setLogo(R.drawable.logo1)
                        .setTosAndPrivacyPolicyUrls("https://policies.google.com/terms?hl=en-US","https://policies.google.com/privacy?hl=en-US")
                        .build();


                startActivityForResult(intent,AUTH_CODE);

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if(requestCode == AUTH_CODE){
            if(resultCode == RESULT_OK){

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user.getMetadata().getCreationTimestamp() == user.getMetadata().getLastSignInTimestamp()){

                    Toast.makeText(this,"Welcome!!",Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(this,"Welcome back!!",Toast.LENGTH_LONG).show();
                }

                Intent intent = new Intent(this,FilesActivity.class);
                startActivity(intent);
                this.finish();



            }else{

                IdpResponse response = IdpResponse.fromResultIntent(data);
                if(response == null){
                    Log.d("Authentication","onActivityResult the user has cancelled sign in request");
                }else{
                    Log.e("Authentication","onActivityResult:",response.getError());
                }

            }
        }
    }

}