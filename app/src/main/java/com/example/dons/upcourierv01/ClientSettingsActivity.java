package com.example.dons.upcourierv01;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ClientSettingsActivity extends AppCompatActivity {

    private EditText mNameClientField, mPhoneClientField;

    private Button mBtnSaveData, mBtnBack;

    private ImageView mClientImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mClientDatabase;

    private Uri resultUri;

    private String userID, mClientName, mClientPhone, clientImageUrl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_settings);

        mNameClientField = (EditText) findViewById(R.id.clientName);
        mPhoneClientField = (EditText) findViewById(R.id.clientPhone);

        mBtnSaveData = (Button) findViewById(R.id.saveDataClient);
        mBtnBack = (Button) findViewById(R.id.backSettingsClient);

        mClientImage = (ImageView) findViewById(R.id.imgClientProfile);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mClientDatabase = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Clients").child(userID);

        getClientInfo();

        mClientImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 2);
            }
        });

        mBtnSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveClientInformation();
            }
        });
        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });
    }

    private void getClientInfo(){
        mClientDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("clientName") != null) {
                        mClientName = map.get("clientName").toString();
                        mNameClientField.setText(mClientName);
                    }

                    if(map.get("clientPhone") != null) {
                        mClientPhone = map.get("clientPhone").toString();
                        mPhoneClientField.setText(mClientPhone);
                    }

                    if(map.get("clientImageUrl") != null){
                        clientImageUrl = map.get("clientImageUrl").toString();
                        Glide.with(getApplication()).load(clientImageUrl).into(mClientImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveClientInformation() {
        mClientName = mNameClientField.getText().toString();
        mClientPhone = mPhoneClientField.getText().toString();

        Map clientInfo = new HashMap();
        clientInfo.put("clientName", mClientName);
        clientInfo.put("clientPhone", mClientPhone);
        mClientDatabase.updateChildren(clientInfo);

        if(resultUri != null){
            StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images")
                    .child(userID);
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream byteArrayInputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 20, byteArrayInputStream);
            byte[] data = byteArrayInputStream.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    finish();
                    return;
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> downloadUri = taskSnapshot.getStorage().getDownloadUrl();
                    while(!downloadUri.isComplete());
                    Uri url = downloadUri.getResult();

                    Log.d("UpCourierTest", "onSuccess: ImagenCargad");
                    Map newImage = new HashMap();


                    newImage.put("clientImageUrl", url.toString());
                    mClientDatabase.updateChildren(newImage);

                    finish();
                    return;
                }
            });
        }else {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mClientImage.setImageURI(resultUri);
        }

    }
}