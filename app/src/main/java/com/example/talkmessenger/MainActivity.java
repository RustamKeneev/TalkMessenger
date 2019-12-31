package com.example.talkmessenger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recycler_view_messages;
    private ImageView imageView;
    private ImageView imageView_send;
    private EditText editText_message;
    private MessagesAdapter messagesAdapter;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private static final  int RC_SIGN_IN = 100;
    private static final  int RC_GET_IMAGE = 101;

    private SharedPreferences sharedPreferences;
    private static final String TAG = "ololo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firestore = FirebaseFirestore.getInstance();
        initAndBuildViews();
    }

    private void initAndBuildViews() {
        imageView = findViewById(R.id.imageView);
        imageView_send = findViewById(R.id.imageView_send);
        editText_message = findViewById(R.id.editText_message);
        recycler_view_messages = findViewById(R.id.recycler_view_messages);

        firebaseAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);


        messagesAdapter = new MessagesAdapter(this);
        recycler_view_messages.setHasFixedSize(true);
        recycler_view_messages.setLayoutManager(new LinearLayoutManager(this));
        recycler_view_messages.setAdapter(messagesAdapter);


        imageView_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(editText_message.getText().toString().trim(),null);
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
                startActivityForResult(intent,RC_GET_IMAGE);
            }
        });

        if (firebaseAuth.getCurrentUser() !=null){
            sharedPreferences.edit().putString("author",firebaseAuth.getCurrentUser().getEmail()).apply();
        }else {
            Toast.makeText(this, "Not register", Toast.LENGTH_SHORT).show();
            signOut();
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sign_up){
            firebaseAuth.signOut();
            signOut();
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendMessage(String textOfMessage, String urlToImage ){
//        String textOfMessage = editText_message.getText().toString().trim();
        Message message = null;
        String author = sharedPreferences.getString("author","anonim");
        if (textOfMessage !=null && !textOfMessage.isEmpty()){
            message = new Message(author,textOfMessage,System.currentTimeMillis(),null);

        }else if (urlToImage !=null && !urlToImage.isEmpty()) {
            message = new Message(author, null, System.currentTimeMillis(), urlToImage);
        }
        if (message != null) {
            firestore.collection("messages").add(message)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            editText_message.setText("");
                            recycler_view_messages.scrollToPosition(messagesAdapter.getItemCount() - 1);
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        firestore.collection("messages").orderBy("date").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (queryDocumentSnapshots != null) {
                    List<Message> messages = queryDocumentSnapshots.toObjects(Message.class);
                    messagesAdapter.setMessages(messages);
                    recycler_view_messages.scrollToPosition(messagesAdapter.getItemCount() - 1);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GET_IMAGE && resultCode == RESULT_OK){
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    final StorageReference storageReference = storageRef.child("images/" + uri.getPathSegments());
                    storageReference.putFile(uri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task){
                            if (!task.isSuccessful()) {
                                
                            }

                            // Continue with the task to get the download URL
                            return storageReference.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                Uri downloadUri = task.getResult();
                                if (downloadUri != null) {
                                    Log.i(TAG, "onComplete: " + downloadUri.toString());
                                    sendMessage(null,downloadUri.toString());
                                }
                            } else {
                                // Handle failures
                                // ...
                            }
                        }
                    });
                }
            }
        }
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    sharedPreferences.edit().putString("author",user.getEmail()).apply();
                }
            } else {
                if (response != null) {
                    Toast.makeText(this, "Error" +response.getError(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void signOut(){
        AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build());

                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(providers)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        });
    }
}
