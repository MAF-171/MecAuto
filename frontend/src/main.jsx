import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { AuthProvider } from './lib/auth.jsx';
import { AvisosProvider } from './components/ui.jsx';
import './styles.css';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <AvisosProvider>
      <AuthProvider>
        <App />
      </AuthProvider>
    </AvisosProvider>
  </StrictMode>
);
