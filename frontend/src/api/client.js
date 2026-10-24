const API_BASE = '/api';

export async function request(path, options = {}, auth) {
  const headers = new Headers(options.headers || {});

  if (auth) {
    headers.set('Authorization', `Basic ${btoa(`${auth.username}:${auth.password}`)}`);
  }

  if (options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(API_BASE + path, { ...options, headers });
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      message = (await response.json()).message || message;
    } catch {
      // Some responses do not include a JSON body.
    }
    throw new Error(message);
  }

  if (response.status === 204) return null;
  return response.headers.get('content-type')?.includes('application/json')
    ? response.json()
    : response;
}
