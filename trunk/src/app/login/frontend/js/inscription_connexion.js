// Import Firebase modules
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.7.2/firebase-app.js";
import { getAuth, signInWithEmailAndPassword, createUserWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.7.2/firebase-auth.js";
import { getDatabase, ref, set } from "https://www.gstatic.com/firebasejs/10.7.2/firebase-database.js";

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

// Initialisation Firebase
const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getDatabase(app);

// Sélecteurs
const signUpForm = document.querySelector("#sign-up-form");
const signInForm = document.querySelector("#sign-in-form");
const errorMessage = document.querySelector("#error-message");

// Regex de validation
const nameRegex = /^[A-Za-zÀ-ÿ]+(?:[-'\s][A-Za-zÀ-ÿ]+)*$/;
const emailRegex = /^[\w.-]+@[\w.-]+\.\w+$/;
const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#;\/,])[A-Za-z\d@$!%*?&.#;\/,]{8,50}$/;

//Fonction d’échappement XSS
function escapeHtml(str) {
  return str.replace(/[&<>"']/g, match => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#039;"
  }[match]));
}

// Inscription
signUpForm.addEventListener("submit", (event) => {
  event.preventDefault();

  const lastname = escapeHtml(document.querySelector("#lastname").value.trim());
  const firstname = escapeHtml(document.querySelector("#firstname").value.trim());
  const email = escapeHtml(document.querySelector("#email").value.trim());
  const password = escapeHtml(document.querySelector("#password").value.trim());
  const confirmPassword = escapeHtml(document.querySelector("#confirm-password").value.trim());

  if (!lastname || !firstname || !email || !password || !confirmPassword) {
    errorMessage.textContent = "Tous les champs doivent être remplis.";
    return;
  }

  if (!nameRegex.test(lastname)) {
    errorMessage.textContent = "Le nom ne doit contenir que des lettres.";
    return;
  }

  if (!nameRegex.test(firstname)) {
    errorMessage.textContent = "Le prénom ne doit contenir que des lettres.";
    return;
  }

  if (!emailRegex.test(email)) {
    errorMessage.textContent = "Format d'email invalide.";
    return;
  }

  if (password !== confirmPassword) {
    errorMessage.textContent = "Les mots de passe ne correspondent pas.";
    return;
  }

  if (!passwordRegex.test(password)) {
    errorMessage.textContent = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial";
    return;
  }

  createUserWithEmailAndPassword(auth, email, password)
    .then((userCredential) => {
      const user = userCredential.user;
      return set(ref(db, "utilisateurs/" + user.uid), {
        uid: user.uid,
        email: email,
        firstname: firstname,
        lastname: lastname
      });
    })
    .then(() => {
      alert("Inscription réussie !");
      errorMessage.textContent = "";
      signUpForm.reset();
    })
    .catch((error) => {
      console.error("Erreur Firebase:", error);
      errorMessage.textContent = escapeHtml(error.message);
    });
});

//Connexion
signInForm.addEventListener("submit", (event) => {
  event.preventDefault();

  const email = escapeHtml(document.querySelector("#signin-email").value.trim());
  const password = escapeHtml(document.querySelector("#signin-password").value.trim());

  if (!email || !password) {
    alert("Veuillez remplir tous les champs.");
    return;
  }

  signInWithEmailAndPassword(auth, email, password)
  .then((userCredential) => {
    console.log("Connexion réussie:", userCredential.user);
    window.location.href = "../html/emotionscope.html";
  })
  .catch((error) => {
    console.error("Erreur de connexion:", error.message);
    alert("Erreur de connexion: " + error.message);
  });
});

//Animation des transitions
const sign_in_btn = document.querySelector("#sign-in-btn");
const sign_up_btn = document.querySelector("#sign-up-btn");
const container = document.querySelector(".container");
const sign_in_btn2 = document.querySelector("#sign-in-btn2");
const sign_up_btn2 = document.querySelector("#sign-up-btn2");

sign_up_btn.addEventListener("click", () => {
  container.classList.add("sign-up-mode");
});
sign_up_btn2.addEventListener("click", () => {
  container.classList.add("sign-up-mode2");
});
sign_in_btn.addEventListener("click", () => {
  container.classList.remove("sign-up-mode");
});
sign_in_btn2.addEventListener("click", () => {
  container.classList.remove("sign-up-mode2");
});

const params = new URLSearchParams(window.location.search);
if (params.get('mode') === 'sign-up') {
  container.classList.add("sign-up-mode");
  container.classList.remove("sign-up-mode2");
}


document.querySelectorAll(".toggle-password").forEach((eyeIcon) => {
  eyeIcon.addEventListener("click", () => {
    const targetId = eyeIcon.getAttribute("data-target");
    const input = document.getElementById(targetId);
    if (input.type === "password") {
      input.type = "text";
      eyeIcon.classList.remove("fa-eye");
      eyeIcon.classList.add("fa-eye-slash");
    } else {
      input.type = "password";
      eyeIcon.classList.remove("fa-eye-slash");
      eyeIcon.classList.add("fa-eye");
    }
  });
});

