import { zip, Zippable } from 'fflate';

/**
 * Directorios que no aportan al analisis (dependencias, builds, VCS). Se
 * descartan en el navegador para no subir cientos de MB innecesarios; el
 * source-service aplica ademas su propia lista de exclusion.
 */
const IGNORED_DIRECTORIES = new Set([
  '.git', '.svn', '.hg', 'node_modules', 'bower_components', 'vendor',
  'target', 'build', 'dist', 'out', 'bin', 'obj', '.next', '.nuxt',
  '.venv', 'venv', 'env', '__pycache__', '.mypy_cache', '.pytest_cache',
  '.gradle', '.idea', '.vscode', '.terraform', 'coverage', '.angular', '.cache',
  '.dart_tool', 'Pods', 'DerivedData',
]);

/** El source-service no analiza archivos de mas de 2 MB; no tiene sentido subirlos. */
const MAX_FILE_BYTES = 2 * 1024 * 1024;
const MAX_FILES = 20_000;

export interface FolderArchive {
  name: string;
  blob: Blob;
  includedFiles: number;
  skippedFiles: number;
}

/** Nombre de la carpeta raiz elegida con <input webkitdirectory>. */
export function folderNameOf(files: File[]): string {
  const first = files[0]?.webkitRelativePath ?? '';
  return first.split('/')[0] || 'carpeta-local';
}

/**
 * Empaqueta en zip los archivos de una carpeta local elegida en el navegador,
 * manteniendo las rutas relativas y saltando dependencias y artefactos de build.
 */
export async function archiveFolder(files: File[]): Promise<FolderArchive> {
  const entries: Zippable = {};
  let included = 0;
  let skipped = 0;

  for (const file of files) {
    const path = file.webkitRelativePath || file.name;
    const segments = path.split('/');
    const ignored = segments.slice(0, -1).some((segment) => IGNORED_DIRECTORIES.has(segment));
    if (ignored || file.size > MAX_FILE_BYTES || included >= MAX_FILES) {
      skipped++;
      continue;
    }
    entries[path] = new Uint8Array(await file.arrayBuffer());
    included++;
  }

  if (included === 0) {
    throw new Error('La carpeta no tiene archivos que se puedan analizar (todo estaba en carpetas ignoradas).');
  }

  const bytes = await new Promise<Uint8Array>((resolve, reject) =>
    zip(entries, { level: 6 }, (error, data) => (error ? reject(error) : resolve(data))),
  );

  return {
    name: folderNameOf(files),
    blob: new Blob([bytes], { type: 'application/zip' }),
    includedFiles: included,
    skippedFiles: skipped,
  };
}
