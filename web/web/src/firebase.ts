import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getStorage } from "firebase/storage";
import { getFunctions } from "firebase/functions";

const firebaseConfig = {
  apiKey: "AIzaSyDlIpNGG-bXxwuZsl6P9PNhpa3dlK45cik",
  authDomain: "amatyma-e75a8.firebaseapp.com",
  projectId: "amatyma-e75a8",
  storageBucket: "amatyma-e75a8.firebasestorage.app",
  messagingSenderId: "306750530279",
  appId: "1:306750530279:web:7315831705255642d83842",
  measurementId: "G-SLFK1EPEGY"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const firestore = getFirestore(app);
export const storage = getStorage(app);
export const functions = getFunctions(app);
export default app;
