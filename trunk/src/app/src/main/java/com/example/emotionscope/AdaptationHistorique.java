package com.example.emotionscope;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adaptateur pour afficher l'historique des analyses dans une liste RecyclerView.
 * Il gère les données de type {@link DetailsHistorique} et affiche des icônes en fonction
 * des types d'analyses effectuées (faciale, vocale, cognitive, globale).
 */
public class AdaptationHistorique extends ListAdapter<DetailsHistorique, AdaptationHistorique.HistoryVH> {

    private final String uid, prenom, nom, email;
    private boolean estModeSemaine = true;

    // DiffCallback défini une seule fois
    private static final DiffUtil.ItemCallback<DetailsHistorique> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DetailsHistorique>() {
                @Override
                public boolean areItemsTheSame(@NonNull DetailsHistorique a, @NonNull DetailsHistorique b) {
                    return a.jourOuSemaine.equals(b.jourOuSemaine);
                }

                @Override
                public boolean areContentsTheSame(@NonNull DetailsHistorique a, @NonNull DetailsHistorique b) {
                    return a.equals(b);
                }
            };

    /**
     * Constructeur de l'adaptateur.
     *
     * @param uid    Identifiant utilisateur.
     * @param prenom Prénom de l'utilisateur.
     * @param nom    Nom de l'utilisateur.
     * @param email  Email de l'utilisateur.
     */
    public AdaptationHistorique(String uid, String prenom, String nom, String email) {
        super(DIFF_CALLBACK);
        this.uid = uid;
        this.prenom = prenom;
        this.nom    = nom;
        this.email  = email;
    }

    /**
     * Crée une nouvelle instance de ViewHolder.
     *
     * @param parent   Le ViewGroup parent dans lequel la vue sera attachée.
     * @param viewType Le type de vue (non utilisé ici car il n'y a qu'un type).
     * @return Une instance de {@link HistoryVH}.
     */

    @NonNull
    @Override
    public HistoryVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.historique_jours, parent, false);
        return new HistoryVH(v);
    }


    
    /**
     * Lie les données d’un élément {@link DetailsHistorique} à un ViewHolder.
     *
     * @param vh  Le ViewHolder.
     * @param pos La position de l’élément dans la liste.
     */

    @Override
    public void onBindViewHolder(@NonNull HistoryVH vh, int pos) {
        DetailsHistorique it = getItem(pos);
        vh.dateView.setText(it.jourOuSemaine);
        vh.facialIcon.setVisibility(it.hasFaciale ? VISIBLE : GONE);
        vh.vocaleIcon.setVisibility(it.hasVocale ? VISIBLE : GONE);
        vh.cognitiveIcon.setVisibility(it.hasCognitive ? VISIBLE : GONE);
        vh.globaleIcon.setVisibility(it.hasGlobale ? VISIBLE : GONE);
        vh.itemView.setClickable(!estModeSemaine);
        vh.imageFleche.setVisibility(GONE);
        if (!estModeSemaine) {
            vh.imageFleche.setVisibility(VISIBLE);
            vh.itemView.setOnClickListener(v -> {
                Context ctx = v.getContext();
                Intent intent = new Intent(ctx, DetailsAnalysesHistorique.class);
                intent.putExtra("date", it.jourOuSemaine);
                intent.putExtra("uid",    uid);
                intent.putExtra("nom",    nom);
                intent.putExtra("prenom", prenom);
                intent.putExtra("email",  email);
                ctx.startActivity(intent);
            });
        } else {
            vh.itemView.setOnClickListener(null);
        }
    }

    /**
     * ViewHolder représentant un élément de l'historique.
     * Il contient une date et des icônes pour chaque type d’analyse.
     */

    static class HistoryVH extends RecyclerView.ViewHolder {
        TextView dateView;
        ImageView facialIcon, vocaleIcon, cognitiveIcon, globaleIcon, imageFleche;

        /**
         * Constructeur du ViewHolder.
         *
         * @param itemView Vue représentant une ligne de l'historique.
         */

        HistoryVH(View itemView) {
            super(itemView);
            dateView = itemView.findViewById(R.id.textViewDate);
            facialIcon = itemView.findViewById(R.id.imageFaciale);
            vocaleIcon = itemView.findViewById(R.id.imageVocale);
            cognitiveIcon = itemView.findViewById(R.id.imageCognitive);
            globaleIcon = itemView.findViewById(R.id.imageAnalyseGlobale);
            imageFleche = itemView.findViewById(R.id.imageFleche);
        }
    }

    @Override
    public int getItemCount() {
        return super.getItemCount();
    }

    public void setModeSemaine(boolean estModeSemaine) {
        this.estModeSemaine = estModeSemaine;
        notifyDataSetChanged();
    }
}