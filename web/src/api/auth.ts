// Signing in and out, and knowing who is signed in.
//
// Sign-in and sign-out are handled by Spring Security's filters, not by a controller, so they aren't in the
// API document and are called with plain fetch here. Everything else goes through the typed client.

import { useQuery } from '@tanstack/react-query';
import { api, csrfToken, unwrap } from './client';
import { ApiError, noConnection, toApiError } from './problem';
import type { components } from './schema';

export type Me = components['schemas']['Me'];
export type Role = Me['role'];

/**
 * The demo accounts behind "Try as customer" and "Try as agent" (DemoAccountSeeder.java). The password is
 * not a secret: anyone can read it here, and these accounts only ever see demo data.
 */
export const DEMO_PASSWORD = 'teduh-demo';
export const demoAccounts = {
  customer: { email: 'aisyah@example.com', name: 'Aisyah Rahman' },
  agent: { email: 'farid@example.com', name: 'Farid Hassan' },
} as const;
export type DemoRole = keyof typeof demoAccounts;

export const meQueryKey = ['me'] as const;

/** Who is signed in, or null when nobody is. A 401 here is an answer ("signed out"), not an error. */
export async function fetchMe(): Promise<Me | null> {
  try {
    return await unwrap(api.GET('/api/auth/me'));
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

/** The signed-in person, cached for the whole visit. It only changes when someone signs in or out. */
export function useMe() {
  return useQuery({ queryKey: meQueryKey, queryFn: fetchMe, staleTime: Infinity });
}

/** Posts to Spring Security's form login, with the CSRF token like any other change. */
async function postForm(path: string, fields?: Record<string, string>): Promise<void> {
  const token = await csrfToken();
  let response: Response;
  try {
    response = await fetch(path, {
      method: 'POST',
      headers: token ? { 'X-XSRF-TOKEN': token } : {},
      // URLSearchParams is sent as an ordinary form (application/x-www-form-urlencoded).
      body: fields ? new URLSearchParams(fields) : undefined,
      credentials: 'same-origin',
    });
  } catch {
    throw noConnection();
  }
  if (!response.ok) {
    throw toApiError(response.status, await response.json().catch(() => undefined));
  }
}

/** Starts a session. A wrong email or password throws an ApiError with "That email and password don't match." */
export function signIn(email: string, password: string): Promise<void> {
  return postForm('/api/auth/login', { email, password });
}

/**
 * "Try as a new customer": the server makes a fresh demo customer with no policy and signs them in. Nobody
 * types a name, email or password, so no real personal details reach the demo.
 */
export function startAsNewCustomer(): Promise<void> {
  return postForm('/api/auth/new-customer');
}

export function signOut(): Promise<void> {
  return postForm('/api/auth/logout');
}

/** The name every "Try as a new customer" account gets (NewDemoCustomers.java). */
export const NEW_CUSTOMER_NAME = 'Demo traveller';

/** "Aisyah Rahman" → "Aisyah", for "Hi, Aisyah". A new demo customer keeps both words: "Hi, Demo" reads oddly. */
export function greetingName(displayName: string): string {
  return displayName === NEW_CUSTOMER_NAME ? displayName : displayName.split(' ')[0];
}

/** Where each role starts. */
export function homeFor(role: Role): string {
  return role === 'AGENT' ? '/agent/queue' : '/';
}

/**
 * Where to go after signing in: back to the page that sent the person to sign in, if it belongs to their
 * role (a customer's link into the agent area is no use to them), otherwise their start page.
 */
export function landingAfterSignIn(role: Role, from?: string): string {
  if (!from || from === '/sign-in') {
    return homeFor(role);
  }
  const isAgentPage = from === '/agent' || from.startsWith('/agent/');
  return isAgentPage === (role === 'AGENT') ? from : homeFor(role);
}
