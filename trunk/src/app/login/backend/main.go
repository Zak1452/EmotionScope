package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"text/template"
	"time"
)

var sessions = map[string]bool{}

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	rootDir, err := os.Getwd()
	if err != nil {
		panic("Impossible de déterminer le répertoire courant")
	}

	frontDir := filepath.Join(rootDir, "frontend")
	htmlDir := filepath.Join(frontDir, "html")

	// Chargement des fichiers CSS, JS, et images
	http.Handle("/css/", http.StripPrefix("/css/", http.FileServer(http.Dir(filepath.Join(frontDir, "css")))))
	http.Handle("/js/", http.StripPrefix("/js/", http.FileServer(http.Dir(filepath.Join(frontDir, "js")))))
	http.Handle("/img/", http.StripPrefix("/img/", http.FileServer(http.Dir(filepath.Join(frontDir, "img")))))

	// Redirige vers inscription-connexion
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		http.Redirect(w, r, "/inscription-connexion", http.StatusSeeOther)
	})

	// Affiche la page d'inscription/connexion
	http.HandleFunc("/inscription-connexion", func(w http.ResponseWriter, r *http.Request) {
		tmpl, err := template.ParseFiles(filepath.Join(htmlDir, "inscription_connexion.html"))
		if err != nil {
			http.Error(w, "Erreur serveur", http.StatusInternalServerError)
			return
		}
		tmpl.Execute(w, nil)
	})

	//Gestion de la connexion et création de session
	http.HandleFunc("/login", func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodPost {
			var requestData struct {
				Uid string `json:"uid"`
			}

			err := json.NewDecoder(r.Body).Decode(&requestData)
			if err != nil || requestData.Uid == "" {
				http.Error(w, "UID non fourni", http.StatusBadRequest)
				return
			}

			sessionID := "session-" + generateUID()
			sessions[sessionID] = true

			//Création d'un cookie sécurisé
			expiration := time.Now().Add(24 * time.Hour)
			cookie := http.Cookie{
				Name:     "session",
				Value:    sessionID,
				Expires:  expiration,
				Path:     "/",
				HttpOnly: true,
				SameSite: http.SameSiteLaxMode,
				Secure:   false, 
			}

			http.SetCookie(w, &cookie)
			w.WriteHeader(http.StatusOK)
		} else {
			http.Redirect(w, r, "/inscription-connexion", http.StatusMethodNotAllowed)
		}
	})

	//Page principale après connexion, accessible depuis /emotionscope
	http.HandleFunc("/emotionscope", func(w http.ResponseWriter, r *http.Request) {
		cookie, err := r.Cookie("session")
		if err != nil || !sessions[cookie.Value] {
			fmt.Println("⚠️ Cookie non trouvé ou session invalide")
			http.Redirect(w, r, "/inscription-connexion", http.StatusSeeOther)
			return
		}

		//Header pour le mode standard et contenu HTML
		w.Header().Set("Content-Type", "text/html; charset=utf-8")
		http.ServeFile(w, r, filepath.Join(htmlDir, "emotionscope.html"))
	})

	//Logout (supprime le cookie et la session)
	http.HandleFunc("/logout", func(w http.ResponseWriter, r *http.Request) {
		cookie, _ := r.Cookie("session")
		if cookie != nil {
			delete(sessions, cookie.Value)

			//Supprime le cookie
			cookie.Value = ""
			cookie.Expires = time.Unix(0, 0)
			cookie.Path = "/"
			http.SetCookie(w, cookie)
		}
		http.Redirect(w, r, "/inscription-connexion", http.StatusSeeOther)
	})

	fmt.Println("Serveur lancé sur http://localhost:" + port)
	http.ListenAndServe(":"+port, nil)
}

//Génération d'un UID unique
func generateUID() string {
	return fmt.Sprintf("%d", time.Now().UnixNano())
}
