import {useCallback,useEffect,useState} from 'react'
import type {Session} from '@supabase/supabase-js'
import {Alert,Button,Dialog,DialogActions,DialogContent,DialogTitle,MenuItem,Paper,Stack,TextField,Typography} from '@mui/material'
import {apiJson,type TenantMembership} from './api'

type Props={session:Session;membership:TenantMembership}
type Inbox={requestId:string;stepId:string;employeeNumber:string;employeeName:string;leaveType:string;startDate:string;endDate:string;days:number;reason:string;stepNumber:number;approverType:string}
type Notification={id:string;requestId:string;message:string;createdAt:string;readAt:string|null}
type Step={id:string;stepNumber:number;approverType:string;status:string;decidedBy:string|null;decidedAt:string|null;comment:string|null}
const choices=['REPORTING_MANAGER','HR_MANAGER','COMPANY_ADMIN']
const label=(value:string)=>value.replaceAll('_',' ').toLowerCase().replace(/^./,c=>c.toUpperCase())
const message=(e:unknown)=>e instanceof Error?e.message:'Unable to complete this action.'

export default function LeaveApprovals({session,membership}:Props){
  const [inbox,setInbox]=useState<Inbox[]>([]),[notifications,setNotifications]=useState<Notification[]>([]),[stages,setStages]=useState<string[]>([])
  const [busy,setBusy]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState(''),[comments,setComments]=useState<Record<string,string>>({})
  const canConfigure=membership.roles.some(r=>['SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN','HR_MANAGER'].includes(r))
  const load=useCallback(async()=>{
    setBusy(true);setError('')
    try{
      const [rows,alerts,policy]=await Promise.all([
        apiJson<Inbox[]>('/leave/approval-inbox',session,membership.tenantId),
        apiJson<Notification[]>('/leave/notifications',session,membership.tenantId),
        apiJson<{stages:string[]}>('/leave/approval-policy',session,membership.tenantId)])
      setInbox(rows);setNotifications(alerts);setStages(policy.stages)
    }catch(e){setError(message(e))}finally{setBusy(false)}
  },[session,membership.tenantId])
  useEffect(()=>{void load()},[load])
  async function decide(row:Inbox,decision:string){
    setBusy(true);setError('');setNotice('')
    try{
      const result=await apiJson<{status:string}>(`/leave/requests/${row.requestId}/decision`,session,membership.tenantId,{method:'POST',body:JSON.stringify({decision,comment:comments[row.stepId]??'',stepId:row.stepId})})
      await load();setNotice(result.status==='PENDING'?'Stage approved. The request is awaiting the next approver.':`Leave request ${result.status.toLowerCase()}.`)
    }catch(e){setError(message(e))}finally{setBusy(false)}
  }
  async function save(){
    setBusy(true);setError('');setNotice('')
    try{await apiJson('/leave/approval-policy',session,membership.tenantId,{method:'PUT',body:JSON.stringify({stages})});setNotice('Approval chain saved for new requests. Existing requests retain their original chain.')}
    catch(e){setError(message(e))}finally{setBusy(false)}
  }
  async function read(id:string){
    setBusy(true);setError('')
    try{await apiJson(`/leave/notifications/${id}/read`,session,membership.tenantId,{method:'POST'});await load()}
    catch(e){setError(message(e))}finally{setBusy(false)}
  }
  return <Stack spacing={3}>
    <Stack direction="row" spacing={2} alignItems="center"><Typography variant="h5">Leave approvals & notifications</Typography><Button disabled={busy} onClick={()=>void load()}>Refresh</Button></Stack>
    {busy&&<Typography role="status">Loading…</Typography>}{error&&<Alert severity="error">{error}</Alert>}{notice&&<Alert severity="success">{notice}</Alert>}
    <Paper sx={{p:3}}><Typography variant="h6">Awaiting my approval</Typography>
      {!busy&&!inbox.length&&<Typography>No requests awaiting your approval.</Typography>}
      {inbox.map(row=><Stack key={row.stepId} spacing={1} sx={{py:2,borderBottom:1,borderColor:'divider'}}>
        <Typography fontWeight={600}>{row.employeeNumber} — {row.employeeName}: {row.leaveType}</Typography>
        <Typography>{row.startDate} to {row.endDate} ({row.days} days). Stage {row.stepNumber}: {label(row.approverType)}</Typography>
        <Typography>{row.reason}</Typography>
        <TextField label="Decision comment (required to reject)" multiline inputProps={{maxLength:1000}} value={comments[row.stepId]??''} onChange={e=>setComments({...comments,[row.stepId]:e.target.value})}/>
        <Stack direction="row" spacing={1}><Button disabled={busy} variant="contained" onClick={()=>void decide(row,'APPROVED')}>Approve stage</Button><Button disabled={busy||!comments[row.stepId]?.trim()} color="error" onClick={()=>void decide(row,'REJECTED')}>Reject</Button><LeaveHistory session={session} membership={membership} requestId={row.requestId}/></Stack>
      </Stack>)}
    </Paper>
    <Paper sx={{p:3}}><Typography variant="h6">My notifications ({notifications.filter(n=>!n.readAt).length} unread)</Typography>
      {!busy&&!notifications.length&&<Typography>No notifications.</Typography>}
      {notifications.map(n=><Stack key={n.id} spacing={1} sx={{py:1,borderBottom:1,borderColor:'divider'}}>
        <Typography fontWeight={n.readAt?400:700}>{n.message}</Typography><Typography variant="caption">{new Date(n.createdAt).toLocaleString()}</Typography>
        <Stack direction="row"><LeaveHistory session={session} membership={membership} requestId={n.requestId}/>{!n.readAt&&<Button disabled={busy} onClick={()=>void read(n.id)}>Mark read</Button>}</Stack>
      </Stack>)}
    </Paper>
    {canConfigure&&<Paper sx={{p:3}}><Stack spacing={2}><Typography variant="h6">Company approval chain</Typography>
      <Typography>Stages run in order. The reporting manager must have a linked employee account with active company access. HR stages allow HR staff and company/group/system administrators. Company administrator stages allow company/group/system administrators. The requester and leave owner cannot approve their own request.</Typography>
      {stages.map((stage,index)=><Stack key={index} direction="row" spacing={1}><TextField fullWidth select label={`Stage ${index+1}`} value={stage} onChange={e=>setStages(stages.map((s,i)=>i===index?e.target.value:s))}>{choices.map(c=><MenuItem key={c} value={c} disabled={stages.includes(c)&&c!==stage}>{label(c)}</MenuItem>)}</TextField><Button disabled={busy||stages.length===1} onClick={()=>setStages(stages.filter((_,i)=>i!==index))}>Remove</Button></Stack>)}
      <Stack direction="row"><Button disabled={busy||stages.length>=3} onClick={()=>setStages([...stages,choices.find(c=>!stages.includes(c))!])}>Add stage</Button><Button disabled={busy||!stages.length} variant="contained" onClick={()=>void save()}>Save chain</Button></Stack>
    </Stack></Paper>}
  </Stack>
}

