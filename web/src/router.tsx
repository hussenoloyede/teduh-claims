// The app's addresses. React Router reads the address and shows the matching screen without asking the
// server for a new page. Spring Boot answers any of these addresses with index.html (WebPages.java), so a
// reload or a shared link lands on the right screen too.
//
// The two layouts are also the guards: a page inside CustomerLayout is only shown to a signed-in
// customer, one inside AgentLayout only to an agent.

import { createBrowserRouter, Navigate } from 'react-router';
import { AgentLayout } from './layouts/AgentLayout';
import { CustomerLayout } from './layouts/CustomerLayout';
import { AssignedPage, HistoryPage, QueuePage } from './pages/agent/AgentListPages';
import { ReviewPage } from './pages/agent/ReviewPage';
import { ClaimPage } from './pages/claim/ClaimPage';
import { ChoosePlanPage } from './pages/choosePlan/ChoosePlanPage';
import { ClaimsPage } from './pages/ClaimsPage';
import { HelpPage } from './pages/HelpPage';
import { HomePage } from './pages/HomePage';
import { NewClaimPage } from './pages/newClaim/NewClaimPage';
import { NotFoundPage } from './pages/NotFoundPage';
import { NotificationsPage } from './pages/NotificationsPage';
import { PoliciesPage } from './pages/PoliciesPage';
import { ProfilePage } from './pages/ProfilePage';
import { SignInPage } from './pages/SignInPage';
import { WelcomePage } from './pages/welcome/WelcomePage';

export const router = createBrowserRouter([
  { path: '/sign-in', element: <SignInPage /> },
  // Full screen, without the header and tabs. Anyone can read it; its buttons lead to customer pages.
  { path: '/welcome', element: <WelcomePage /> },
  {
    element: <CustomerLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'claims', element: <ClaimsPage /> },
      { path: 'claims/new', element: <NewClaimPage /> },
      { path: 'claims/:claimId', element: <ClaimPage /> },
      { path: 'policies', element: <PoliciesPage /> },
      { path: 'plans', element: <ChoosePlanPage /> },
      { path: 'help', element: <HelpPage /> },
      { path: 'profile', element: <ProfilePage /> },
      { path: 'notifications', element: <NotificationsPage claimPath={(id) => `/claims/${id}`} /> },
    ],
  },
  {
    path: 'agent',
    element: <AgentLayout />,
    children: [
      { index: true, element: <Navigate to="queue" replace /> },
      { path: 'queue', element: <QueuePage /> },
      { path: 'assigned', element: <AssignedPage /> },
      { path: 'history', element: <HistoryPage /> },
      { path: 'claims/:claimId', element: <ReviewPage /> },
      { path: 'notifications', element: <NotificationsPage claimPath={(id) => `/agent/claims/${id}`} /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
]);
