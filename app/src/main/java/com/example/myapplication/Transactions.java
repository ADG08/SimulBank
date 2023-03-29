package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class Transactions extends AppCompatActivity {
    FirebaseAuth auth;
    FirebaseFirestore db;
    FirebaseUser user;
    ListView ls;
    TextView aucune_transaction;

    Button retour;

    private HashMap<String, String> map;
    private ArrayList<HashMap<String, String>> values = new ArrayList<HashMap<String, String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);
        ls = findViewById(R.id.lst);
        retour = findViewById(R.id.retour);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        aucune_transaction = findViewById(R.id.aucune_transaction);




        user = auth.getCurrentUser();

        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        } else {


            db.collection("Transaction")
                    .whereEqualTo("Id", auth.getCurrentUser().getUid())
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (task.getResult().size() == 0)
                                    aucune_transaction.setText("Aucune opération effectuée");
                                else
                                    aucune_transaction.setVisibility(View.GONE);
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    map = new HashMap<String, String>();
                                    map.put("Type", "Type : " + document.get("Type").toString());
                                    map.put("Montant", "Montant : " + document.get("Montant").toString());
                                    map.put("Objet", "Objet : " + document.get("Objet").toString());
                                    map.put("Date", "Date de transaction : " +  document.get("Date").toString());

                                    values.add(map);

                                    SimpleAdapter adapter = new SimpleAdapter(Transactions.this, values, R.layout.item,
                                            new String[]{"Type", "Montant", "Date", "Objet"},
                                            new int[]{R.id.type, R.id.montant, R.id.date, R.id.objet}
                                    );
                                    ls.setAdapter(adapter);
                                }
                            }
                        }
                    });


        }

        retour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }
}