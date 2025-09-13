// src/lib/oidc.ts
export const issuer = process.env.KEYCLOAK_ISSUER!;
export const clientId = process.env.KEYCLOAK_CLIENT_ID!;
export const appBase = process.env.APP_BASE_URL!;
export const redirectUri = `${appBase}/api/auth/callback`;

export const endpoints = {
  auth: `${issuer}/protocol/openid-connect/auth`,
  token: `${issuer}/protocol/openid-connect/token`,
  logout: `${issuer}/protocol/openid-connect/logout`,
  userinfo: `${issuer}/protocol/openid-connect/userinfo`,
};

export async function sha256(base: string) {
  const data = new TextEncoder().encode(base);
  const hashBuffer = await crypto.subtle.digest("SHA-256", data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashBase64 = btoa(String.fromCharCode(...hashArray))
    .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  return hashBase64;
}
export function randomString(length = 64) {
  const charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
  let result = "";
  const randomValues = new Uint8Array(length);
  crypto.getRandomValues(randomValues);
  randomValues.forEach(v => (result += charset[v % charset.length]));
  return result;
}
export async function createPkce() {
  const verifier = randomString(64);
  const challenge = await sha256(verifier);
  return { verifier, challenge };
}
