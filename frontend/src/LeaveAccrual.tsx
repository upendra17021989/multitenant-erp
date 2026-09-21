import {useEffect,useState} from 'react'
import type {Session} from '@supabase/supabase-js'
import {Alert,Button,Paper,Stack,TextField,Typography} from '@mui/material'
import {apiJson,type TenantMembership} from './api'
type Event={employmentId:string;leaveTypeId:string;leaveYear:number;eventType:string;periodEnd:string;amount:number}
export default function LeaveAccrual({session,membership}:{session:Session;membership:TenantMembership}){
 const [start,setStart]=useState(''),[events,setEvents]=useState<Event[]>([]),[error,setError]=useState(''),[busy,setBusy]=useState(false),[revision,setRevision]=useState(0)
 useEffect(()=>{let active=true;setBusy(true);Promise.all([apiJson<{accrualStart:string|null}>('/leave/accrual',session,membership.tenantId),apiJson<Event[]>('/leave/accrual/events',session,membership.tenantId)]).then(([settings,rows])=>{if(active){setStart(settings.accrualStart??'');setEvents(rows);setError('')}}).catch((e:unknown)=>{if(active)setError(e instanceof Error?e.message:'Unable to load accrual settings.')}).finally(()=>{if(active)setBusy(false)});return()=>{active=false}},[session,membership.tenantId,revision])
 async function act(path:string,method:string,body?:unknown){setBusy(true);try{await apiJson(path,session,membership.tenantId,{method,body:body?JSON.stringify(body):undefined});setRevision(v=>v+1)}catch(e){setError(e instanceof Error?e.message:'Operation failed.');setBusy(false)}}
 return <Paper sx={{p:3}}><Stack spacing={2}><Typography variant="h6">Automatic leave accrual</Typography>
  <Typography>Choose the first date not already included in opening balances. The hourly job credits completed monthly, quarterly or annual periods, prorated by calendar service days. Year-end carry-forward is capped by each leave type. Reruns do not duplicate credits. The start date cannot change after the first credit.</Typography>
  {error&&<Alert severity="error">{error}</Alert>}
  <TextField type="date" label="Accrual start (blank disables initial activation)" InputLabelProps={{shrink:true}} value={start} disabled={busy||events.length>0} onChange={e=>setStart(e.target.value)}/>
  <Stack direction="row"><Button disabled={busy||events.length>0} onClick={()=>void act('/leave/accrual','PUT',{accrualStart:start||null})}>Save start date</Button><Button disabled={busy||!start} onClick={()=>void act('/leave/accrual/process','POST')}>Process completed periods</Button></Stack>
  <Typography>{events.length} processing events recorded</Typography>
  {events.slice(0,100).map((event,i)=><Typography variant="body2" key={i}>{event.periodEnd} · {event.eventType} · {event.amount} days · Employee {event.employmentId}</Typography>)}
 </Stack></Paper>
}
