import type { Session } from '@supabase/supabase-js'

export interface TenantMembership {
  tenantId: string
  companyCode: string
  legalName: string
  roles: string[]
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api').replace(/\/$/, '')

export async function loadTenantMemberships(session: Session): Promise<TenantMembership[]> {
  const response = await fetch(`${apiBaseUrl}/me/tenants`, {
    headers: { Authorization: `Bearer ${session.access_token}` },
  })
  if (!response.ok) {
    if (response.status === 401) {
      const challenge = response.headers.get('www-authenticate')
      const description = challenge?.match(/error_description="([^"]+)"/)?.[1]
      throw new Error(description
        ? `Backend rejected the session: ${description}`
        : 'Your session is not authorized by the backend.')
    }
    let detail = ''
    try {
      const problem = await response.json() as { message?: string; error?: string }
      detail = problem.message ?? problem.error ?? ''
    } catch {
      detail = await response.text().catch(() => '')
    }
    throw new Error(detail
      ? `Unable to load company access (${response.status}): ${detail}`
      : `Unable to load company access (${response.status}).`)
  }
  return response.json() as Promise<TenantMembership[]>
}

export async function apiFetch(path: string, session: Session, tenantId: string, init: RequestInit = {}) {
  const headers = new Headers(init.headers)
  headers.set('Authorization', `Bearer ${session.access_token}`)
  headers.set('X-Tenant-Id', tenantId)
  return fetch(`${apiBaseUrl}${path.startsWith('/') ? path : `/${path}`}`, { ...init, headers })
}
