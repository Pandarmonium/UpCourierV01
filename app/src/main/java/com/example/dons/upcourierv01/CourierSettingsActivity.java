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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CourierSettingsActivity extends AppCompatActivity {

    private EditText mNameCourierField, mPhoneCourierField;

    private Button mBtnSaveData, mBtnBack;

    private ImageView mCourierImage;

    private FirebaseAuth mAuth;
    private DatabaseReference mCourierDatabase;

    private Uri resultUri;

    private String userID, mCourierName, mCourierPhone, courierImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courier_settings);
        mNameCourierField = (EditText) findViewById(R.id.courierName);
        mPhoneCourierField = (EditText) findViewById(R.id.courierPhone);

        mBtnSaveData = (Button) findViewById(R.id.saveDataCourier);
        mBtnBack = (Button) findViewById(R.id.backSettingsCourier);

        mCourierImage = (ImageView) findViewById(R.id.imgCourierProfile);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mCourierDatabase = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Couriers").child(userID);

        getCourierInfo();

        mCourierImage.setOnClickListener(new View.OnClickListener() {
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
                saveCourierInformation();
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

    private void getCourierInfo(){
        mCourierDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("courierName") != null) {
                        mCourierName = map.get("courierName").toString();
                        mNameCourierField.setText(mCourierName);
                    }

                    if(map.get("courierPhone") != null) {
                        mCourierPhone = map.get("courierPhone").toString();
                        mPhoneCourierField.setText(mCourierPhone);
                    }

                    if(map.get("courierImageUrl") != null){
                        courierImageUrl = map.get("courierImageUrl").toString();
                        Glide.with(getApplication()).load(courierImageUrl).into(mCourierImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveCourierInformation() {
        mCourierName = mNameCourierField.getText().toString();
        mCourierPhone = mPhoneCourierField.getText().toString();

        Map courierInfo = new HashMap();
        courierInfo.put("courierName", mCourierName);
        courierInfo.put("courierPhone", mCourierPhone);
        mCourierDatabase.updateChildren(courierInfo);

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


                    newImage.put("courierImageUrl", url.toString());
                    mCourierDatabase.updateChildren(newImage);

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
        if (requestCode == 3 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mCourierImage.setImageURI(resultUri);
        }

    }
}