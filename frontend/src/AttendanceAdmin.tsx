import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import type { Session } from '@supabase/supabase-js'
import {
  Alert, Box, Button, Chip, CircularProgress, MenuItem, Paper, Stack, Tab, Table,
  TableBody, TableCell, TableHead, TableRow, Tabs, TextField, Typography,
} from '@mui/material'
import { apiFetch, apiJson, type TenantMembership } from './api'

type Employee = { id: string; employeeNumber: string; firstName: string; lastName: string }
type Shift = { id: string; code: string; name: string; startTime: string; endTime: string; breakMinutes: number; graceInMinutes: number; graceOutMinutes: number; fullDayMinutes: number; halfDayMinutes: number; effectiveFrom: string; effectiveTo?: string; status: string }
type Attendance = { id: string; employmentId: string; attendanceDate: string; shiftId?: string; status: string; checkIn?: string; checkOut?: string; workedMinutes?: number; overtimeMinutes: number; source: string; notes?: string }
type ImportResult = { totalRows: number; acceptedRows: number; rejectedRows: number; rows: { rowNumber: number; accepted: boolean; error?: string }[] }
type Section = 'records' | 'shifts' | 'assignments' | 'import'

const editRoles = new Set(['SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER','HR_EXECUTIVE'])
const lockRoles = new Set(['SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER','PAYROLL_MANAGER'])
const attendanceStatuses = ['PRESENT','ABSENT','HALF_DAY','LEAVE','HOLIDAY','WEEKLY_OFF','WORK_FROM_HOME','ON_DUTY','MISSING_PUNCH']
const today = new Date().toISOString().slice(0,10)
const monthStart = `${today.slice(0,7)}-01`

const emptyShift = { code:'', name:'', startTime:'09:00', endTime:'18:00', breakMinutes:60, graceInMinutes:10, graceOutMinutes:10, fullDayMinutes:480, halfDayMinutes:240, effectiveFrom:today, effectiveTo:'', status:'ACTIVE' }
const emptyRecord = { employmentId:'', attendanceDate:today, shiftId:'', status:'PRESENT', checkIn:'', checkOut:'', notes:'' }

