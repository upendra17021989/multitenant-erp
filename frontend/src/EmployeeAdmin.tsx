import { useCallback, useEffect, useState, type FormEvent } from 'react'
import type { Session } from '@supabase/supabase-js'
import {
  Alert, Box, Button, CircularProgress, MenuItem, Paper, Stack, Table, TableBody,
  TableCell, TableHead, TableRow, TextField, Typography,
} from '@mui/material'
import { apiFetch, apiJson, type TenantMembership } from './api'

type Row = Record<string, unknown>
type Option = { id: string; name?: string; title?: string; employeeNumber?: string; firstName?: string; lastName?: string }
type Field = { key: string; label: string; type?: 'date' | 'email' | 'select'; options?: readonly string[]; relation?: keyof Masters }
type Masters = { branches: Option[]; departments: Option[]; designations: Option[]; grades: Option[]; costCentres: Option[]; employees: Option[] }
type EmployeeDocument = { id: string; documentType: string; fileName: string; contentType: string; sizeBytes: number; uploadedAt: string }

const emptyMasters: Masters = { branches: [], departments: [], designations: [], grades: [], costCentres: [], employees: [] }
const editRoles = new Set(['SYSTEM_ADMIN', 'GROUP_ADMIN', 'COMPANY_ADMIN', 'HR_MANAGER'])
const statuses = ['ACTIVE', 'PROBATION', 'NOTICE', 'INACTIVE', 'EXITED'] as const
const employmentTypes = ['PERMANENT', 'PROBATION', 'CONTRACT', 'CONSULTANT', 'INTERN'] as const

const sections: { title: string; fields: Field[] }[] = [
  { title: 'Employment', fields: [
    { key: 'employeeNumber', label: 'Employee number' }, { key: 'employmentStatus', label: 'Status', type: 'select', options: statuses },
    { key: 'employmentType', label: 'Employment type', type: 'select', options: employmentTypes },
    { key: 'joiningDate', label: 'Joining date', type: 'date' }, { key: 'confirmationDate', label: 'Confirmation date', type: 'date' },
    { key: 'probationEndDate', label: 'Probation end date', type: 'date' }, { key: 'exitDate', label: 'Exit date', type: 'date' },
    { key: 'exitReason', label: 'Exit reason' }, { key: 'workEmail', label: 'Work email', type: 'email' },
  ]},
  { title: 'Personal details', fields: [
    { key: 'firstName', label: 'First name' }, { key: 'middleName', label: 'Middle name' }, { key: 'lastName', label: 'Last name' },
    { key: 'dateOfBirth', label: 'Date of birth', type: 'date' }, { key: 'gender', label: 'Gender' },
    { key: 'personalEmail', label: 'Personal email', type: 'email' }, { key: 'mobileNumber', label: 'Mobile number' },
    { key: 'currentAddress', label: 'Current address' }, { key: 'permanentAddress', label: 'Permanent address' },
    { key: 'emergencyContactName', label: 'Emergency contact' }, { key: 'emergencyContactPhone', label: 'Emergency phone' },
  ]},
  { title: 'Organisation assignment', fields: [
    { key: 'branchId', label: 'Branch', type: 'select', relation: 'branches' },
    { key: 'departmentId', label: 'Department', type: 'select', relation: 'departments' },
    { key: 'designationId', label: 'Designation', type: 'select', relation: 'designations' },
    { key: 'gradeId', label: 'Grade', type: 'select', relation: 'grades' },
    { key: 'costCentreId', label: 'Cost centre', type: 'select', relation: 'costCentres' },
    { key: 'reportingManagerEmploymentId', label: 'Reporting manager', type: 'select', relation: 'employees' as keyof Masters },
  ]},
  { title: 'Payment and statutory details', fields: [
    { key: 'paymentMode', label: 'Payment mode', type: 'select', options: ['BANK_TRANSFER', 'CHEQUE', 'CASH'] },
    { key: 'bankAccountName', label: 'Account holder' }, { key: 'bankAccountNumber', label: 'Account number' },
    { key: 'bankName', label: 'Bank' }, { key: 'bankBranch', label: 'Bank branch' }, { key: 'bankIfsc', label: 'IFSC' },
    { key: 'pan', label: 'PAN' }, { key: 'aadhaarLastFour', label: 'Aadhaar last four digits' },
    { key: 'uan', label: 'UAN' }, { key: 'pfNumber', label: 'PF number' }, { key: 'esiNumber', label: 'ESI number' },
  ]},
]

