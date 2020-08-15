package com.example.dons.upcourierv01;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
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

public class RegisterCourierActivity extends AppCompatActivity {

    //Declaración de variables
    private Button btnSignUpCourier;
    private Button btnLogInCourier;
    private EditText txtCourierEmail;
    private EditText txtCourierPassw;

    private FirebaseAuth authUser;
    private FirebaseAuth.AuthStateListener fBAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_courier);

        //Empezamos a trabajar con la base de datos, para ello obtenemos una instancia de esta
        authUser = FirebaseAuth.getInstance();
        fBAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //Obtenemos el nodo de user
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                //Evaluamos para el ingreso
                if(user != null){
                    Intent intent = new Intent(RegisterCourierActivity.this, MapsActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        };

        //Inicialización
        txtCourierEmail = (EditText) findViewById(R.id.SignUp_Courier_Email);
        txtCourierPassw = (EditText) findViewById(R.id.SignUp_Courier_Passw);
        btnLogInCourier = (Button) findViewById(R.id.btnCourierLogIn);
        btnSignUpCourier = (Button) findViewById(R.id.btnCourierSignUp);

        //Comportamiento para registrar un usuario
        btnSignUpCourier.setOnClickListener(new View.OnClickListener() {
            //Cargamos los datos para subir a la base de datos
            @Override
            public void onClick(View v) {
                final String email = txtCourierEmail.getText().toString();
                final String passw = txtCourierPassw.getText().toString();

                //Empieza el procedimiento para añadir dentro del apartado correspondiente en la base de datos
                authUser.createUserWithEmailAndPassword(email,passw).addOnCompleteListener(RegisterCourierActivity.this
                        , new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                //Manejo de error
                                if(!task.isSuccessful()){
                                    Toast.makeText(RegisterCourierActivity.this, "Ha ocurrido un error al registrarse", Toast.LENGTH_LONG).show();
                                }else{
                                    String user_id = authUser.getCurrentUser().getUid();
                                    //Añadimos el usuario al nodo correspondiente
                                    DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference()
                                            .child("Users").child("Couriers").child(user_id).child("name");
                                    //Debemos asegurar que el registro se ha añadido
                                    current_user_db.setValue(email);
                                }
                            }
                        });
            }
        });
        // Comportamiento para el login
        btnLogInCourier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = txtCourierEmail.getText().toString();
                final String passw = txtCourierPassw.getText().toString();

                authUser.signInWithEmailAndPassword(email,passw).addOnCompleteListener(RegisterCourierActivity.this
                        , new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                //Manejo de error
                                if(!task.isSuccessful()){
                                    Toast.makeText(RegisterCourierActivity.this
                                            , "Ha ocurrido un error al ingresar", Toast.LENGTH_LONG).show();
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
