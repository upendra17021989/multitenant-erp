import {useCallback,useEffect,useState,type FormEvent} from 'react'
import type {Session} from '@supabase/supabase-js'
import {Alert,Button,Checkbox,FormControlLabel,MenuItem,Paper,Stack,Table,TableBody,TableCell,TableHead,TableRow,TextField,Typography} from '@mui/material'
import {apiJson,type TenantMembership} from './api'

type Account={id:string;email:string;displayName:string;status:string;roles:string[];employmentId?:string;employeeNumber?:string;employeeName?:string}
type Employee={id:string;employeeNumber:string;firstName:string;lastName:string;workEmail?:string}
const roles=['EMPLOYEE','HR_EXECUTIVE','HR_MANAGER','PAYROLL_EXECUTIVE','PAYROLL_MANAGER','COMPANY_ADMIN'] as const
const label=(value:string)=>value.replaceAll('_',' ').toLowerCase().replace(/\b\w/g,c=>c.toUpperCase())

export default function AccountAdmin({session,membership}:{session:Session;membership:TenantMembership}){
 const [accounts,setAccounts]=useState<Account[]>([]),[employees,setEmployees]=useState<Employee[]>([])
 const [email,setEmail]=useState(''),[displayName,setDisplayName]=useState(''),[employmentId,setEmploymentId]=useState('')
 const [selectedRoles,setSelectedRoles]=useState<string[]>(['EMPLOYEE']),[busy,setBusy]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState('')
 const load=useCallback(async()=>{setBusy(true);setError('');try{const [users,staff]=await Promise.all([apiJson<Account[]>('/accounts',session,membership.tenantId),apiJson<Employee[]>('/employees',session,membership.tenantId)]);setAccounts(users);setEmployees(staff)}catch(reason){setError(reason instanceof Error?reason.message:'Unable to load accounts.')}finally{setBusy(false)}},[session,membership.tenantId])
 useEffect(()=>{void load()},[load])
 function chooseEmployee(id:string){setEmploymentId(id);const employee=employees.find(item=>item.id===id);if(employee){setDisplayName(`${employee.firstName} ${employee.lastName}`.trim());if(employee.workEmail)setEmail(employee.workEmail)}}
 function toggleRole(role:string){setSelectedRoles(current=>current.includes(role)?current.filter(item=>item!==role):[...current,role])}
 async function invite(event:FormEvent){event.preventDefault();setBusy(true);setError('');setNotice('');try{await apiJson('/accounts',session,membership.tenantId,{method:'POST',body:JSON.stringify({email,displayName,roles:selectedRoles,employmentId:employmentId||null})});setEmail('');setDisplayName('');setEmploymentId('');setSelectedRoles(['EMPLOYEE']);setNotice('Invitation sent and company access created.');await load()}catch(reason){setError(reason instanceof Error?reason.message:'Unable to create account.')}finally{setBusy(false)}}
 async function changeStatus(account:Account){setBusy(true);setError('');setNotice('');const status=account.status==='ACTIVE'?'DISABLED':'ACTIVE';try{await apiJson(`/accounts/${account.id}/status`,session,membership.tenantId,{method:'PUT',body:JSON.stringify({status})});setNotice(`Account ${status.toLowerCase()}.`);await load()}catch(reason){setError(reason instanceof Error?reason.message:'Unable to change account status.')}finally{setBusy(false)}}
 return <Stack spacing={3}>
  {error&&<Alert severity="error">{error}</Alert>}{notice&&<Alert severity="success">{notice}</Alert>}
  <Paper sx={{p:3}}><Typography variant="h6" mb={1}>Invite employee account</Typography><Typography color="text.secondary" mb={2}>Sends a Supabase invitation and creates company access. Selecting an employee also links self-service access.</Typography>
   <Stack component="form" spacing={2} onSubmit={invite}>
    <TextField select label="Employee (optional)" value={employmentId} onChange={event=>chooseEmployee(event.target.value)}><MenuItem value="">No employee link</MenuItem>{employees.map(employee=><MenuItem key={employee.id} value={employee.id}>{employee.employeeNumber} — {employee.firstName} {employee.lastName}</MenuItem>)}</TextField>
    <Stack direction={{xs:'column',md:'row'}} spacing={2}><TextField required fullWidth label="Display name" value={displayName} onChange={event=>setDisplayName(event.target.value)}/><TextField required fullWidth type="email" label="Email" value={email} onChange={event=>setEmail(event.target.value)}/></Stack>
    <div><Typography variant="subtitle2">Company roles</Typography>{roles.map(role=><FormControlLabel key={role} control={<Checkbox checked={selectedRoles.includes(role)} onChange={()=>toggleRole(role)}/>} label={label(role)}/>)}</div>
    <Button type="submit" variant="contained" disabled={busy||selectedRoles.length===0}>Send invitation</Button>
   </Stack>
  </Paper>
  <Paper sx={{p:3,overflow:'auto'}}><Stack direction="row" alignItems="center" mb={2}><Typography variant="h6" flexGrow={1}>Company accounts</Typography><Button onClick={()=>void load()} disabled={busy}>Refresh</Button></Stack>
   <Table size="small"><TableHead><TableRow>{['Name','Email','Roles','Employee','Status',''].map(value=><TableCell key={value}>{value}</TableCell>)}</TableRow></TableHead><TableBody>{accounts.map(account=><TableRow key={account.id}><TableCell>{account.displayName}</TableCell><TableCell>{account.email}</TableCell><TableCell>{account.roles.map(label).join(', ')}</TableCell><TableCell>{account.employeeNumber?`${account.employeeNumber} — ${account.employeeName}`:'Not linked'}</TableCell><TableCell>{label(account.status)}</TableCell><TableCell><Button color={account.status==='ACTIVE'?'error':'success'} onClick={()=>void changeStatus(account)} disabled={busy}>{account.status==='ACTIVE'?'Disable':'Enable'}</Button></TableCell></TableRow>)}</TableBody></Table>
   {!accounts.length&&!busy&&<Typography color="text.secondary" textAlign="center" py={3}>No accounts found.</Typography>}
  </Paper>
 </Stack>
}
