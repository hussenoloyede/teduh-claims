// The app's addresses. React Router reads the address and shows the matching screen without asking the
// server for a new page. Spring Boot answers any of these addresses with index.html (WebPages.java), so a
// reload or a shared link lands on the right screen too.
//
// The two layouts are also the guards: a page inside CustomerLayout is only shown to a signed-in
// customer, one inside AgentLayout only to an agent. Screens still to be built show ComingSoonPage.

import { createBrowserRouter, Navigate } from 'react-router';
import { AgentLayout } from './layouts/AgentLayout';
import { CustomerLayout } from './layouts/CustomerLayout';
import { ComingSoonPage } from './pages/ComingSoonPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { ProfilePage } from './pages/ProfilePage';
import { SignInPage } from './pages/SignInPage';

export const router = createBrowserRouter([
  { path: '/sign-in', element: <SignInPage /> },
  {
    element: <CustomerLayout />,
    children: [
      { index: true, element: <ComingSoonPage title="Home" /> },
      { path: 'claims', element: <ComingSoonPage title="Claims" /> },
      { path: 'policies', element: <ComingSoonPage title="Policies" /> },
      { path: 'profile', element: <ProfilePage /> },
      { path: 'notifications', element: <ComingSoonPage title="Notifications" /> },
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
      { path: 'notifications', element: <ComingSoonPage title="Notifications" /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]);
