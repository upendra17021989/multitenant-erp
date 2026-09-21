import {useEffect,useState,type FormEvent} from 'react'
import type {Session} from '@supabase/supabase-js'
import {Alert,Button,MenuItem,Paper,Stack,TextField,Typography} from '@mui/material'
import {apiJson,type TenantMembership} from './api'
type Review={id:string;attendanceId:string;reviewType:string;status:string;reason:string;overtimeMinutes:number;requestedBy:string;comment?:string}
type Attendance={id:string;employmentId:string;attendanceDate:string;status:string;overtimeMinutes:number}
export default function AttendanceReviews({session,membership}:{session:Session;membership:TenantMembership}) {
 const [rows,setRows]=useState<Review[]>([]),[records,setRecords]=useState<Attendance[]>([]),[error,setError]=useState(''),[busy,setBusy]=useState(false)
 const [form,setForm]=useState({attendanceId:'',reviewType:'OVERTIME',checkIn:'',checkOut:'',overtimeMinutes:0,reason:''})
 const [comment,setComment]=useState(''),[revision,setRevision]=useState(0),[month,setMonth]=useState(new Date().toISOString().slice(0,7))
 const [zone,setZone]=useState(''),[savedZone,setSavedZone]=useState('')
 const admin=membership.roles.some(r=>['SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN'].includes(r))
 const canDecide=admin||membership.roles.includes('HR_MANAGER')
 useEffect(()=>{let active=true;setBusy(true);setError('');const from=`${month}-01`,last=new Date(Number(month.slice(0,4)),Number(month.slice(5,7)),0).getDate();
  Promise.all([apiJson<Review[]>('/attendance/reviews',session,membership.tenantId),apiJson<Attendance[]>(`/attendance/records?from=${from}&to=${month}-${last}`,session,membership.tenantId),apiJson<{timeZone:string}>('/organization/timezone',session,membership.tenantId)])
  .then(([reviews,attendance,timezone])=>{if(active){setRows(reviews);setRecords(attendance);setZone(timezone.timeZone);setSavedZone(timezone.timeZone)}}).catch((e:unknown)=>{if(active)setError(message(e))}).finally(()=>{if(active)setBusy(false)});return()=>{active=false}
 },[session,membership.tenantId,revision,month])
 async function act(path:string,body:unknown,method='POST'){setBusy(true);setError('');try{await apiJson(path,session,membership.tenantId,{method,body:JSON.stringify(body)});setRevision(v=>v+1)}catch(e){setError(message(e));setBusy(false)}}
 function submit(event:FormEvent){event.preventDefault();void act('/attendance/reviews',{...form,checkIn:form.checkIn||null,checkOut:form.checkOut||null})}
 return <Stack spacing={2}>
  <Typography variant="h6">Attendance corrections and overtime approvals</Typography>
  <Typography>Payroll uses approved overtime only. A different user must approve each request. Editing attendance invalidates its overtime approval.</Typography>
  {error&&<Alert severity="error">{error}</Alert>}
  <Paper sx={{p:2}}><Stack direction="row" spacing={2}><TextField label="Company timezone" value={zone} disabled={!admin} onChange={e=>setZone(e.target.value)}/>{admin&&<Button disabled={busy||zone===savedZone} onClick={()=>void act('/organization/timezone',{timeZone:zone},'PUT')}>Save timezone</Button>}</Stack></Paper>
  <TextField type="month" label="Attendance month" value={month} onChange={e=>{if(e.target.value)setMonth(e.target.value)}}/>
  <Paper component="form" onSubmit={submit} sx={{p:2}}><Stack spacing={2}>
   <TextField required select label="Attendance record" value={form.attendanceId} onChange={e=>setForm({...form,attendanceId:e.target.value})}>{records.map(r=><MenuItem key={r.id} value={r.id}>{r.attendanceDate} · {r.employmentId} · {r.status} · OT {r.overtimeMinutes} min</MenuItem>)}</TextField>
   <TextField select label="Review type" value={form.reviewType} onChange={e=>setForm({...form,reviewType:e.target.value})}><MenuItem value="OVERTIME">Overtime</MenuItem><MenuItem value="CORRECTION">Punch correction</MenuItem></TextField>
   {form.reviewType==='OVERTIME'?<TextField required type="number" label="Overtime minutes" inputProps={{min:1}} value={form.overtimeMinutes} onChange={e=>setForm({...form,overtimeMinutes:Number(e.target.value)})}/>:<><TextField required label="Correct check-in (ISO timestamp with timezone)" placeholder="2026-09-21T09:00:00+05:30" value={form.checkIn} onChange={e=>setForm({...form,checkIn:e.target.value})}/><TextField required label="Correct check-out (ISO timestamp with timezone)" placeholder="2026-09-21T18:00:00+05:30" value={form.checkOut} onChange={e=>setForm({...form,checkOut:e.target.value})}/></>}
   <TextField required label="Reason" inputProps={{maxLength:1000}} value={form.reason} onChange={e=>setForm({...form,reason:e.target.value})}/><Button disabled={busy} type="submit">Submit for approval</Button>
  </Stack></Paper>
  {canDecide&&<TextField label="Decision comment (required)" value={comment} onChange={e=>setComment(e.target.value)}/>}
  {rows.map(r=><Paper key={r.id} sx={{p:2}}><Typography>{r.reviewType} · {r.status} · {r.reason}</Typography><Typography variant="caption">Requested by {r.requestedBy} · {r.comment}</Typography>
   {canDecide&&r.status==='PENDING'&&<Stack direction="row">{['APPROVED','REJECTED'].map(decision=><Button key={decision} disabled={busy||!comment.trim()||r.requestedBy===session.user.id} onClick={()=>void act(`/attendance/reviews/${r.id}/decision`,{decision,comment})}>{decision==='APPROVED'?'Approve':'Reject'}</Button>)}</Stack>}
  </Paper>)}
 </Stack>
}
function message(e:unknown){return e instanceof Error?e.message:'Operation failed.'}
