package com.example.emotionscope;

import java.util.Objects;

/**
 * Modèle de données représentant les détails d’un historique d’analyse
 * pour un jour ou une semaine donné.
 * Il indique la présence ou non de chaque type d’analyse.
 * 
 * @author CHAKER Zakaria
 */

public class DetailsHistorique {


    public String jourOuSemaine;

    public boolean hasFaciale;

    public boolean hasVocale;

    public boolean hasCognitive;

    public boolean hasGlobale;



    /**
     * Constructeur par défaut requis pour Firebase ou la sérialisation.
     */

    public DetailsHistorique(){}

    /**
     * Constructeur avec paramètres pour initialiser les champs.
     *
     * @param jourOuSemaine la période concernée (jour ou semaine)
     * @param hasFaciale    présence d’analyse faciale
     * @param hasCognitive  présence d’analyse cognitive
     * @param hasVocale     présence d’analyse vocale
     * @param hasGlobale    présence d’analyse globale
     */

    public DetailsHistorique(String jourOuSemaine, boolean hasFaciale, boolean hasCognitive, boolean hasVocale, boolean hasGlobale)
    {
        this.jourOuSemaine = jourOuSemaine;
        this.hasFaciale = hasFaciale;
        this.hasCognitive = hasCognitive;
        this.hasGlobale = hasGlobale;
    }

    /**
     * Compare deux objets {@code DetailsHistorique} pour vérifier s’ils sont égaux.
     *
     * @param o l’objet à comparer
     * @return {@code true} si les deux objets sont égaux, sinon {@code false}
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DetailsHistorique)) return false;
        DetailsHistorique that = (DetailsHistorique) o;
        return hasFaciale == that.hasFaciale
                && hasVocale   == that.hasVocale
                && hasCognitive== that.hasCognitive
                && hasGlobale  == that.hasGlobale
                && Objects.equals(jourOuSemaine, that.jourOuSemaine);
    }

    /**
     * Calcule le code de hachage de cet objet pour les structures de type HashMap ou HashSet.
     *
     * @return le code de hachage de l’objet
     */

    @Override
    public int hashCode() {
        return Objects.hash(jourOuSemaine, hasFaciale, hasVocale, hasCognitive, hasGlobale);
    }
}
