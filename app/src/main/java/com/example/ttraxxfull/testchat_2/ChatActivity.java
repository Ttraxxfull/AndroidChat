package com.example.ttraxxfull.testchat_2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.ttraxxfull.testchat_2.Adapters.ChatAdapter;
import com.example.ttraxxfull.testchat_2.Entities.Message;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.List;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {


    private static final String TAG = "CHAT";
    private static final int SELECT_PHOTO = 1;
    private EditText etMessage;
    private ImageButton imageButton;
    private RecyclerView recycler;
    private ProgressBar imageLoader;

    private FirebaseAuth mAuth;
    private DatabaseReference mRef;
    private StorageReference storageReference;
    private FirebaseAuth.AuthStateListener authStateListener;
    private ChildEventListener childEventListener;

    private UploadTask uploadTask;

    private SharedPreferences prefs;
    private String username;
    private String userId;

    private ChatAdapter adapter;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

//Initialisation de la toolbar

        android.support.v7.widget.Toolbar toolbar;
        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Time Flies");
        setSupportActionBar(toolbar);

//Initialisation des vues
        initViews();
        initFirebase();

        prefs = getSharedPreferences("chat", MODE_PRIVATE);

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    attachChildListener();
                    username = prefs.getString("PSEUDO", null);
                    userId = user.getUid();
                    adapter.setUser(user);

                } else {
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                    finish();
                }

            }
        };

    }

    private void attachChildListener() {
        if (childEventListener == null) {
            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                    Log.w(TAG, "onChildAdded");
                    Message message = dataSnapshot.getValue(Message.class);
                    assert message != null;
                    message.setUid(dataSnapshot.getKey());
                    adapter.addMessage(message);
                    recycler.scrollToPosition(adapter.getItemCount() - 1);
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    Message message = dataSnapshot.getValue(Message.class);
                    assert message != null;
                    message.setUid(dataSnapshot.getKey());
                    adapter.deleteMessage(message);
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            };
            mRef.child(Constants.MESSAGES_DB).limitToLast(100).addChildEventListener(childEventListener);
        }
    }

    private void detachChildListener() {
        if (childEventListener != null) {
            mRef.child(Constants.MESSAGES_DB).removeEventListener(childEventListener);
            childEventListener = null;
        }
    }

    private void initViews() {
        etMessage = findViewById(R.id.etMessage);
        ImageButton sendButton = findViewById(R.id.sendButton);
        imageButton = findViewById(R.id.imageButton);
        recycler = findViewById(R.id.recycler);
        imageLoader = findViewById(R.id.imageLoader);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        recycler.setLayoutManager(manager);
        adapter = new ChatAdapter();
        recycler.setAdapter(adapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMesssage(null);
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickImage();
            }
        });

    }

    private void pickImage() {
        Intent picker = new Intent(Intent.ACTION_GET_CONTENT);
        picker.setType("image/jpeg");
        picker.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(picker, "Sélectionner une image"), SELECT_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PHOTO && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            assert imageUri != null;
            Log.w(TAG, "onActivityResult: " + imageUri.toString());
            uploadImage(imageUri);

        }
    }

    private void uploadImage(Uri imageUri) {
        uploadTask = storageReference.child(UUID.randomUUID() + ".jpg").putFile(imageUri);
        imageLoader.setVisibility(View.VISIBLE);
        imageButton.setEnabled(false);
        addUploadListener(uploadTask);
    }

    private void addUploadListener(UploadTask task) {
        OnCompleteListener<UploadTask.TaskSnapshot> completeListener = new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    UploadTask.TaskSnapshot imageUrl = task.getResult();
                    if (imageUrl != null) {
                        sendMesssage(imageUrl.toString());
                    }
                } else {
                    Toast.makeText(ChatActivity.this, "Impossible d'envoyer l'image", Toast.LENGTH_SHORT).show();

                }
                imageLoader.setVisibility(View.GONE);
                imageButton.setEnabled(true);
            }
        };
        OnProgressListener<UploadTask.TaskSnapshot> onProgressListener = new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double percent = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                imageLoader.setProgress((int) percent);
            }
        };
        task.addOnCompleteListener(this, completeListener).addOnCompleteListener(this, (OnCompleteListener<UploadTask.TaskSnapshot>) onProgressListener);
    }

    private void sendMesssage(String imageUrl) {
        Message message = null;
        if (imageUrl == null) {
            String content = etMessage.getText().toString();
            if (!TextUtils.isEmpty(content)) {
                message = new Message(username, userId, content, null);

            }
        } else {
            message = new Message(username, userId, null, imageUrl);
        }
        mRef.child(Constants.MESSAGES_DB).push().setValue(message);

        etMessage.setText("");

    }


    private void initFirebase() {

        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        mRef = mDatabase.getReference();
        FirebaseStorage mStorage = FirebaseStorage.getInstance();
        storageReference = mStorage.getReferenceFromUrl(Constants.STORAGE_PATH).child(Constants.STORAGE_REF);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        if (storageReference != null) {
            outState.putString("storageReference", storageReference.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String ref = savedInstanceState.getString("storageReference");
        if (ref == null) {
            return;
        }

        storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(ref);
        List<UploadTask> tasks = storageReference.getActiveUploadTasks();
        if (tasks.size() > 0) {
            imageButton.setEnabled(false);
            imageLoader.setVisibility(View.VISIBLE);
            uploadTask = tasks.get(0);
            addUploadListener(uploadTask);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            clearOnLogout();
            startActivity(new Intent(getApplicationContext(),LoginActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private void clearOnLogout() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mRef.child(Constants.USERS_DB).child(user.getUid()).removeValue();
            mRef.child(Constants.USERNAMES_DB).child(username).removeValue();
            prefs.edit().remove("PSEUDO").apply();
            adapter.clearMessage();
            detachChildListener();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        imageLoader.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (authStateListener != null) {
            mAuth.removeAuthStateListener(authStateListener);
        }
        detachChildListener();
        adapter.clearMessage();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mAuth.addAuthStateListener(authStateListener);
    }

}