export default function AttendanceAdmin({session,membership}:{session:Session;membership:TenantMembership}) {
  const [section,setSection]=useState<Section>('records')
  const [employees,setEmployees]=useState<Employee[]>([])
  const [shifts,setShifts]=useState<Shift[]>([])
  const [records,setRecords]=useState<Attendance[]>([])
  const [from,setFrom]=useState(monthStart)
  const [to,setTo]=useState(today)
  const [employeeFilter,setEmployeeFilter]=useState('')
  const [shiftForm,setShiftForm]=useState<Record<string,string|number>>(emptyShift)
  const [shiftId,setShiftId]=useState('')
  const [recordForm,setRecordForm]=useState<Record<string,string>>(emptyRecord)
  const [recordId,setRecordId]=useState('')
  const [assignment,setAssignment]=useState({employmentId:'',shiftId:'',effectiveFrom:today,effectiveTo:''})
  const [lockMonth,setLockMonth]=useState(today.slice(0,7))
  const [file,setFile]=useState<File|null>(null)
  const [importResult,setImportResult]=useState<ImportResult|null>(null)
  const [busy,setBusy]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState('')
  const canEdit=membership.roles.some(role=>editRoles.has(role)), canLock=membership.roles.some(role=>lockRoles.has(role))
  const employeeNames=useMemo(()=>new Map(employees.map(e=>[e.id,`${e.employeeNumber} - ${e.firstName} ${e.lastName}`])),[employees])
  const shiftNames=useMemo(()=>new Map(shifts.map(s=>[s.id,`${s.code} - ${s.name}`])),[shifts])

  const loadMasters=useCallback(async()=>{
    const [employeeRows,shiftRows]=await Promise.all([
      apiJson<Employee[]>('/employees?status=ACTIVE',session,membership.tenantId),
      apiJson<Shift[]>('/attendance/shifts',session,membership.tenantId),
    ])
    setEmployees(employeeRows);setShifts(shiftRows)
  },[membership.tenantId,session])
  const loadRecords=useCallback(async()=>{
    const query=new URLSearchParams({from,to});if(employeeFilter)query.set('employmentId',employeeFilter)
    setRecords(await apiJson<Attendance[]>(`/attendance/records?${query}`,session,membership.tenantId))
  },[employeeFilter,from,membership.tenantId,session,to])
  const refresh=useCallback(async()=>{setBusy(true);setError('');try{await Promise.all([loadMasters(),loadRecords()])}catch(e){setError(message(e))}finally{setBusy(false)}},[loadMasters,loadRecords])
  useEffect(()=>{void refresh()},[refresh])

  async function saveShift(event:FormEvent){event.preventDefault();await action(async()=>{
    const payload={...shiftForm,effectiveTo:shiftForm.effectiveTo||null}
    await apiJson(`/attendance/shifts${shiftId?`/${shiftId}`:''}`,session,membership.tenantId,{method:shiftId?'PUT':'POST',body:JSON.stringify(payload)})
    setShiftId('');setShiftForm(emptyShift);await loadMasters();return `Shift ${shiftId?'updated':'created'}.`
  })}
  async function saveRecord(event:FormEvent){event.preventDefault();await action(async()=>{
    const payload={...recordForm,shiftId:recordForm.shiftId||null,checkIn:iso(recordForm.checkIn),checkOut:iso(recordForm.checkOut),workedMinutes:null,overtimeMinutes:0,source:'MANUAL'}
    await apiJson(`/attendance/records${recordId?`/${recordId}`:''}`,session,membership.tenantId,{method:recordId?'PUT':'POST',body:JSON.stringify(payload)})
    setRecordId('');setRecordForm(emptyRecord);await loadRecords();return `Attendance ${recordId?'updated':'created'}.`
  })}
  async function assignShift(event:FormEvent){event.preventDefault();await action(async()=>{
    await apiJson('/attendance/shift-assignments',session,membership.tenantId,{method:'POST',body:JSON.stringify({...assignment,effectiveTo:assignment.effectiveTo||null})})
    setAssignment({employmentId:'',shiftId:'',effectiveFrom:today,effectiveTo:''});return 'Shift assigned.'
  })}
  async function changeLock(lock:boolean){await action(async()=>{
    const response=await apiFetch(`/attendance/months/${lockMonth}/${'lock'}`,session,membership.tenantId,{method:lock?'POST':'DELETE'})
    if(!response.ok)throw new Error(await problem(response));return lock?'Attendance month locked.':'Attendance month reopened.'
  })}
  async function upload(event:FormEvent){event.preventDefault();if(!file){setError('Choose a CSV file.');return}await action(async()=>{
    const body=new FormData();body.append('file',file)
    const response=await apiFetch('/attendance/imports/csv',session,membership.tenantId,{method:'POST',body})
    if(!response.ok)throw new Error(await problem(response));const result=await response.json() as ImportResult
    setImportResult(result);setFile(null);await loadRecords();return `Import complete: ${result.acceptedRows} accepted, ${result.rejectedRows} rejected.`
  })}
  async function action(work:()=>Promise<string>){setBusy(true);setError('');setNotice('');try{setNotice(await work())}catch(e){setError(message(e))}finally{setBusy(false)}}

  function selectShift(row:Shift){setShiftId(row.id);setShiftForm({...row,effectiveTo:row.effectiveTo??''})}
  function selectRecord(row:Attendance){setRecordId(row.id);setRecordForm({employmentId:row.employmentId,attendanceDate:row.attendanceDate,shiftId:row.shiftId??'',status:row.status,checkIn:local(row.checkIn),checkOut:local(row.checkOut),notes:row.notes??''})}

  return <Stack spacing={3}>
    <Paper><Tabs value={section} onChange={(_e,value:Section)=>setSection(value)} variant="scrollable" scrollButtons="auto">
      <Tab value="records" label="Attendance"/><Tab value="shifts" label="Shifts"/><Tab value="assignments" label="Assignments"/><Tab value="import" label="CSV Import"/>
    </Tabs></Paper>
    {error&&<Alert severity="error">{error}</Alert>}{notice&&<Alert severity="success">{notice}</Alert>}
    {busy&&<Box sx={{display:'flex',justifyContent:'center'}}><CircularProgress size={28}/></Box>}
    {section==='records'&&<>
      <Paper sx={{p:2}}><Stack direction={{xs:'column',md:'row'}} spacing={2} alignItems={{md:'center'}}>
        <TextField type="date" label="From" value={from} onChange={e=>setFrom(e.target.value)} InputLabelProps={{shrink:true}}/>
        <TextField type="date" label="To" value={to} onChange={e=>setTo(e.target.value)} InputLabelProps={{shrink:true}}/>
        <SelectEmployee value={employeeFilter} employees={employees} label="Employee" all onChange={setEmployeeFilter}/><Button onClick={()=>void refresh()}>Refresh</Button>
        {canLock&&<><TextField type="month" label="Month" value={lockMonth} onChange={e=>setLockMonth(e.target.value)} InputLabelProps={{shrink:true}}/><Button color="warning" onClick={()=>void changeLock(true)}>Lock</Button><Button onClick={()=>void changeLock(false)}>Reopen</Button></>}
      </Stack></Paper>
      <Stack direction={{xs:'column',lg:'row'}} spacing={3} alignItems="flex-start">
        <Paper sx={{width:{xs:'100%',lg:'65%'},overflow:'auto'}}><Table size="small"><TableHead><TableRow><TableCell>Date</TableCell><TableCell>Employee</TableCell><TableCell>Shift</TableCell><TableCell>Status</TableCell><TableCell>Worked</TableCell><TableCell>OT</TableCell><TableCell>Source</TableCell></TableRow></TableHead><TableBody>
          {records.map(r=><TableRow hover key={r.id} onClick={()=>selectRecord(r)} sx={{cursor:'pointer'}}><TableCell>{r.attendanceDate}</TableCell><TableCell>{employeeNames.get(r.employmentId)??r.employmentId}</TableCell><TableCell>{r.shiftId?shiftNames.get(r.shiftId):'—'}</TableCell><TableCell><Chip size="small" label={label(r.status)}/></TableCell><TableCell>{minutes(r.workedMinutes)}</TableCell><TableCell>{minutes(r.overtimeMinutes)}</TableCell><TableCell>{label(r.source)}</TableCell></TableRow>)}
        </TableBody></Table>{!records.length&&<Typography color="text.secondary" sx={{p:3}}>No attendance records in this period.</Typography>}</Paper>
        <Paper component="form" onSubmit={saveRecord} sx={{p:3,width:{xs:'100%',lg:'35%'}}}><Typography variant="h6">{recordId?'Edit':'New'} attendance</Typography><Stack spacing={2} sx={{mt:2}}>
          <SelectEmployee value={recordForm.employmentId} employees={employees} label="Employee" onChange={value=>setRecordForm({...recordForm,employmentId:value})}/>
          <TextField type="date" label="Date" required value={recordForm.attendanceDate} onChange={e=>setRecordForm({...recordForm,attendanceDate:e.target.value})} InputLabelProps={{shrink:true}}/>
          <SelectShift value={recordForm.shiftId} shifts={shifts} onChange={value=>setRecordForm({...recordForm,shiftId:value})}/>
          <TextField select label="Status" value={recordForm.status} onChange={e=>setRecordForm({...recordForm,status:e.target.value})}>{attendanceStatuses.map(s=><MenuItem key={s} value={s}>{label(s)}</MenuItem>)}</TextField>
          <TextField type="datetime-local" label="Check in" value={recordForm.checkIn} onChange={e=>setRecordForm({...recordForm,checkIn:e.target.value})} InputLabelProps={{shrink:true}}/>
          <TextField type="datetime-local" label="Check out" value={recordForm.checkOut} onChange={e=>setRecordForm({...recordForm,checkOut:e.target.value})} InputLabelProps={{shrink:true}}/>
          <TextField label="Notes" value={recordForm.notes} onChange={e=>setRecordForm({...recordForm,notes:e.target.value})} multiline/>
          {canEdit&&<Button type="submit" variant="contained" disabled={busy||!recordForm.employmentId}>Save attendance</Button>}
        </Stack></Paper>
      </Stack></>}
    {section==='shifts'&&<Stack direction={{xs:'column',lg:'row'}} spacing={3} alignItems="flex-start">
      <Paper sx={{width:{xs:'100%',lg:'60%'},overflow:'auto'}}><Table size="small"><TableHead><TableRow><TableCell>Code</TableCell><TableCell>Name</TableCell><TableCell>Hours</TableCell><TableCell>Effective</TableCell><TableCell>Status</TableCell></TableRow></TableHead><TableBody>{shifts.map(s=><TableRow hover key={s.id} onClick={()=>selectShift(s)} sx={{cursor:'pointer'}}><TableCell>{s.code}</TableCell><TableCell>{s.name}</TableCell><TableCell>{s.startTime}–{s.endTime}</TableCell><TableCell>{s.effectiveFrom}{s.effectiveTo?` – ${s.effectiveTo}`:''}</TableCell><TableCell>{label(s.status)}</TableCell></TableRow>)}</TableBody></Table></Paper>
      <Paper component="form" onSubmit={saveShift} sx={{p:3,width:{xs:'100%',lg:'40%'}}}><Typography variant="h6">{shiftId?'Edit':'New'} shift</Typography><Box sx={{display:'grid',gridTemplateColumns:'1fr 1fr',gap:2,mt:2}}>
        {['code','name'].map(key=><TextField key={key} label={label(key)} required value={shiftForm[key]} onChange={e=>setShiftForm({...shiftForm,[key]:e.target.value})}/>)}
        {['startTime','endTime'].map(key=><TextField key={key} type="time" label={label(key)} required value={shiftForm[key]} onChange={e=>setShiftForm({...shiftForm,[key]:e.target.value})} InputLabelProps={{shrink:true}}/>)}
        {['breakMinutes','graceInMinutes','graceOutMinutes','fullDayMinutes','halfDayMinutes'].map(key=><TextField key={key} type="number" label={label(key)} value={shiftForm[key]} onChange={e=>setShiftForm({...shiftForm,[key]:Number(e.target.value)})}/>)}
        {['effectiveFrom','effectiveTo'].map(key=><TextField key={key} type="date" label={label(key)} required={key==='effectiveFrom'} value={shiftForm[key]} onChange={e=>setShiftForm({...shiftForm,[key]:e.target.value})} InputLabelProps={{shrink:true}}/>)}
        <TextField select label="Status" value={shiftForm.status} onChange={e=>setShiftForm({...shiftForm,status:e.target.value})}><MenuItem value="ACTIVE">Active</MenuItem><MenuItem value="INACTIVE">Inactive</MenuItem></TextField>
      </Box>{canEdit&&<Button type="submit" variant="contained" sx={{mt:2}}>Save shift</Button>}</Paper>
    </Stack>}
    {section==='assignments'&&<Paper component="form" onSubmit={assignShift} sx={{p:3,maxWidth:700}}><Typography variant="h6">Assign employee shift</Typography><Stack spacing={2} sx={{mt:2}}><SelectEmployee value={assignment.employmentId} employees={employees} label="Employee" onChange={value=>setAssignment({...assignment,employmentId:value})}/><SelectShift value={assignment.shiftId} shifts={shifts} onChange={value=>setAssignment({...assignment,shiftId:value})}/><TextField type="date" label="Effective from" required value={assignment.effectiveFrom} onChange={e=>setAssignment({...assignment,effectiveFrom:e.target.value})} InputLabelProps={{shrink:true}}/><TextField type="date" label="Effective to" value={assignment.effectiveTo} onChange={e=>setAssignment({...assignment,effectiveTo:e.target.value})} InputLabelProps={{shrink:true}}/>{canEdit&&<Button type="submit" variant="contained">Assign shift</Button>}</Stack></Paper>}
    {section==='import'&&<Paper component="form" onSubmit={upload} sx={{p:3}}><Typography variant="h6">Import attendance CSV</Typography><Typography color="text.secondary" sx={{my:1}}>Required: employeeNumber, attendanceDate. Optional: shiftCode, checkIn, checkOut, status, notes. Punches use ISO timestamps.</Typography><Stack direction={{xs:'column',sm:'row'}} spacing={2} alignItems={{sm:'center'}}><Button component="label" variant="outlined">Choose CSV<input hidden type="file" accept=".csv,text/csv" onChange={e=>setFile(e.target.files?.[0]??null)}/></Button><Typography sx={{flexGrow:1}}>{file?.name??'No file selected'}</Typography>{canEdit&&<Button type="submit" variant="contained" disabled={!file||busy}>Upload</Button>}</Stack>{importResult&&<Box sx={{mt:3}}><Alert severity={importResult.rejectedRows?'warning':'success'}>{importResult.acceptedRows} accepted; {importResult.rejectedRows} rejected.</Alert>{importResult.rows.filter(r=>!r.accepted).map(r=><Typography key={r.rowNumber} color="error" sx={{mt:1}}>Row {r.rowNumber}: {r.error}</Typography>)}</Box>}</Paper>}
  </Stack>
}

