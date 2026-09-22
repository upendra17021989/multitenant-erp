import {useEffect,useRef,useState} from 'react'
import type {Session} from '@supabase/supabase-js'
import {Alert,Button,Dialog,DialogActions,DialogContent,DialogTitle,Stack,Typography} from '@mui/material'
import {apiJson,type TenantMembership} from './api'

type Attempt={id:string;recipient:string;requestedBy:string;requestedAt:string;completedAt:string|null;status:string;failureCode:string|null}
export default function PayslipEmailDelivery({session,membership,slip,canSend}:{session:Session;membership:TenantMembership;slip:{id:string;year:number;month:number;status:string};canSend:boolean}) {
  const [open,setOpen]=useState(false),[enabled,setEnabled]=useState(false),[busy,setBusy]=useState(false),[error,setError]=useState(''),[attempts,setAttempts]=useState<Attempt[]>([])
  const request=useRef<string|null>(null)
  const base=`/payroll/runs/${slip.year}/${slip.month}/payslips`
  useEffect(()=>{
    if(!open)return
    let active=true
    setBusy(true);setError('')
    void Promise.all([apiJson<{enabled:boolean}>(`${base}/email-settings`,session,membership.tenantId),apiJson<Attempt[]>(`${base}/${slip.id}/email-attempts`,session,membership.tenantId)])
      .then(([settings,rows])=>{if(active){setEnabled(settings.enabled);setAttempts(rows)}})
      .catch(e=>{if(active)setError(String(e instanceof Error?e.message:e))})
      .finally(()=>{if(active)setBusy(false)})
    return ()=>{active=false}
  },[open,base,slip.id,session,membership.tenantId])
  async function send(){
    setBusy(true);setError('')
    request.current??=crypto.randomUUID()
    try{
      const attempt=await apiJson<Attempt>(`${base}/${slip.id}/email`,session,membership.tenantId,{method:'POST',body:JSON.stringify({requestId:request.current})})
      setAttempts(rows=>[attempt,...rows.filter(row=>row.id!==attempt.id)])
      if(attempt.status!=='SENDING')request.current=null
    }catch(e){setError(String(e instanceof Error?e.message:e))}finally{setBusy(false)}
  }
  return <><Button size="small" onClick={()=>setOpen(true)}>Email & history</Button><Dialog open={open} onClose={()=>{if(!busy)setOpen(false)}} fullWidth maxWidth="sm">
    <DialogTitle>Payslip email delivery</DialogTitle><DialogContent><Stack spacing={2}>
      <Typography>The PDF will be sent to the employee’s recorded work email. Check that address in Employee Master before sending.</Typography>
      {error&&<Alert severity="error">{error}</Alert>}
      {!enabled&&!busy&&<Alert severity="info">Email delivery is disabled. Configure it on the server to enable sending.</Alert>}
      <Typography variant="body2">Accepted means the mail server accepted the message; it does not confirm inbox delivery. A sending attempt may have an unknown outcome if processing was interrupted. Check with the mail provider before resending.</Typography>
      {!attempts.length&&!busy&&<Typography>No email attempts recorded.</Typography>}
      {attempts.map(a=><Stack key={a.id} spacing={0.5} sx={{borderBottom:1,borderColor:'divider',pb:1}}>
        <Typography>{a.status} — {a.recipient||'Missing work email'}</Typography>
        <Typography variant="body2">Requested {new Date(a.requestedAt).toLocaleString()} by {a.requestedBy}</Typography>
        {a.completedAt&&<Typography variant="body2">Completed {new Date(a.completedAt).toLocaleString()}</Typography>}
        {a.failureCode&&<Alert severity="warning">{a.failureCode.replaceAll('_',' ')}</Alert>}
      </Stack>)}
    </Stack></DialogContent><DialogActions><Button disabled={busy} onClick={()=>setOpen(false)}>Close</Button>
      {canSend&&<Button disabled={busy||!enabled||slip.status!=='RELEASED'||attempts.some(a=>a.status==='SENDING')} onClick={()=>void send()}>{request.current?'Check / retry request':attempts.length?'Send again':'Send email'}</Button>}
    </DialogActions></Dialog></>
}
