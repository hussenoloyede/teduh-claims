// The app's addresses. React Router reads the address and shows the matching screen without asking the
// server for a new page. Spring Boot answers any of these addresses with index.html (WebPages.java), so a
// reload or a shared link lands on the right screen too.
//
// The two layouts are also the guards: a page inside CustomerLayout is only shown to a signed-in
// customer, one inside AgentLayout only to an agent. Screens still to be built show ComingSoonPage.

import { createBrowserRouter, Navigate } from 'react-router';
import { AgentLayout } from './layouts/AgentLayout';
import { CustomerLayout } from './layouts/CustomerLayout';
import { ClaimsPage } from './pages/ClaimsPage';
import { ComingSoonPage } from './pages/ComingSoonPage';
import { HomePage } from './pages/HomePage';
import { NotFoundPage } from './pages/NotFoundPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { PoliciesPage } from './pages/PoliciesPage';
import { ProfilePage } from './pages/ProfilePage';
import { SignInPage } from './pages/SignInPage';

export const router = createBrowserRouter([
  { path: '/sign-in', element: <SignInPage /> },
  {
    element: <CustomerLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'claims', element: <ClaimsPage /> },
      { path: 'claims/new', element: <ComingSoonPage title="New claim" /> },
      { path: 'claims/:claimId', element: <ComingSoonPage title="Claim" /> },
      { path: 'policies', element: <PoliciesPage /> },
      { path: 'profile', element: <ProfilePage /> },
      { path: 'notifications', element: <NotificationsPage claimPath={(id) => `/claims/${id}`} /> },
    ],
  },
  {
    path: 'agent',
    element: <AgentLayout />,
    children: [
      { index: true, element: <Navigate to="queue" replace /> },
      { path: 'queue', element: <ComingSoonPage title="Claims queue" /> },
      { path: 'assigned', element: <ComingSoonPage title="Assigned reviews" /> },
      { path: 'history', element: <ComingSoonPage title="History" /> },
      { path: 'claims/:claimId', element: <ComingSoonPage title="Claim review" /> },
      { path: 'notifications', element: <NotificationsPage claimPath={(id) => `/agent/claims/${id}`} /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]);
