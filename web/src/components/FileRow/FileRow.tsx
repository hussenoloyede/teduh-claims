import { Download, FileImage, FileText } from 'lucide-react';
import { formatFileSize } from '../../lib/format';
import styles from './FileRow.module.css';

type FileRowProps = {
  fileName: string;
  /** As the API stores it: application/pdf, image/png or image/jpeg. */
  contentType: string;
  sizeBytes: number;
  onDownload: () => void;
  /** True while the download link is being fetched. */
  busy?: boolean;
};

// One uploaded document. Download is a button, not a link, because the
// address only exists after asking the API for a 15-minute link.
export function FileRow({ fileName, contentType, sizeBytes, onDownload, busy = false }: FileRowProps) {
  const isImage = contentType.startsWith('image/');
  const Icon = isImage ? FileImage : FileText;
  const kind = contentType === 'application/pdf' ? 'PDF' : contentType.replace('image/', '').toUpperCase();

  return (
    <div className={styles.row}>
      <span className={styles.icon} aria-hidden="true">
        <Icon size={18} />
      </span>
      <div className={styles.text}>
        <span className={styles.name} title={fileName}>
          {fileName}
        </span>
        <span className={styles.meta}>
          {formatFileSize(sizeBytes)} · {kind}
        </span>
      </div>
      <button
        type="button"
        className={styles.action}
        onClick={onDownload}
        disabled={busy}
        aria-busy={busy || undefined}
      >
        <Download size={14} aria-hidden="true" />
        Download
        {/* Several rows each have a Download button; this says which file. */}
        <span className="visually-hidden"> {fileName}</span>
      </button>
    </div>
  );
}
