package com.example.emotionscope;

/**
 * Représente un utilisateur de l'application Emotion Scope.
 * 
 * Un utilisateur est identifié de manière unique par son UID et possède des
 * informations personnelles telles que le nom, prénom, email, mot de passe,
 * ainsi que la date de création de son compte.
 * 
 * Cette classe est principalement utilisée pour la gestion des données
 * utilisateur lors de l'authentification et de la personnalisation de l'expérience.
 * 
 * @author Chaker Zakaria
 */


public class Utilisateur {

    /** Identifiant unique de l'utilisateur. */
    public String uid;

    /** Nom de famille de l'utilisateur. */
    public String nom;
    
    /** Prénom de l'utilisateur. */
    public String prenom;
    
    /** Adresse e-mail de l'utilisateur. */
    public String email;
    
    /** Mot de passe de l'utilisateur. */
    public String motDePasse;
    
    /** Date de création du compte utilisateur. */
    public String dateCreation;

    public Utilisateur(){}

    
    /**
     * Constructeur permettant d’instancier un utilisateur avec toutes ses informations.
     *
     * @param uid L'identifiant unique de l'utilisateur.
     * @param nom Le nom de famille de l'utilisateur.
     * @param prenom Le prénom de l'utilisateur.
     * @param email L'adresse e-mail de l'utilisateur.
     * @param motDePasse Le mot de passe de l'utilisateur.
     * @param dateCreation La date de création du compte.
     */

    public Utilisateur(String uid, String nom, String prenom, String email, String motDePasse, String dateCreation) {
        this.uid = uid;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.dateCreation = dateCreation;
    }

    /**
     * Récupère la date de création du compte utilisateur.
     *
     * @return La date de création sous forme de chaîne.
     */

    public String getDateCreation() {
        return dateCreation;
    }

    /**
     * Définit la date de création du compte utilisateur.
     *
     * @param dateCreation La nouvelle date de création.
     */

    public void setDateCreation(String dateCreation) {
        this.dateCreation = dateCreation;
    }
}