export function LeaveHistory({session,membership,requestId}:Props&{requestId:string}){
  const [open,setOpen]=useState(false),[rows,setRows]=useState<Step[]>([]),[error,setError]=useState(''),[busy,setBusy]=useState(false)
  useEffect(()=>{
    if(!open)return
    let active=true;setBusy(true);setError('');setRows([])
    void apiJson<Step[]>(`/leave/requests/${requestId}/history`,session,membership.tenantId)
      .then(result=>{if(active)setRows(result)}).catch(e=>{if(active)setError(message(e))}).finally(()=>{if(active)setBusy(false)})
    return()=>{active=false}
  },[open,requestId,session,membership.tenantId])
  return <><Button onClick={()=>setOpen(true)}>Approval history</Button><Dialog open={open} onClose={()=>setOpen(false)} fullWidth maxWidth="sm"><DialogTitle>Leave approval history</DialogTitle><DialogContent><Stack spacing={2}>
    {busy&&<Typography>Loading…</Typography>}{error&&<Alert severity="error">{error}</Alert>}
    {!busy&&!error&&!rows.length&&<Typography>No staged history is available for this older request.</Typography>}
    {rows.map(s=><Stack key={s.id}><Typography>Stage {s.stepNumber}: {label(s.approverType)} — {label(s.status)}</Typography>{s.decidedAt&&<Typography variant="body2">{new Date(s.decidedAt).toLocaleString()} · {s.decidedBy}</Typography>}{s.comment&&<Typography>{s.comment}</Typography>}</Stack>)}
  </Stack></DialogContent><DialogActions><Button onClick={()=>setOpen(false)}>Close</Button></DialogActions></Dialog></>
}
