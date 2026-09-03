import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import type { Session } from '@supabase/supabase-js'
import {
  Alert, Box, Button, Checkbox, CircularProgress, FormControlLabel, MenuItem,
  Paper, Stack, Tab, Table, TableBody, TableCell, TableHead, TableRow,
  Tabs, TextField, Typography,
} from '@mui/material'
import { apiJson, type TenantMembership } from './api'

type Row = Record<string, unknown>
type ResourceKey = 'company' | 'branches' | 'departments' | 'grades' | 'designations' | 'cost-centres' | 'holidays'
type Field = { key: string; label: string; kind?: 'text' | 'date' | 'boolean' | 'status' | 'relation'; optionsFrom?: ResourceKey; optionLabel?: string }
type Resource = { key: ResourceKey; label: string; singular: string; fields: Field[]; singleton?: boolean }

const resources: Resource[] = [
  { key: 'company', label: 'Company', singular: 'Company profile', singleton: true, fields: [
    { key: 'registeredAddress', label: 'Registered address' }, { key: 'pan', label: 'PAN' }, { key: 'tan', label: 'TAN' },
    { key: 'gstin', label: 'GSTIN' }, { key: 'pfRegistration', label: 'PF registration' },
    { key: 'esiRegistration', label: 'ESI registration' }, { key: 'logoPath', label: 'Logo path' },
  ]},
  { key: 'branches', label: 'Branches', singular: 'Branch', fields: [
    { key: 'code', label: 'Code' }, { key: 'name', label: 'Name' }, { key: 'address', label: 'Address' },
    { key: 'stateCode', label: 'State code' }, { key: 'professionalTaxApplicable', label: 'Professional tax', kind: 'boolean' },
    { key: 'labourWelfareFundApplicable', label: 'Labour welfare fund', kind: 'boolean' }, { key: 'status', label: 'Status', kind: 'status' },
  ]},
  { key: 'departments', label: 'Departments', singular: 'Department', fields: [
    { key: 'code', label: 'Code' }, { key: 'name', label: 'Name' },
    { key: 'parentDepartmentId', label: 'Parent department', kind: 'relation', optionsFrom: 'departments', optionLabel: 'name' },
    { key: 'status', label: 'Status', kind: 'status' },
  ]},
  { key: 'grades', label: 'Grades', singular: 'Grade', fields: [
    { key: 'code', label: 'Code' }, { key: 'name', label: 'Name' }, { key: 'status', label: 'Status', kind: 'status' },
  ]},
  { key: 'designations', label: 'Designations', singular: 'Designation', fields: [
    { key: 'code', label: 'Code' }, { key: 'title', label: 'Title' },
    { key: 'gradeId', label: 'Grade', kind: 'relation', optionsFrom: 'grades', optionLabel: 'name' },
    { key: 'status', label: 'Status', kind: 'status' },
  ]},
  { key: 'cost-centres', label: 'Cost centres', singular: 'Cost centre', fields: [
    { key: 'code', label: 'Code' }, { key: 'name', label: 'Name' },
    { key: 'accountingReference', label: 'Accounting reference' }, { key: 'status', label: 'Status', kind: 'status' },
  ]},
  { key: 'holidays', label: 'Holidays', singular: 'Holiday', fields: [
    { key: 'holidayDate', label: 'Date', kind: 'date' }, { key: 'name', label: 'Name' },
    { key: 'branchId', label: 'Branch', kind: 'relation', optionsFrom: 'branches', optionLabel: 'name' },
    { key: 'optional', label: 'Optional holiday', kind: 'boolean' },
  ]},
]

const editRoles = new Set(['SYSTEM_ADMIN', 'GROUP_ADMIN', 'COMPANY_ADMIN', 'HR_MANAGER'])

export interface OrganizationAdminProps { session: Session; membership: TenantMembership }

