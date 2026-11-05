import { createContext, useContext, useState } from 'react';
import { authService } from '../api/services';
import { clearSession, getStoredUser, getToken, setSession } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => (getToken() ? getStoredUser() : null));

  async function login(email, password) {
    const res = await authService.login(email, password);
    setSession(res.token, res.user);
    setUser(res.user);
    return res.user;
  }
  async function register(name, email, password) {
    const res = await authService.register(name, email, password);
    setSession(res.token, res.user);
    setUser(res.user);
    return res.user;
  }
  function logout() {
    clearSession();
    setUser(null);
  }
  return <AuthContext.Provider value={{ user, login, register, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth() { return useContext(AuthContext); }
