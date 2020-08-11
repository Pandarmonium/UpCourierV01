package com.example.dons.upcourierv01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterClientActivity extends AppCompatActivity {

    //Declaración de variables
    private Button btnSignUpClient;
    private Button btnLogInClient;
    private EditText txtClientEmail;
    private EditText txtClientPassw;

    private FirebaseAuth authUser;
    private FirebaseAuth.AuthStateListener fBAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_client);

        //Empezamos a trabajar con la base de datos, para ello obtenemos una instancia de esta
        authUser = FirebaseAuth.getInstance();
        fBAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //Obtenemos el nodo de user
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                //Evaluamos para el ingreso
                if(user != null){
                    Intent intent = new Intent(RegisterClientActivity.this, ClientMapsActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        //Inicialización
        txtClientEmail = (EditText) findViewById(R.id.SignUp_Client_Email);
        txtClientPassw = (EditText) findViewById(R.id.SignUp_Client_Passw);
        btnLogInClient = (Button) findViewById(R.id.btnClientLogIn);
        btnSignUpClient = (Button) findViewById(R.id.btnClientSignUp);

        //Comportamiento para registrar un usuario
        btnSignUpClient.setOnClickListener(new View.OnClickListener() {
            //Cargamos los datos para subir a la base de datos
            @Override
            public void onClick(View v) {
                final String email = txtClientEmail.getText().toString();
                final String passw = txtClientPassw.getText().toString();

                //Empieza el procedimiento para añadir dentro del apartado correspondiente en la base de datos
                authUser.createUserWithEmailAndPassword(email,passw).addOnCompleteListener(RegisterClientActivity.this
                        , new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                //Manejo de error
                                if(!task.isSuccessful()){
                                    Toast.makeText(RegisterClientActivity.this, "Ha ocurrido un error al registrarse", Toast.LENGTH_LONG).show();
                                }else{
                                    String user_id = authUser.getCurrentUser().getUid();
                                    //Añadimos el usuario al nodo correspondiente
                                    DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference()
                                            .child("Users").child("Clients").child(user_id);
                                    //Debemos asegurar que el registro se ha añadido
                                    current_user_db.setValue(true);
                                    Toast.makeText(RegisterClientActivity.this, "Registro exitoso", Toast.LENGTH_LONG).show();
                                    Log.d("REGISTROCLIENTE","registro exitoso");
                                }
                            }
                        });
            }
        });

        // Comportamiento para el login
        btnLogInClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = txtClientEmail.getText().toString();
                final String passw = txtClientPassw.getText().toString();

                authUser.signInWithEmailAndPassword(email,passw).addOnCompleteListener(RegisterClientActivity.this
                        , new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //Manejo de error
                        if(!task.isSuccessful()){
                            Toast.makeText(RegisterClientActivity.this
                                    , "Ha ocurrido un error al ingresar", Toast.LENGTH_LONG).show();
                        }else {
                            Toast.makeText(RegisterClientActivity.this
                                    , "Ingreso exitoso", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

    }

    //Cuando la activity se inicia, tambien se iniciara el listener para la db
    @Override
    protected void onStart() {
        super.onStart();
        authUser.addAuthStateListener(fBAuthListener);
    }

    //Cuando la activity pasa a segundo plano, el listener se desactiva
    @Override
    protected void onStop() {
        super.onStop();
        authUser.removeAuthStateListener(fBAuthListener);
    }
}