export default function OrganizationAdmin({ session, membership }: OrganizationAdminProps) {
  const [activeKey, setActiveKey] = useState<ResourceKey>('company')
  const [data, setData] = useState<Partial<Record<ResourceKey, Row[]>>>({})
  const [form, setForm] = useState<Row>({})
  const [selectedId, setSelectedId] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const canEdit = membership.roles.some((role) => editRoles.has(role))
  const resource = resources.find((item) => item.key === activeKey)!

  const loadAll = useCallback(async () => {
    setBusy(true); setError(''); setNotice('')
    try {
      const results = await Promise.all(resources.map(async (item) => {
        const value = await apiJson<Row | Row[]>(`/organization/${item.key}`, session, membership.tenantId)
        return [item.key, Array.isArray(value) ? value : [value]] as const
      }))
      setData(Object.fromEntries(results) as Partial<Record<ResourceKey, Row[]>>)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to load organisation masters.')
    } finally { setBusy(false) }
  }, [membership.tenantId, session])

  useEffect(() => { setSelectedId(''); setForm({}); void loadAll() }, [loadAll])
  useEffect(() => {
    setSelectedId('')
    setForm(resource.singleton ? (data[resource.key]?.[0] ?? {}) : {})
    setError(''); setNotice('')
  }, [activeKey, data, resource.key, resource.singleton])

  const rows = data[activeKey] ?? []
  const visibleFields = resource.fields.slice(0, 4)
  const title = useMemo(() => resource.singleton ? membership.legalName : resource.label, [membership.legalName, resource])

  function selectRow(row: Row) { setSelectedId(String(row.id ?? '')); setForm({ ...row }); setNotice('') }
  function newRecord() { setSelectedId(''); setForm({ status: 'ACTIVE' }); setNotice('') }
  function setValue(key: string, value: unknown) { setForm((current) => ({ ...current, [key]: value })) }

  async function save(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    try {
      const path = `/organization/${resource.key}${resource.singleton ? '' : selectedId ? `/${selectedId}` : ''}`
      const method = resource.singleton || selectedId ? 'PUT' : 'POST'
      const payload = Object.fromEntries(resource.fields.map((field) => [field.key, form[field.key] === '' ? null : form[field.key]]))
      await apiJson<Row>(path, session, membership.tenantId, { method, body: JSON.stringify(payload) })
      setNotice(`${resource.singular} saved.`); setSelectedId(''); setForm({}); await loadAll()
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to save record.') }
    finally { setBusy(false) }
  }

  return <Stack spacing={3}>
    <Paper sx={{ overflow: 'hidden' }}>
      <Tabs value={activeKey} onChange={(_event, value: ResourceKey) => setActiveKey(value)} variant="scrollable" scrollButtons="auto">
        {resources.map((item) => <Tab key={item.key} value={item.key} label={item.label} />)}
      </Tabs>
    </Paper>
    {error && <Alert severity="error">{error}</Alert>}
    {notice && <Alert severity="success">{notice}</Alert>}
    {busy && !rows.length ? <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}><CircularProgress /></Box> :
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={3} alignItems="flex-start">
        {!resource.singleton && <Paper sx={{ width: { xs: '100%', md: '58%' }, overflow: 'auto' }}>
          <Stack direction="row" alignItems="center" sx={{ p: 2 }}>
            <Typography variant="h6" sx={{ flexGrow: 1 }}>{title}</Typography>
            {canEdit && <Button onClick={newRecord}>New</Button>}
          </Stack>
          <Table size="small">
            <TableHead><TableRow>{visibleFields.map((field) => <TableCell key={field.key}>{field.label}</TableCell>)}</TableRow></TableHead>
            <TableBody>{rows.map((row) => <TableRow hover selected={selectedId === row.id} key={String(row.id)} onClick={() => selectRow(row)} sx={{ cursor: 'pointer' }}>
              {visibleFields.map((field) => <TableCell key={field.key}>{displayValue(field, row[field.key], data)}</TableCell>)}
            </TableRow>)}</TableBody>
          </Table>
          {!rows.length && <Typography color="text.secondary" sx={{ p: 3 }}>No records configured for this company.</Typography>}
        </Paper>}
        <Paper component="form" onSubmit={save} sx={{ p: 3, width: { xs: '100%', md: resource.singleton ? '100%' : '42%' } }}>
          <Typography variant="h6" gutterBottom>{resource.singleton ? title : selectedId ? `Edit ${resource.singular}` : `New ${resource.singular}`}</Typography>
          {!canEdit && <Alert severity="info" sx={{ mb: 2 }}>Your roles provide read-only access.</Alert>}
          <Stack spacing={2}>
            {resource.fields.map((field) => <FieldInput key={field.key} field={field} value={form[field.key]} setValue={setValue} data={data} selectedId={selectedId} disabled={!canEdit || busy} />)}
            {canEdit && <Button type="submit" variant="contained" disabled={busy}>{busy ? 'Saving…' : 'Save'}</Button>}
          </Stack>
        </Paper>
      </Stack>}
  </Stack>
}

function FieldInput({ field, value, setValue, data, selectedId, disabled }: { field: Field; value: unknown; setValue: (key: string, value: unknown) => void; data: Partial<Record<ResourceKey, Row[]>>; selectedId: string; disabled: boolean }) {
  if (field.kind === 'boolean') return <FormControlLabel control={<Checkbox checked={Boolean(value)} onChange={(event) => setValue(field.key, event.target.checked)} disabled={disabled} />} label={field.label} />
  if (field.kind === 'status') return <TextField select label={field.label} value={String(value ?? 'ACTIVE')} onChange={(event) => setValue(field.key, event.target.value)} disabled={disabled}><MenuItem value="ACTIVE">Active</MenuItem><MenuItem value="INACTIVE">Inactive</MenuItem></TextField>
  if (field.kind === 'relation') {
    const options = (data[field.optionsFrom!] ?? []).filter((item) => String(item.id) !== selectedId)
    return <TextField select label={field.label} value={String(value ?? '')} onChange={(event) => setValue(field.key, event.target.value)} disabled={disabled}>
      <MenuItem value=""><em>None / all locations</em></MenuItem>
      {options.map((item) => <MenuItem key={String(item.id)} value={String(item.id)}>{String(item[field.optionLabel!] ?? item.id)}</MenuItem>)}
    </TextField>
  }
  return <TextField label={field.label} type={field.kind === 'date' ? 'date' : 'text'} value={String(value ?? '')} onChange={(event) => setValue(field.key, event.target.value)} disabled={disabled} required={['code', 'name', 'title', 'holidayDate'].includes(field.key)} InputLabelProps={field.kind === 'date' ? { shrink: true } : undefined} multiline={field.key.toLowerCase().includes('address')} />
}

function displayValue(field: Field, value: unknown, data: Partial<Record<ResourceKey, Row[]>>) {
  if (field.kind === 'boolean') return value ? 'Yes' : 'No'
  if (field.kind === 'relation' && value) return String((data[field.optionsFrom!] ?? []).find((item) => item.id === value)?.[field.optionLabel!] ?? '—')
  return value == null || value === '' ? '—' : String(value)
}