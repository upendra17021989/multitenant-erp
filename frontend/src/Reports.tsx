import {useEffect, useState} from 'react'
import type {Session} from '@supabase/supabase-js'
import {Alert, Button, MenuItem, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography} from '@mui/material'
import {apiFetch, apiJson, type TenantMembership} from './api'

type Props = {session:Session; membership:TenantMembership}
type Report = {company:string; tenantId:string; from:string; to:string; columns:string[]; rows:string[][]}
const payrollRoles = ['SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','PAYROLL_MANAGER','PAYROLL_EXECUTIVE']
const hrReports = ['employees','employee-movements','attendance','attendance-summary','attendance-exceptions','leave-balances','leave-transactions']
const payrollReports = ['salary-register','payroll-components','payroll-adjustments','department-payroll','bank-statement','payslip-register']
const label = (value:string) => value.replaceAll('-', ' ').replaceAll('_', ' ')
export default function Reports({session,membership}:Props) {
  const now = new Date()
  const [name,setName] = useState('employees')
  const [from,setFrom] = useState(`${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-01`)
  const [to,setTo] = useState(localDate(now))
  const [report,setReport] = useState<Report|null>(null)
  const [error,setError] = useState('')
  const [busy,setBusy] = useState(false)
  const names = membership.roles.some(r=>payrollRoles.includes(r)) ? [...hrReports,...payrollReports] : hrReports
  async function load() {
    setBusy(true); setError(''); setReport(null)
    try {setReport(await apiJson<Report>(`/reports/${name}?from=${from}&to=${to}`,session,membership.tenantId))}
    catch(e) {setError(e instanceof Error ? e.message : 'Unable to load report.')} finally {setBusy(false)}
  }
  async function download() {
    setBusy(true); setError('')
    try {
      const response=await apiFetch(`/reports/${name}/csv?from=${from}&to=${to}`,session,membership.tenantId)
      if(!response.ok) throw new Error(`Export failed (${response.status}).`)
      const url=URL.createObjectURL(await response.blob()),anchor=document.createElement('a')
      anchor.href=url; anchor.download=`${membership.companyCode}-${name}-${from}-${to}.csv`; anchor.click()
      window.setTimeout(()=>URL.revokeObjectURL(url),1000)
    } catch(e) {setError(e instanceof Error ? e.message : 'Export failed.')} finally {setBusy(false)}
  }
  return <Stack spacing={2}>
    <Typography variant="h5">Reports — {membership.legalName}</Typography>
    <Typography>Payroll reports include approved, locked and paid runs. Bank statement is a review export; confirm your bank's upload format before payment. Department grouping uses current employee assignments.</Typography>
    {error&&<Alert severity="error">{error}</Alert>}
    <Stack direction={{xs:'column',md:'row'}} spacing={2}>
      <TextField select label="Report" value={name} onChange={e=>{setName(e.target.value);setReport(null)}} sx={{minWidth:240}}>{names.map(n=><MenuItem key={n} value={n}>{label(n)}</MenuItem>)}</TextField>
      <TextField type="date" label="From" InputLabelProps={{shrink:true}} value={from} onChange={e=>{setFrom(e.target.value);setReport(null)}}/>
      <TextField type="date" label="To" InputLabelProps={{shrink:true}} value={to} onChange={e=>{setTo(e.target.value);setReport(null)}}/>
      <Button disabled={busy||!from||!to} onClick={()=>void load()}>View</Button>
      <Button disabled={busy||!from||!to} onClick={()=>void download()}>Download CSV</Button>
    </Stack>
    {report&&<><Typography>{report.company} · {report.from} to {report.to} · {report.rows.length} rows</Typography>
      {!report.rows.length?<Alert severity="info">No matching records.</Alert>:<Paper sx={{overflow:'auto'}}><Table size="small"><TableHead><TableRow>{report.columns.map(c=><TableCell key={c}>{label(c)}</TableCell>)}</TableRow></TableHead><TableBody>{report.rows.map((row,i)=><TableRow key={i}>{row.map((cell,j)=><TableCell key={j}>{cell}</TableCell>)}</TableRow>)}</TableBody></Table></Paper>}</>}
  </Stack>
}
export function LiveDashboard({session,membership}:Props) {
  const [data,setData]=useState<Record<string,string|number>|null>(null),[error,setError]=useState('')
  const [date,setDate]=useState(localDate(new Date()))
  useEffect(()=>{
    let active=true; setData(null); setError('')
    apiJson<Record<string,string|number>>(`/reports/dashboard?date=${date}`,session,membership.tenantId)
      .then(value=>{if(active)setData(value)}).catch((e:unknown)=>{if(active)setError(e instanceof Error?e.message:'Unable to load dashboard.')})
    return ()=>{active=false}
  },[session,membership.tenantId,date])
  const labels:Record<string,string>={employees:'Total employees',activeEmployees:'Active employees',attendanceToday:'Attendance recorded',onLeave:'Employees on approved leave',pendingLeave:'Pending leave approvals',monthlyPayrollCost:'Approved payroll cost for month'}
  return <Stack spacing={2}><Typography variant="h5">{membership.legalName}</Typography>
    <TextField type="date" label="Dashboard date" InputLabelProps={{shrink:true}} value={date} onChange={e=>{if(e.target.value)setDate(e.target.value)}}/>
    <Typography>Workforce and pending counts reflect current records. Attendance and leave use the selected date; payroll cost uses its month.</Typography>
    {error&&<Alert severity="error">{error}</Alert>}
    <div className="wf-kpis">{data&&Object.entries(labels).filter(([key])=>key in data).map(([key,title])=><div className="wf-kpi" key={key}><div className="wf-label">{title}</div><div className="wf-value">{data[key]}</div></div>)}</div>
  </Stack>
}
function localDate(date:Date) {return `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`}
