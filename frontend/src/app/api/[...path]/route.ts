/**
 * Catch-all API proxy route.
 * Every request to /api/* from the browser is forwarded server-side
 * to the gateway. The gateway URL is read at REQUEST TIME from the
 * environment, so it always picks up the Docker-injected value.
 *
 * Docker:     API_GATEWAY_URL=http://api-gateway:8000  (set in docker-compose.yml)
 * Local dev:  API_GATEWAY_URL=http://localhost:8000    (default fallback)
 */
import { NextRequest, NextResponse } from 'next/server';

const GATEWAY = process.env.API_GATEWAY_URL || 'http://localhost:8000';

async function proxy(request: NextRequest): Promise<NextResponse> {
  // Reconstruct the target URL: replace the Next.js origin with the gateway
  const url = new URL(request.url);
  const target = `${GATEWAY}${url.pathname}${url.search}`;

  // Forward the request with all original headers except 'host'
  const headers = new Headers(request.headers);
  headers.delete('host');

  try {
    const upstream = await fetch(target, {
      method: request.method,
      headers,
      body: ['GET', 'HEAD'].includes(request.method) ? undefined : request.body,
      // @ts-expect-error — duplex is required for streaming bodies in Node fetch
      duplex: 'half',
    });

    // Stream the upstream response back to the browser
    return new NextResponse(upstream.body, {
      status: upstream.status,
      headers: upstream.headers,
    });
  } catch (err) {
    console.error(`Failed to proxy ${target}:`, err);
    return NextResponse.json(
      { error: 'Gateway unreachable', detail: String(err) },
      { status: 502 }
    );
  }
}

export const GET     = proxy;
export const POST    = proxy;
export const PUT     = proxy;
export const DELETE  = proxy;
export const PATCH   = proxy;
export const OPTIONS = proxy;
