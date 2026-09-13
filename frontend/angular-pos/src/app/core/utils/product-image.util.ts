import { environment } from '../../../environments/environment';

/**
 * High-quality SVG placeholder for products without an image or offline fallback.
 * Encoded as data URI so it works 100% offline without any network requests.
 */
export const DEFAULT_PRODUCT_PLACEHOLDER_SVG = `data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="100%" height="100%"><defs><linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="%231a1f2c"/><stop offset="100%" stop-color="%230f131a"/></linearGradient><linearGradient id="accent" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" stop-color="%23f59e0b"/><stop offset="100%" stop-color="%23d97706"/></linearGradient></defs><rect width="100%" height="100%" fill="url(%23bg)"/><circle cx="100" cy="100" r="64" fill="%23262d3d" stroke="%23374151" stroke-width="2"/><path d="M70 115 C70 85, 130 85, 130 115 Z" fill="url(%23accent)" opacity="0.85"/><circle cx="100" cy="82" r="6" fill="%23f59e0b"/><line x1="62" y1="118" x2="138" y2="118" stroke="%23f59e0b" stroke-width="4" stroke-linecap="round"/><text x="100" y="148" text-anchor="middle" fill="%2394a3b8" font-family="system-ui,-apple-system,sans-serif" font-size="11" font-weight="600" letter-spacing="0.5">RASM YO‘Q</text></svg>`;

/**
 * Resolves a product image URL against the backend server base URL.
 * Works seamlessly across Dev, Prod, and Offline Windows Desktop/Electron deployments.
 */
export function getProductImageUrl(imageUrl?: string | null): string {
  if (!imageUrl || !imageUrl.trim()) {
    return DEFAULT_PRODUCT_PLACEHOLDER_SVG;
  }

  const trimmed = imageUrl.trim();

  // If already absolute or data/blob URI, return as-is
  if (
    trimmed.startsWith('http://') ||
    trimmed.startsWith('https://') ||
    trimmed.startsWith('data:') ||
    trimmed.startsWith('blob:')
  ) {
    return trimmed;
  }

  // Derive backend base host from environment.apiUrl (e.g. "http://localhost:8080/api" -> "http://localhost:8080")
  const base = environment.apiUrl ? environment.apiUrl.replace(/\/api\/?$/, '') : '';
  const cleanPath = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;

  return `${base}${cleanPath}`;
}

/**
 * Graceful error fallback for <img> tags in templates.
 * Replaces broken images with the professional POS SVG placeholder.
 */
export function handleImageError(event: Event): void {
  const target = event.target as HTMLImageElement;
  if (target && target.src !== DEFAULT_PRODUCT_PLACEHOLDER_SVG) {
    target.src = DEFAULT_PRODUCT_PLACEHOLDER_SVG;
  }
}
