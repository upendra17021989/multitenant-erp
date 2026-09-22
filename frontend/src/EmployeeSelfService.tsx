import {LeaveHistory} from './LeaveApprovals'
import EmployeePayslips from './EmployeePayslips'
import {useEffect, useState, type FormEvent} from 'react'
import type {Session} from '@supabase/supabase-js'
import {Alert, Button, MenuItem, Paper, Stack, TextField, Typography} from '@mui/material'
import {apiFetch, apiJson, type TenantMembership} from './api'

type LeaveType = {id:string; name:string; status:string; supportingDocumentRequired:boolean}
type Balance = {id:string; leaveTypeId:string; leaveYear:number; available:number}
type Leave = {id:string; leaveTypeId:string; startDate:string; endDate:string; requestedDays:number; status:string}


export default function EmployeeSelfService({session, membership}:{session:Session; membership:TenantMembership}) {
  const [employmentId, setEmploymentId] = useState('')
  const [types, setTypes] = useState<LeaveType[]>([])
  const [balances, setBalances] = useState<Balance[]>([])
  const [requests, setRequests] = useState<Leave[]>([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(true)
  const [revision, setRevision] = useState(0)
  const [file, setFile] = useState<File|null>(null)
  const [form, setForm] = useState({leaveTypeId:'', startDate:'', endDate:'', requestedDays:1, reason:''})
  const tenant = membership.tenantId
  useEffect(() => {
    let active = true
    setBusy(true)
    Promise.all([
      apiJson<{employmentId:string}>('/self/employment', session, tenant),
      apiJson<LeaveType[]>('/leave/types', session, tenant),
    ]).then(async ([identity, leaveTypes]) => {
      const [balanceRows, requestRows] = await Promise.all([
        apiJson<Balance[]>(`/leave/balances?employmentId=${identity.employmentId}`, session, tenant),
        apiJson<Leave[]>(`/leave/requests?employmentId=${identity.employmentId}`, session, tenant),
      ])
      if (!active) return
      setEmploymentId(identity.employmentId); setTypes(leaveTypes)
      setBalances(balanceRows); setRequests(requestRows); setError('')
    }).catch((e:unknown) => {if(active) setError(message(e))})
      .finally(() => {if(active) setBusy(false)})
    return () => {active = false}
  }, [session, tenant, revision])
  async function mutate(path:string, body?:unknown) {
    setBusy(true); setError('')
    try {
      await apiJson(path, session, tenant, {method:'POST', body:body ? JSON.stringify(body) : undefined})
      setRevision(value => value + 1)
    } catch(e) {setError(message(e)); setBusy(false)}
  }
  async function submit(event:FormEvent) {
    event.preventDefault()
    setBusy(true); setError('')
    try {
      let supportingDocumentId:string|undefined
      if(file) {
        const body=new FormData();body.append('file',file)
        const response=await apiFetch('/self/documents/leave-support',session,tenant,{method:'POST',body})
        if(!response.ok)throw new Error(`Unable to upload supporting document (${response.status}).`)
        supportingDocumentId=(await response.json() as {id:string}).id
      }
      await mutate('/leave/requests', {...form, employmentId, supportingDocumentId})
    } catch(e) {setError(message(e));setBusy(false)}
  }
  const name = (id:string) => types.find(t => t.id === id)?.name ?? id
  return <Stack spacing={3}>
    <Typography variant="h5">My leave and payslips — {membership.legalName}</Typography>
    {error && <Alert severity="error">{error} If your employee profile is not linked, contact HR.</Alert>}
    {busy && <Typography role="status">Loading…</Typography>}
    <EmployeePayslips key={session.user.id + ':' + tenant} session={session} membership={membership} />
    <Paper sx={{p:3}}><Typography variant="h6">Leave balances</Typography>
      {!busy && !balances.length && <Typography>No leave balances configured.</Typography>}
      {balances.map(balance => <Typography key={balance.id}>{name(balance.leaveTypeId)} ({balance.leaveYear}): {balance.available} days available</Typography>)}
    </Paper>
    <Paper component="form" onSubmit={submit} sx={{p:3}}><Stack spacing={2}>
      <Typography variant="h6">Request leave</Typography>
      <TextField select required label="Leave type" value={form.leaveTypeId} onChange={e => setForm({...form, leaveTypeId:e.target.value})}>
        {types.filter(t => t.status === 'ACTIVE').map(t => <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>)}
      </TextField>
      <TextField required type="date" label="Start date" InputLabelProps={{shrink:true}} value={form.startDate} onChange={e => setForm({...form, startDate:e.target.value})}/>
      <TextField required type="date" label="End date" InputLabelProps={{shrink:true}} value={form.endDate} onChange={e => setForm({...form, endDate:e.target.value})}/>
      <TextField required type="number" label="Days" inputProps={{min:0.5,step:0.5}} value={form.requestedDays} onChange={e => setForm({...form, requestedDays:Number(e.target.value)})}/>
      <TextField required multiline label="Reason" inputProps={{maxLength:1000}} value={form.reason} onChange={e => setForm({...form, reason:e.target.value})}/>
      <Typography>Supporting document (maximum 10 MB)</Typography>
      <input aria-label="Supporting document" type="file" required={types.some(t=>t.id===form.leaveTypeId&&t.supportingDocumentRequired)} onChange={e=>setFile(e.target.files?.[0]??null)}/>
      <Button type="submit" variant="contained" disabled={busy || !employmentId}>Submit request</Button>
    </Stack></Paper>
    <Paper sx={{p:3}}><Typography variant="h6">My requests</Typography>
      {!busy && !requests.length && <Typography>No leave requests.</Typography>}
      {requests.map(request => <Stack key={request.id} direction="row" alignItems="center" spacing={2}>
        <Typography>{name(request.leaveTypeId)}: {request.startDate} to {request.endDate} ({request.requestedDays} days) — {request.status}</Typography>
        <LeaveHistory session={session} membership={membership} requestId={request.id}/>{request.status === 'PENDING' && <Button disabled={busy} onClick={() => void mutate(`/leave/requests/${request.id}/cancel`)}>Cancel</Button>}
      </Stack>)}
    </Paper>
  </Stack>
}
function message(error:unknown) {return error instanceof Error ? error.message : 'Operation failed.'}
