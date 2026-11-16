import { useState } from 'react';
import { downloadDocument, type ClaimDetails } from '../../api/claims';
import { Banner } from '../../components/Banner/Banner';
import { FileRow } from '../../components/FileRow/FileRow';
import page from '../Page.module.css';

// A claim's documents, each with a Download button. Shared by the customer's claim page and the agent's
// review page.
/** Each document with a Download button. Only one link is fetched at a time, so a button shows it's busy. */
export function ClaimDocuments({ claim }: { claim: ClaimDetails }) {
  const [busy, setBusy] = useState<string>();
  const [failed, setFailed] = useState<string>();

  async function download(documentId: string) {
    setBusy(documentId);
    setFailed(undefined);
    try {
      await downloadDocument(claim.id, documentId);
    } catch (error) {
      setFailed(error instanceof Error ? error.message : 'The download link could not be made.');
    } finally {
      setBusy(undefined);
    }
  }

  return (
    <>
      {failed && <Banner variant="error">{failed}</Banner>}
      <ul className={page.stack}>
        {claim.documents.map((document) => (
          <li key={document.id}>
            <FileRow
              fileName={document.fileName}
              contentType={document.contentType}
              sizeBytes={document.sizeBytes}
              busy={busy === document.id}
              onDownload={() => void download(document.id)}
            />
          </li>
        ))}
      </ul>
    </>
  );
}