function SelectEmployee({value,employees,label:fieldLabel,all,onChange}:{value:string;employees:Employee[];label:string;all?:boolean;onChange:(value:string)=>void}){return <TextField select label={fieldLabel} value={value} required={!all} onChange={e=>onChange(e.target.value)} sx={{minWidth:220}}>{all&&<MenuItem value="">All employees</MenuItem>}{employees.map(e=><MenuItem key={e.id} value={e.id}>{e.employeeNumber} - {e.firstName} {e.lastName}</MenuItem>)}</TextField>}
function SelectShift({value,shifts,onChange}:{value:string;shifts:Shift[];onChange:(value:string)=>void}){return <TextField select label="Shift" value={value} onChange={e=>onChange(e.target.value)}><MenuItem value=""><em>None</em></MenuItem>{shifts.filter(s=>s.status==='ACTIVE').map(s=><MenuItem key={s.id} value={s.id}>{s.code} - {s.name}</MenuItem>)}</TextField>}
function label(value:string){return value.replace(/([a-z])([A-Z])/g,'$1 $2').toLowerCase().replaceAll('_',' ').replace(/^./,c=>c.toUpperCase())}
function minutes(value?:number){return value==null?'—':`${Math.floor(value/60)}h ${value%60}m`}
function iso(value:string){return value?new Date(value).toISOString():null}
function local(value?:string){if(!value)return '';const date=new Date(value);return new Date(date.getTime()-date.getTimezoneOffset()*60000).toISOString().slice(0,16)}
function message(error:unknown){return error instanceof Error?error.message:'Operation failed.'}
async function problem(response:Response){try{const body=await response.json() as {message?:string;error?:string};return body.message??body.error??`Request failed (${response.status}).`}catch{return `Request failed (${response.status}).`}}
