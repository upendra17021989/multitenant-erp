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
    throw new Error(response.status === 401
      ? 'Your session is not authorized by the backend.'
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
