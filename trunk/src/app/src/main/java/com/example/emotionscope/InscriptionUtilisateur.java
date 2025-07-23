package com.example.emotionscope;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.mindrot.jbcrypt.BCrypt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Classe permettant de créer un compte Utilisateur
 * Cette classe permet de:
 * - Créer un compte et d'y etre connecté immediatement
 * - Verifier si le compte exite ou pas
 * - Verifier la cohérence du compté créé
 * - ...
 *
 * Pas de FireBase Auth dans notre cas!
 * @author: Chaker Zakaria
 */
public class InscriptionUtilisateur extends AppCompatActivity {
    private static final String CLE_UID = "uid";

    private EditText saisieNom, saisiePrenom, saisieMail, saisieMotDePasse, saisieConfirmerMotDePasse;
    private Button btnInscriptionFinale;
    private TextView  tvMessageErreur;
    private LinearLayout textViewInformationInscription;

    FirebaseDatabase database = FirebaseDatabase.getInstance("https://emotionscope-477b0-default-rtdb.europe-west1.firebasedatabase.app");
    DatabaseReference utilisateursRef = database.getReference("utilisateurs");

    private String uidGenere;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inscription_utilisateur_activity);

        saisieNom = findViewById(R.id.saisieNom);
        saisiePrenom = findViewById(R.id.saisiePrenom);
        saisieMail = findViewById(R.id.saisieMail);
        saisieMotDePasse = findViewById(R.id.saisieMotDePasse);
        saisieConfirmerMotDePasse = findViewById(R.id.saisieConfirmerMotDePasse);
        btnInscriptionFinale = findViewById(R.id.btnInscriptionFinale);
        textViewInformationInscription = findViewById(R.id.textViewInformationInscription);
        tvMessageErreur = findViewById(R.id.tvMessageErreur);

        ImageView gifImageView = findViewById(R.id.imgPlanetes);

        Glide.with(this)
                .asGif()
                .load(R.drawable.planetes_gif)
                .into(gifImageView);

        btnInscriptionFinale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String nom = saisieNom.getText().toString().trim();
                String prenom = saisiePrenom.getText().toString().trim();
                String eMail = saisieMail.getText().toString().trim();
                String motDePasse = saisieMotDePasse.getText().toString();
                String confirmationMotDePasse = saisieConfirmerMotDePasse.getText().toString();

                /**
                 * Phase de vérification
                 * - 1ere vérification: champs vides
                 * - 2eme verification: mot de passe différents
                 * - 3eme verification: email non conforme
                 *
                 */

                textViewInformationInscription.setVisibility(View.GONE);
                if (nom.isEmpty() || prenom.isEmpty() || eMail.isEmpty() || motDePasse.isEmpty() || confirmationMotDePasse.isEmpty()) {
                    afficherMessageErreur("Tous les champs doivent être remplis");
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(eMail).matches()) {
                    afficherMessageErreur("L'adresse e-mail n'est pas valide");
                    return;
                }

                if (!motDePasse.equals(confirmationMotDePasse)) {
                    afficherMessageErreur("Les mots de passe ne correspondent pas");
                    return;
                }

                if (motDePasse.length() < 8) {
                    afficherMessageErreur("Le mot de passe doit contenir au moins 8 caractères.");
                    return;
                }
                if (!motDePasse.matches(".*[A-Z].*")) {
                    afficherMessageErreur("Le mot de passe doit contenir au moins une majuscule.");
                    return;
                }
                if (!motDePasse.matches(".*\\d.*")) {
                    afficherMessageErreur("Le mot de passe doit contenir au moins un chiffre.");
                    return;
                }
                if (!motDePasse.matches(".*[!@#$%^&*()\\-+=<>?].*")) {
                    afficherMessageErreur("Le mot de passe doit contenir au moins un caractère spécial.");
                    return;
                }

                //Verification si connexion internet etablie. Sinon a faire sur thread courant.
                Log.d("Inscription", "Conditions validées, enregistrement en cours...");

                verifierEtEnregistrerUtilisateur(nom, prenom, eMail, motDePasse);

                //Log pour verification si ecriture dans BD
            }
        });
    }

    /**
     * Méthode permet de sauvegarder dans la Base de données FireBase le nom d'utilisateur avec les parametres ci dessous.
     * Mais elle permet aussi de verfier si l'utilsiateur n'existe pas deja dans la BD.
     * Cette méthode permet d'attibuer un UID personnel par user.
     *
     * @param nom
     * @param prenom
     * @param eMail
     * @param motDePasse
     * @author: Chaker Zakaria
     */

    private void verifierEtEnregistrerUtilisateur(String nom, String prenom, String eMail, String motDePasse) {
        utilisateursRef.orderByChild("email").equalTo(eMail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    afficherMessageErreur(
                            "Cet email est déjà utilisé. Veuillez en essayer un autre."
                    );
                } else {

                     uidGenere = utilisateursRef.push().getKey();
                    if (uidGenere == null) {
                        afficherMessageErreur("Impossible de générer un identifiant.");
                        return;
                    }


                    String hashedPass = hacherMotDePasse(motDePasse);
                    String dateCreation = obtenirDateActuelle();
                    Utilisateur nouvelUtilisateur =
                            new Utilisateur(uidGenere, nom, prenom, eMail, hashedPass, dateCreation);

                    utilisateursRef.child(uidGenere)
                            .setValue(nouvelUtilisateur)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("Firebase", "Utilisateur enregistré avec succès.");
                                    afficherBoiteDeDialogueBienvenue(uidGenere, prenom.trim());
                                } else {
                                    Log.e("Firebase",
                                            "Erreur lors de l'enregistrement : " +
                                                    task.getException().getMessage()
                                    );
                                    afficherMessageErreur(
                                            "Erreur lors de l'inscription : " +
                                                    task.getException().getMessage()
                                    );
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                afficherMessageErreur("Erreur réseau : " + error.getMessage());
            }
        });
    }

    /**
     * Méthode qui permet de réaliser unn cryptage d'un mot de pass saisir par l'utilisateur.
     * Cette méthode utilise la bibliothèque BCrypt.
     *
     * @param motDePasse
     * @return mot de passe crypté
     * @author: Chaker Zakaria
     */

    private String hacherMotDePasse(String motDePasse) {
        return BCrypt.hashpw(motDePasse, BCrypt.gensalt());
    }

    /**
     * Méthode qui permet de faire un affichage sur la nature de l'erreur rencontrée:
     * - Erreur réseau
     * - Erreur ecriture BD
     * - Erruer connexion a la BD
     * - ..
     *
     * @param message
     * @author: Chaker Zakaria
     */

    private void afficherMessageErreur(String message) {
        textViewInformationInscription.setVisibility(View.VISIBLE);
        tvMessageErreur.setText(message);
    }

    /**
     * Méthode qui permet de retourner à la page de Connexion (boite de dialogue du mainActivity)
     * On ajoute un parametre permettant de reconnaitre ce retour de connexion.
     *
     * @param view
     * @author: Chaker Zakaria
     */
    public void retourConnexion(View view) {
        Intent retourIntent = new Intent();
        retourIntent.putExtra("depuis_inscription", true);
        setResult(RESULT_OK, retourIntent);
        finish();
    }

    /**
     * Méthode qui permet de faire l'affichage à l'utilisateur que tout s'est bien passé.
     * Affichage d'une petite animation...
     *
     * @param prenom
     * @author: Chaker Zakaria
     */

    private void afficherBoiteDeDialogueBienvenue(String uid, String prenom) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        View dialogView = inflater.inflate(R.layout.boite_dialogue_bienvenue, null);
        builder.setView(dialogView);

        TextView texteBienvenue = dialogView.findViewById(R.id.texteBienvenue);
        ImageView gifFusee = dialogView.findViewById(R.id.gifFusee);
        Button boutonOk = dialogView.findViewById(R.id.boutonOk);

        texteBienvenue.setText("Bienvenue à toi " + prenom + "\nTu es à présent connecté à EmotionScope !");
        Glide.with(this).load(R.drawable.fusee).into(gifFusee);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        boutonOk.setOnClickListener(v -> {
            alertDialog.dismiss();
            redirigerVersMenu(uid, prenom, saisieNom.getText().toString().trim(), saisieMail.getText().toString().trim());
        });
    }

    /**
     * Méthode qui permet d'envoyer à l'instance de MainAcitivity les données de l'user.
     * Permet aussi de connatire l'etat de la connexion.
     *
     * @param prenom
     */
    private void redirigerVersMenu(String uid, String prenom, String nom, String adresseMail) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(CLE_UID, uid);
        intent.putExtra("prenom", prenom);
        intent.putExtra("nom", nom);
        intent.putExtra("email", adresseMail);
        intent.putExtra("estConnecte", true);
        startActivity(intent);
        finish();
    }

    private String obtenirDateActuelle() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}


