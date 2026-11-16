import { CircleAlert, CloudUpload, FileText, Image, Plus, X } from 'lucide-react';
import { useId, useRef, useState, type DragEvent } from 'react';
import { formatFileSize } from '../../lib/format';
import styles from './FileDrop.module.css';

export type SelectedFile = {
  id: string;
  name: string;
  size: number;
  /** Why this file can't be sent, when the browser can already tell. */
  problem?: string;
};

type FileDropProps = {
  /** The id the error summary links to: the Choose files button. */
  id?: string;
  files: SelectedFile[];
  max: number;
  onAdd: (files: File[]) => void;
  onRemove: (id: string) => void;
  error?: string;
};

// The upload area and the list of picked files on C05. Choosing files works with the button (which opens
// the system file picker) or by dropping files on the area. Nothing is uploaded until the claim is
// submitted: the files stay in the browser until then.
export function FileDrop({ id, files, max, onAdd, onRemove, error }: FileDropProps) {
  const input = useRef<HTMLInputElement>(null);
  const [dragging, setDragging] = useState(false);
  const listId = useId();
  const errorId = `${listId}-error`;
  const usable = files.filter((file) => !file.problem);

  function drop(event: DragEvent) {
    event.preventDefault();
    setDragging(false);
    onAdd([...event.dataTransfer.files]);
  }

  return (
    <div className={styles.wrap}>
      <div
        className={styles.zone}
        data-dragging={dragging || undefined}
        onDragOver={(event) => {
          event.preventDefault();
          setDragging(true);
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={drop}
      >
        <span className={styles.cloud} aria-hidden="true">
          <CloudUpload size={24} />
        </span>
        <button
          id={id}
          type="button"
          className={styles.choose}
          onClick={() => input.current?.click()}
          aria-describedby={error ? errorId : undefined}
        >
          <Plus size={16} aria-hidden="true" />
          Choose files
        </button>
        <p className={styles.help}>or drop them here. PDF, JPG or PNG, up to 10 MB each.</p>
        <input
          ref={input}
          type="file"
          multiple
          hidden
          accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
          onChange={(event) => {
            onAdd([...(event.target.files ?? [])]);
            // Lets the same file be picked again after it was removed.
            event.target.value = '';
          }}
        />
      </div>

      {error && (
        <p className={styles.error} id={errorId}>
          {error}
        </p>
      )}

      {files.length > 0 && (
        <div className={styles.listBox}>
          <p className={styles.listTitle} id={listId}>
            Attached files <span className={styles.count}>{`${usable.length} of ${max}`}</span>
          </p>
          <ul className={styles.list} aria-labelledby={listId}>
            {files.map((file) => (
              <li key={file.id} className={styles.file} data-problem={file.problem ? true : undefined}>
                <span className={styles.fileIcon} aria-hidden="true">
                  {file.problem ? (
                    <CircleAlert size={18} />
                  ) : /\.pdf$/i.test(file.name) ? (
                    <FileText size={18} />
                  ) : (
                    <Image size={18} />
                  )}
                </span>
                <span className={styles.fileText}>
                  <span className={styles.fileName}>{file.name}</span>
                  <span className={styles.fileMeta}>
                    {formatFileSize(file.size)}
                    {file.problem ? ` · ${file.problem}` : ' · Ready to send'}
                  </span>
                </span>
                <button type="button" className={styles.remove} onClick={() => onRemove(file.id)}>
                  <X size={18} aria-hidden="true" />
                  <span className="visually-hidden">{`Remove ${file.name}`}</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
