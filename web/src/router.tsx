// The app's addresses. React Router reads the address and shows the matching screen without asking the
// server for a new page. Spring Boot answers any of these addresses with index.html (WebPages.java), so a
// reload or a shared link lands on the right screen too. The screens arrive here step by step in week 6.

import { createBrowserRouter } from 'react-router';
import App from './App';
import { NotFoundPage } from './pages/NotFoundPage';

export const router = createBrowserRouter([
  { path: '/', element: <App /> },
  { path: '*', element: <NotFoundPage /> },
]);
