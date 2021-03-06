package com.example.hp.blogapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class AccounrSetup extends AppCompatActivity {

    private Toolbar toolbarSetup;
    private ProgressBar progressBar;
    private CircleImageView userImg;
    private Uri main_uri = null;
    private Uri default_uri = null;
    private FirebaseAuth mAuth;
    private boolean isChanged = true;
    private FirebaseFirestore fireStore;
    private EditText Name, Contact, Address, Intro, EmailID_text;
    private StorageReference mStorageRef;
    Button Submit;
    private String user_id;
    private Bitmap compressedImageBitmap;


    DatabaseReference databaseReference;
    FirebaseDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounr_setup);

        //Toolbar
        toolbarSetup = findViewById(R.id.toolbarSetup);
        setSupportActionBar(toolbarSetup);
        getSupportActionBar().setTitle("Account Setup");

        Contact = findViewById(R.id.conatct);
        Address = findViewById(R.id.address);
        Intro = findViewById(R.id.intro);
        EmailID_text = findViewById(R.id.emailID);

        mAuth = FirebaseAuth.getInstance();
        EmailID_text.setText(mAuth.getCurrentUser().getEmail());

        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("Users");

        Submit = findViewById(R.id.submit);
        progressBar = findViewById(R.id.AccountSettingsBar);
        user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        Name = findViewById(R.id.name);
        userImg = findViewById(R.id.profile);
        default_uri = Uri.parse("R.mipmap.user");

        databaseReference.child(mAuth.getCurrentUser().getUid() + "/profile_Details").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                final DatabaseReference databaseReference1 = databaseReference.child(mAuth.getCurrentUser().getUid() + "/profile_Details");
                DatabaseReference databaseReference2 = databaseReference1.child("uid");
                ValueEventListener eventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Log.e("Value",""+dataSnapshot.exists());
                        if (dataSnapshot.exists()) {
                            isChanged = false;
                            databaseReference1.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    AccountSetupModelClass accountSetupModelClass = snapshot.getValue(AccountSetupModelClass.class);
                                    String name = accountSetupModelClass.getName();
                                    Log.e("Step 2",""+name);
                                    String contact = accountSetupModelClass.getContact();
                                    String add = accountSetupModelClass.getAddress();
                                    String imgUri = accountSetupModelClass.getImageUri();
                                    String intro = accountSetupModelClass.getIntroduction();
                                    Toast.makeText(AccounrSetup.this, "DATA EXISTS", Toast.LENGTH_LONG).show();
                                    Name.setText(name);
                                    Contact.setText(contact);
                                    Address.setText(add);
                                    Intro.setText(intro);

                                    //GLIDE APP set default background
                                    RequestOptions placeHolder = new RequestOptions();
                                    placeHolder.placeholder(R.mipmap.user);

                                    //Convert image string to URI and store it in mainImageUri
                                    main_uri = Uri.parse(imgUri);

                                    Glide.with(AccounrSetup.this).setDefaultRequestOptions(placeHolder.placeholder(R.mipmap.user)).load(imgUri).into(userImg);

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("AccountSetup", databaseError.getMessage());
                    }
                };
                databaseReference2.addListenerForSingleValueEvent(eventListener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        Submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String uName = Name.getText().toString();
                final String contact = Contact.getText().toString();
                final String address = Address.getText().toString();
                final String Introduction = Intro.getText().toString();
                final String email = EmailID_text.getText().toString();
                if (isChanged) {
                    if (!TextUtils.isEmpty(uName) && main_uri != null) {
                        progressBar.setVisibility(View.VISIBLE);
                        // String user_id = mAuth.getCurrentUser().getUid();

                        File imageFile = new File(main_uri.getPath());

                        try {
                            compressedImageBitmap = new Compressor(AccounrSetup.this)
                                    .setMaxHeight(100)
                                    .setMaxWidth(100)
                                    .setQuality(10)
                                    .compressToBitmap(imageFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //Uploadinf bitmap to firebase
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        compressedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] thumbBitmap = baos.toByteArray();

                        UploadTask thumbImage = mStorageRef.child("/Profile_Photos/thumbs").child(user_id + ".jpg").putBytes(thumbBitmap);
                        thumbImage.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()) {
                                    saveToFire_base(uName, contact, address, Introduction, email);
                                } else {
                                    String error = task.getException().getMessage();
                                    Toast.makeText(AccounrSetup.this, " Image Error" + error, Toast.LENGTH_LONG).show();
                                }
                                progressBar.setVisibility(View.INVISIBLE);

                            }
                        });
//
                    } else {
                        Toast.makeText(AccounrSetup.this, "No image", Toast.LENGTH_LONG).show();
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                } else {
                    Toast.makeText(AccounrSetup.this, "Image not changed", Toast.LENGTH_LONG).show();
                    saveToFire_base(uName, contact, address, Introduction, email);
                }
            }

        });

        userImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //REad permisssion available in less tham Marhmallow..else include code to get the permsion

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    Toast.makeText(AccounrSetup.this, "ReadPerm", Toast.LENGTH_LONG).show();
                    if (ContextCompat.checkSelfPermission(AccounrSetup.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(AccounrSetup.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
//                        ActivityCompat.requestPermissions(AccounrSetup.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

                    }
                    Toast.makeText(AccounrSetup.this, "WritePerm", Toast.LENGTH_LONG).show();
                    if (ContextCompat.checkSelfPermission(AccounrSetup.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(AccounrSetup.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
                        ActivityCompat.requestPermissions(AccounrSetup.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);

                    }
                }
                getPicture();
            }

            private void getPicture() {
                Toast.makeText(AccounrSetup.this, "Successfully Saved", Toast.LENGTH_LONG).show();
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1, 1)
                        .start(AccounrSetup.this);
            }


        });
    }


    private void saveToFire_base(final String uName, final String con, final String add, final String intro, final String email) {


        //If task is not null that is change occured. So get new URI for image also change ThumbNail


        mStorageRef.child("/Profile_Photos/thumbs").child(user_id + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                final AccountSetupModelClass accountSetupModelClass = new AccountSetupModelClass(uName, con, add, intro, uri.toString(), email,mAuth.getUid());

                //Create map..with keys common and values
                databaseReference.child(user_id + "/profile_Details").setValue(accountSetupModelClass).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(AccounrSetup.this, "Settings Saved Successfully", Toast.LENGTH_LONG).show();
                            Intent main = new Intent(AccounrSetup.this, MainActivity.class);
                            startActivity(main);
                        } else {
                            String error = task.getException().getMessage();
                            Toast.makeText(AccounrSetup.this, " Firebase Error" + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                main_uri = result.getUri();
                userImg.setImageURI(main_uri);
                isChanged = true;
                String tct = main_uri.toString();


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();

            }
        }
    }


}