const initialForm: Row = { employmentStatus: 'ACTIVE', employmentType: 'PERMANENT', paymentMode: 'BANK_TRANSFER' }

export default function EmployeeAdmin({ session, membership }: { session: Session; membership: TenantMembership }) {
  const [employees, setEmployees] = useState<Row[]>([])
  const [masters, setMasters] = useState<Masters>(emptyMasters)
  const [form, setForm] = useState<Row>(initialForm)
  const [selectedId, setSelectedId] = useState('')
  const [status, setStatus] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const canEdit = membership.roles.some((role) => editRoles.has(role))

  const load = useCallback(async () => {
    setBusy(true); setError('')
    try {
      const [employeeRows, branches, departments, designations, grades, costCentres] = await Promise.all([
        apiJson<Row[]>(`/employees${status ? `?status=${status}` : ''}`, session, membership.tenantId),
        apiJson<Option[]>('/organization/branches', session, membership.tenantId),
        apiJson<Option[]>('/organization/departments', session, membership.tenantId),
        apiJson<Option[]>('/organization/designations', session, membership.tenantId),
        apiJson<Option[]>('/organization/grades', session, membership.tenantId),
        apiJson<Option[]>('/organization/cost-centres', session, membership.tenantId),
      ])
      setEmployees(employeeRows); setMasters({ branches, departments, designations, grades, costCentres, employees: employeeRows as Option[] })
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to load employees.') }
    finally { setBusy(false) }
  }, [membership.tenantId, session, status])

  useEffect(() => { setSelectedId(''); setForm(initialForm); void load() }, [load])

  function select(row: Row) { setSelectedId(String(row.id)); setForm({ ...row }); setNotice('') }
  function newEmployee() { setSelectedId(''); setForm(initialForm); setNotice('') }
  function setValue(key: string, value: string) { setForm((current) => ({ ...current, [key]: value })) }

  async function save(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('')
    try {
      const payload = Object.fromEntries(sections.flatMap((section) => section.fields).map((field) => [field.key, form[field.key] || null]))
      await apiJson(`/employees${selectedId ? `/${selectedId}` : ''}`, session, membership.tenantId,
        { method: selectedId ? 'PUT' : 'POST', body: JSON.stringify(payload) })
      setNotice(`Employee ${selectedId ? 'updated' : 'created'}.`); newEmployee(); await load()
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to save employee.') }
    finally { setBusy(false) }
  }

  return <Stack spacing={3}>
    {error && <Alert severity="error">{error}</Alert>}
    {notice && <Alert severity="success">{notice}</Alert>}
    <Stack direction={{ xs: 'column', lg: 'row' }} spacing={3} alignItems="flex-start">
      <Paper sx={{ width: { xs: '100%', lg: '42%' }, overflow: 'auto' }}>
        <Stack direction="row" spacing={2} alignItems="center" sx={{ p: 2 }}>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>Employees</Typography>
          <TextField select size="small" label="Status" value={status} onChange={(event) => setStatus(event.target.value)} sx={{ minWidth: 135 }}>
            <MenuItem value="">All</MenuItem>{statuses.map((value) => <MenuItem key={value} value={value}>{label(value)}</MenuItem>)}
          </TextField>
          {canEdit && <Button onClick={newEmployee}>New</Button>}
        </Stack>
        {busy && !employees.length ? <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}><CircularProgress /></Box> : <>
          <Table size="small"><TableHead><TableRow><TableCell>Number</TableCell><TableCell>Name</TableCell><TableCell>Status</TableCell></TableRow></TableHead>
            <TableBody>{employees.map((employee) => <TableRow hover key={String(employee.id)} selected={selectedId === employee.id} onClick={() => select(employee)} sx={{ cursor: 'pointer' }}>
              <TableCell>{String(employee.employeeNumber)}</TableCell><TableCell>{`${employee.firstName} ${employee.lastName}`}</TableCell><TableCell>{label(String(employee.employmentStatus))}</TableCell>
            </TableRow>)}</TableBody></Table>
          {!employees.length && <Typography color="text.secondary" sx={{ p: 3 }}>No employees found for this company.</Typography>}
        </>}
      </Paper>
      <Paper sx={{ p: 3, width: { xs: '100%', lg: '58%' } }}>
        <Box component="form" onSubmit={save}>
          <Typography variant="h6">{selectedId ? 'Edit employee' : 'New employee'}</Typography>
          {!canEdit && <Alert severity="info" sx={{ mt: 2 }}>Your roles provide read-only access.</Alert>}
          {sections.map((section) => <Box key={section.title} sx={{ mt: 3 }}>
          <Typography variant="subtitle1" color="primary" gutterBottom>{section.title}</Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2 }}>
            {section.fields.map((field) => <EmployeeField key={field.key} field={field} value={form[field.key]} setValue={setValue}
              masters={{ ...masters, employees: employees as Option[] }} selectedId={selectedId} disabled={!canEdit || busy} />)}
          </Box>
          </Box>)}
          {canEdit && <Button type="submit" variant="contained" disabled={busy} sx={{ mt: 3 }}>{busy ? 'Saving…' : 'Save employee'}</Button>}
        </Box>
        {selectedId && <EmployeeDocuments session={session} membership={membership} employeeId={selectedId} canEdit={canEdit} />}
      </Paper>
    </Stack>
  </Stack>
}

