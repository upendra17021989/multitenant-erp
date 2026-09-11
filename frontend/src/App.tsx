import { useEffect, useMemo, useState, type FormEvent } from 'react'
import type { Session } from '@supabase/supabase-js'
import { Alert, AppBar, Box, Button, Chip, CircularProgress, Container, FormControl, InputLabel, MenuItem, Paper, Select, Stack, Tab, Tabs, TextField, Toolbar, Typography } from '@mui/material'
import { loadTenantMemberships, type TenantMembership } from './api'
import { supabase, supabaseConfigurationMissing } from './supabase'
import OrganizationAdmin from './OrganizationAdmin'
import EmployeeAdmin from './EmployeeAdmin'
import AttendanceAdmin from './AttendanceAdmin'
import LeaveAdmin from './LeaveAdmin'

const ACTIVE_TENANT_KEY = 'erp.activeTenantId'

export default function App() {
  const [session, setSession] = useState<Session | null>(null)
  const [loadingSession, setLoadingSession] = useState(true)
  const [memberships, setMemberships] = useState<TenantMembership[]>([])
  const [activeTenantId, setActiveTenantId] = useState(localStorage.getItem(ACTIVE_TENANT_KEY) ?? '')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [module, setModule] = useState<'organization' | 'employees' | 'attendance' | 'leave'>('organization')

  useEffect(() => {
    if (!supabase) {
      setLoadingSession(false)
      return
    }
    void supabase.auth.getSession().then(({ data }) => {
      setSession(data.session)
      setLoadingSession(false)
    })
    const { data } = supabase.auth.onAuthStateChange((_event, nextSession) => setSession(nextSession))
    return () => data.subscription.unsubscribe()
  }, [])

  useEffect(() => {
    if (!session) {
      setMemberships([])
      return
    }
    setBusy(true)
    setError('')
    void loadTenantMemberships(session)
      .then((items) => {
        setMemberships(items)
        const savedIsAllowed = items.some((item) => item.tenantId === activeTenantId)
        const nextTenantId = savedIsAllowed ? activeTenantId : (items[0]?.tenantId ?? '')
        setActiveTenantId(nextTenantId)
        if (nextTenantId) localStorage.setItem(ACTIVE_TENANT_KEY, nextTenantId)
      })
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : 'Unable to load company access.'))
      .finally(() => setBusy(false))
  }, [session])

  const activeMembership = useMemo(
    () => memberships.find((item) => item.tenantId === activeTenantId),
    [activeTenantId, memberships],
  )
  const canViewEmployees = activeMembership?.roles.some((role) =>
    ['SYSTEM_ADMIN', 'GROUP_ADMIN', 'COMPANY_ADMIN', 'HR_MANAGER', 'PAYROLL_MANAGER'].includes(role)) ?? false

  async function signIn(event: FormEvent) {
    event.preventDefault()
    if (!supabase) return
    setBusy(true)
    setError('')
    const { error: signInError } = await supabase.auth.signInWithPassword({ email, password })
    if (signInError) setError(signInError.message)
    setBusy(false)
  }

  async function signOut() {
    await supabase?.auth.signOut()
    localStorage.removeItem(ACTIVE_TENANT_KEY)
    setActiveTenantId('')
  }

  function selectTenant(tenantId: string) {
    setActiveTenantId(tenantId)
    localStorage.setItem(ACTIVE_TENANT_KEY, tenantId)
  }

  if (loadingSession) return <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}><CircularProgress /></Box>

  if (supabaseConfigurationMissing) return <Container maxWidth="sm" sx={{ py: 8 }}>
    <Alert severity="error">Set VITE_SUPABASE_URL and VITE_SUPABASE_PUBLISHABLE_KEY.</Alert>
  </Container>

  if (!session) return <Container maxWidth="sm" sx={{ py: 8 }}>
    <Paper component="form" onSubmit={signIn} sx={{ p: 4 }}>
      <Stack spacing={3}>
        <Typography variant="h4">Sign in</Typography>
        <Typography color="text.secondary">Use your organisation account to access the HR & Payroll ERP.</Typography>
        {error && <Alert severity="error">{error}</Alert>}
        <TextField label="Email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
        <TextField label="Password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
        <Button type="submit" variant="contained" disabled={busy}>{busy ? 'Signing inÃ¢â‚¬Â¦' : 'Sign in'}</Button>
      </Stack>
    </Paper>
  </Container>

  return <Box sx={{ minHeight: '100vh' }}>
    <AppBar position="static" elevation={0}>
      <Toolbar sx={{ gap: 2 }}>
        <Typography variant="h6" sx={{ flexGrow: 1 }}>HR & Payroll ERP</Typography>
        {memberships.length > 0 && <FormControl size="small" sx={{ minWidth: 220, bgcolor: 'background.paper', borderRadius: 1 }}>
          <InputLabel>Active company</InputLabel>
          <Select label="Active company" value={activeTenantId} onChange={(event) => selectTenant(event.target.value)}>
            {memberships.map((membership) => <MenuItem key={membership.tenantId} value={membership.tenantId}>{membership.legalName}</MenuItem>)}
          </Select>
        </FormControl>}
        <Button color="inherit" onClick={signOut}>Sign out</Button>
      </Toolbar>
    </AppBar>
    <Container maxWidth="xl" sx={{ py: 4 }}>
      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
      {busy ? <CircularProgress /> : <Stack spacing={3}>
        <Paper sx={{ p: 3 }}>
          <Typography color="primary">ACTIVE COMPANY</Typography>
          <Typography variant="h4" gutterBottom>{activeMembership?.legalName ?? 'No company access'}</Typography>
          {activeMembership ? <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
            <Chip label={activeMembership.companyCode} />
            {activeMembership.roles.map((role) => <Chip key={role} label={role} color="primary" variant="outlined" />)}
          </Stack> : <Typography color="text.secondary">Your account is authenticated but has not been assigned to an active company.</Typography>}
        </Paper>
        {activeMembership && <>
          <Paper><Tabs value={module} onChange={(_event, value: 'organization' | 'employees' | 'attendance' | 'leave') => setModule(value)}>
            <Tab value="organization" label="Organisation" />
            {canViewEmployees && <Tab value="employees" label="Employees" />}
            {canViewEmployees && <Tab value="attendance" label="Attendance" />}`r`n            {canViewEmployees && <Tab value="leave" label="Leave" />}
          </Tabs></Paper>
          {(module === 'organization' || !canViewEmployees) && <OrganizationAdmin session={session} membership={activeMembership} />}
          {module === 'employees' && canViewEmployees && <EmployeeAdmin session={session} membership={activeMembership} />}
          {module === 'attendance' && canViewEmployees && <AttendanceAdmin session={session} membership={activeMembership} />}
          {module === 'leave' && canViewEmployees && <LeaveAdmin session={session} membership={activeMembership} />}
        </>}
      </Stack>}
    </Container>
  </Box>
}
