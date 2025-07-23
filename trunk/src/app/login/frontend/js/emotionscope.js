// Import Firebase App et Auth
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.7.2/firebase-app.js";
import { getAuth, signOut, onAuthStateChanged } from "https://www.gstatic.com/firebasejs/10.7.2/firebase-auth.js";

// Configuration Firebase
const firebaseConfig = {
  apiKey: "AIzaSyDoaqAM0GEKvApv715Y-smclpI-9u1MVPk",
  authDomain: "emotionscope-477b0.firebaseapp.com",
  databaseURL: "https://emotionscope-477b0-default-rtdb.europe-west1.firebasedatabase.app",
  projectId: "emotionscope-477b0",
  storageBucket: "emotionscope-477b0.appspot.com",
  messagingSenderId: "778324586002",
  appId: "1:778324586002:web:aaec5e492b8d55ebb172cb"
};

// Initialise Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);

// Vérifie si l'utilisateur est connecté
onAuthStateChanged(auth, (user) => {
  if (!user) {
    console.warn("Utilisateur non connecté, redirection...");
    window.location.href = "/inscription-connexion";
  }
});

// 🔁 Gestion du bouton de déconnexion
const logoutBtn = document.querySelector("#logout-btn");

if (logoutBtn) {
  logoutBtn.addEventListener("click", () => {
    console.log("Bouton déconnexion cliqué !");
    signOut(auth)
      .then(() => {
        console.log("Déconnexion réussie");
        window.location.href = "/inscription-connexion";
      })
      .catch((error) => {
        console.error("Erreur lors de la déconnexion:", error);
        alert("Erreur de déconnexion: " + error.message);
      });
  });
} else {
  console.error("Le bouton de déconnexion n'a pas été trouvé !");
}