const documentTypes = [
  'PHOTOGRAPH', 'APPOINTMENT_LETTER', 'IDENTITY_PROOF', 'ADDRESS_PROOF', 'PAN',
  'BANK_PROOF', 'EDUCATION_CERTIFICATE', 'PREVIOUS_EMPLOYMENT', 'SIGNED_POLICY', 'OTHER',
] as const

function EmployeeDocuments({ session, membership, employeeId, canEdit }: { session: Session; membership: TenantMembership; employeeId: string; canEdit: boolean }) {
  const [documents, setDocuments] = useState<EmployeeDocument[]>([])
  const [documentType, setDocumentType] = useState<string>('IDENTITY_PROOF')
  const [file, setFile] = useState<File | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const loadDocuments = useCallback(async () => {
    setBusy(true); setError('')
    try { setDocuments(await apiJson<EmployeeDocument[]>(`/employees/${employeeId}/documents`, session, membership.tenantId)) }
    catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to load employee documents.') }
    finally { setBusy(false) }
  }, [employeeId, membership.tenantId, session])

  useEffect(() => { setFile(null); setNotice(''); void loadDocuments() }, [loadDocuments])

  async function upload(event: FormEvent) {
    event.preventDefault()
    if (!file) { setError('Choose a document to upload.'); return }
    if (file.size > 10 * 1024 * 1024) { setError('Document must not exceed 10 MB.'); return }
    setBusy(true); setError(''); setNotice('')
    try {
      const body = new FormData(); body.append('file', file)
      const response = await apiFetch(`/employees/${employeeId}/documents?documentType=${encodeURIComponent(documentType)}`,
        session, membership.tenantId, { method: 'POST', body })
      if (!response.ok) {
        const problem = await response.json().catch(() => ({})) as { message?: string }
        throw new Error(problem.message || `Upload failed (${response.status}).`)
      }
      setFile(null); setNotice('Document uploaded.'); await loadDocuments()
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to upload document.') }
    finally { setBusy(false) }
  }

  async function download(document: EmployeeDocument) {
    setBusy(true); setError('')
    try {
      const response = await apiFetch(`/employees/${employeeId}/documents/${document.id}/content`, session, membership.tenantId)
      if (!response.ok) throw new Error(`Download failed (${response.status}).`)
      const url = URL.createObjectURL(await response.blob())
      const anchor = window.document.createElement('a'); anchor.href = url; anchor.download = document.fileName; anchor.click()
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Unable to download document.') }
    finally { setBusy(false) }
  }

  return <Box sx={{ mt: 4, pt: 3, borderTop: 1, borderColor: 'divider' }}>
    <Typography variant="h6" gutterBottom>Documents</Typography>
    {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
    {notice && <Alert severity="success" sx={{ mb: 2 }}>{notice}</Alert>}
    {canEdit && <Stack component="form" onSubmit={upload} direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} sx={{ mb: 2 }}>
      <TextField select size="small" label="Document type" value={documentType} onChange={(event) => setDocumentType(event.target.value)} sx={{ minWidth: 210 }}>
        {documentTypes.map((type) => <MenuItem key={type} value={type}>{label(type)}</MenuItem>)}
      </TextField>
      <Button component="label" variant="outlined" disabled={busy}>Choose file<input hidden type="file" onChange={(event) => { setFile(event.target.files?.[0] ?? null); event.target.value = '' }} /></Button>
      <Typography variant="body2" color="text.secondary" sx={{ flexGrow: 1 }}>{file?.name ?? 'Maximum 10 MB'}</Typography>
      <Button type="submit" variant="contained" disabled={busy || !file}>{busy ? 'Uploading…' : 'Upload'}</Button>
    </Stack>}
    <Table size="small"><TableHead><TableRow><TableCell>Type</TableCell><TableCell>File</TableCell><TableCell>Size</TableCell><TableCell>Uploaded</TableCell><TableCell /></TableRow></TableHead>
      <TableBody>{documents.map((document) => <TableRow key={document.id}>
        <TableCell>{label(document.documentType)}</TableCell><TableCell>{document.fileName}</TableCell>
        <TableCell>{formatBytes(document.sizeBytes)}</TableCell><TableCell>{new Date(document.uploadedAt).toLocaleDateString()}</TableCell>
        <TableCell align="right"><Button size="small" disabled={busy} onClick={() => void download(document)}>Download</Button></TableCell>
      </TableRow>)}</TableBody></Table>
    {!busy && !documents.length && <Typography color="text.secondary" sx={{ py: 2 }}>No documents uploaded for this employee.</Typography>}
    {busy && !documents.length && <Box sx={{ display: 'grid', placeItems: 'center', py: 2 }}><CircularProgress size={24} /></Box>}
  </Box>
}

function EmployeeField({ field, value, setValue, masters, selectedId, disabled }: { field: Field; value: unknown; setValue: (key: string, value: string) => void; masters: Masters; selectedId: string; disabled: boolean }) {
  const required = ['employeeNumber','employmentStatus','employmentType','firstName','lastName','joiningDate'].includes(field.key)
  if (field.type === 'select') {
    const options = field.relation ? ((masters[field.relation] ?? []) as Option[]).filter((item) => item.id !== selectedId) : []
    return <TextField select label={field.label} value={String(value ?? '')} required={required} disabled={disabled} onChange={(event) => setValue(field.key,event.target.value)}>
      {!required && <MenuItem value=""><em>None</em></MenuItem>}
      {field.options?.map((option) => <MenuItem key={option} value={option}>{label(option)}</MenuItem>)}
      {options.map((option) => <MenuItem key={option.id} value={option.id}>{optionLabel(option)}</MenuItem>)}
    </TextField>
  }
  return <TextField label={field.label} type={field.type ?? 'text'} value={String(value ?? '')} required={required} disabled={disabled}
    onChange={(event) => setValue(field.key,event.target.value)} InputLabelProps={field.type === 'date' ? { shrink: true } : undefined}
    multiline={field.key.toLowerCase().includes('address') || field.key === 'exitReason'} />
}

function optionLabel(option: Option) { return option.name ?? option.title ?? [option.employeeNumber, option.firstName, option.lastName].filter(Boolean).join(' ') ?? option.id }
function label(value: string) { return value.toLowerCase().replaceAll('_',' ').replace(/^./, (character) => character.toUpperCase()) }
function formatBytes(bytes: number) { return bytes < 1024 ? `${bytes} B` : bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB` }
