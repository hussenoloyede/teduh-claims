import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import { meQueryKey, signOut } from './api/auth';
import { queryClient } from './queryClient';

/**
 * Ends the session, forgets everything cached for this person, and goes to the sign-in page. The cache is
 * cleared even if the request fails, so the next person on this browser never sees the last one's claims.
 */
export function useSignOut() {
  const navigate = useNavigate();
  return useMutation({
    mutationFn: signOut,
    onSettled: () => {
      // "Nobody is signed in" first, so the signed-in pages close without asking for anything more...
      queryClient.setQueryData(meQueryKey, null);
      void navigate('/sign-in', { replace: true });
      // ...then everything else this person had cached is dropped.
      queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== meQueryKey[0] });
    },
  });
}
