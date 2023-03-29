package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private User currentUser = new User();
    private FirebaseAuth auth;
        private Button disconect, crediter, debiter, seeTransactions;
    private TextView textViewDetails, textViewArgents;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private Object argent;

    private String statue;
    private AlertDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        crediter = findViewById(R.id.crediter);
        debiter = findViewById(R.id.debiter);
        seeTransactions = findViewById(R.id.seeTransactions);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        disconect = findViewById(R.id.logout);
        textViewDetails = findViewById(R.id.user_details);
        textViewArgents = findViewById(R.id.user_argent);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Saisie de données");

        View view = getLayoutInflater().inflate(R.layout.custom_dialog, null);
        EditText eObjet;
        EditText eMontant;
        eObjet = view.findViewById(R.id.objet);
        eMontant = view.findViewById(R.id.montant);
        Button valider = view.findViewById(R.id.valid);
        valider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(TextUtils.isEmpty(eMontant.getText().toString())){
                    Toast.makeText(MainActivity.this, "Saisissez un montant.",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(TextUtils.isEmpty(eObjet.getText().toString()) ){
                    Toast.makeText(MainActivity.this, "Saisissez un objet.",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(!eObjet.getText().toString().matches("^[a-zA-Z]*$") && eObjet.getTextSize() < 15){
                    Toast.makeText(MainActivity.this, "Saisissez un objet avec moins de 15 caractères",Toast.LENGTH_SHORT).show();
                    return;
                }

                if (Float.parseFloat(eMontant.getText().toString()) > 0 ) {
                    if (statue == "Crediter") {
                        float x = Float.parseFloat(eMontant.getText().toString()) + currentUser.getSolde();

                        argent = x;
                        setNewData();
                        setNewTransactions("Credit", Float.parseFloat(eMontant.getText().toString()), eObjet.getText().toString());
                        refreshData();
                    } else if (statue == "Debiter") {

                        float x = currentUser.getSolde() - Float.parseFloat(eMontant.getText().toString());
                        setNewTransactions("Debit", Float.parseFloat(eMontant.getText().toString()), eObjet.getText().toString());
                        argent = x;
                        setNewData();
                        refreshData();
                    }
                    eMontant.setText("");
                    dialog.dismiss();
                } else {
                    Toast.makeText(MainActivity.this, "Saisissez un montant correct.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setView(view);
        dialog = builder.create();
        crediter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
                statue = "Crediter";
            }
        });


        debiter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
                statue = "Debiter";
            }
        });


        user = auth.getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {
            refreshData();
        }

        disconect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        seeTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Transactions.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setNewData() {
        currentUser.setSolde(Float.parseFloat(argent.toString()));

        Map<String, Object> user = new HashMap<>();
        user.put("name", currentUser.getName());
        user.put("solde",String.format("%.02f", currentUser.getSolde()));
        user.put("email", currentUser.getEmail());

        db.collection("users").document(auth.getUid()).set(currentUser).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });
    }

    private void refreshData() {
        DocumentReference docRef = db.collection("users").document(user.getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        currentUser.setSolde(Float.parseFloat(document.getData().get("solde").toString()));
                        currentUser.setName(document.getData().get("name").toString());
                        currentUser.setEmail(document.getData().get("email").toString());

                        textViewDetails.setText("Bonjour, " + currentUser.getName());
                        textViewArgents.setText("Vous avez : " + String.format("%.02f", currentUser.getSolde()) + "€");
                    }
                }
            }
        });
    }

    private void setNewTransactions(String state, float montant, String objet) {

        Map<String, Object> transactions = new HashMap<>();
        transactions.put("Id", user.getUid());
        transactions.put("Type", state);
        transactions.put("Montant", String.format("%.02f", montant));
        transactions.put("Objet", objet);
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String strDate = dateFormat.format(date);
        transactions.put("Date", strDate);

        db.collection("Transaction").add(transactions).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
            }
        });
    }
}