import {useEffect,useState,type FormEvent} from 'react'
import type {Session} from '@supabase/supabase-js'
import {Alert,Button,Checkbox,FormControlLabel,MenuItem,Paper,Stack,TextField,Typography} from '@mui/material'
import {apiJson,type TenantMembership} from './api'
type Employee={id:string;employeeNumber:string;firstName:string;lastName:string}
type Master={id:string;name?:string;title?:string;code:string}
type Workflow={id:string;employmentId?:string;workflowType:string;title:string;description:string;effectiveDate:string;status:string;createdBy:string;configuration:{rating?:number;annualCtc?:number;settlementEarnings?:number;settlementDeductions?:number};tasks:{title:string;completed:boolean;comment:string;completedBy?:string}[]}
type Event={action:string;actor:string;reason:string;actedAt:string}
const caption=(value:string)=>value.replaceAll('_',' ')
export default function HrWorkflows({session,membership}:{session:Session;membership:TenantMembership}){
 const admin=membership.roles.some(r=>['SYSTEM_ADMIN','GROUP_ADMIN','COMPANY_ADMIN'].includes(r)),hr=admin||membership.roles.includes('HR_MANAGER'),payroll=admin||membership.roles.includes('PAYROLL_MANAGER')
 const types=[...(hr?['RECRUITMENT','ONBOARDING','TRAINING','APPRAISAL','PROMOTION']:[]),...(payroll?['SALARY_REVISION','EXIT']:[])]
 const [rows,setRows]=useState<Workflow[]>([]),[employees,setEmployees]=useState<Employee[]>([]),[designations,setDesignations]=useState<Master[]>([]),[structures,setStructures]=useState<Master[]>([])
 const [error,setError]=useState(''),[busy,setBusy]=useState(false),[revision,setRevision]=useState(0),[comment,setComment]=useState(''),[history,setHistory]=useState<Event[]>([]),[historyId,setHistoryId]=useState('')
 const [form,setForm]=useState({workflowType:types[0]??'ONBOARDING',employmentId:'',title:'',description:'',effectiveDate:new Date().toISOString().slice(0,10),rating:3,designationId:'',salaryStructureId:'',annualCtc:0,settlementEarnings:0,settlementDeductions:0})
 useEffect(()=>{let active=true;setBusy(true);setError('');Promise.all([apiJson<Workflow[]>('/hr-workflows',session,membership.tenantId),apiJson<Employee[]>('/employees',session,membership.tenantId),apiJson<Master[]>('/organization/designations',session,membership.tenantId),payroll?apiJson<Master[]>('/payroll/structures',session,membership.tenantId):Promise.resolve([])]).then(([workflows,people,roles,salary])=>{if(active){setRows(workflows);setEmployees(people);setDesignations(roles);setStructures(salary)}}).catch((e:unknown)=>{if(active)setError(message(e))}).finally(()=>{if(active)setBusy(false)});return()=>{active=false}},[session,membership.tenantId,payroll,revision])
 async function action(path:string,body:unknown,method='POST'){setBusy(true);setError('');try{await apiJson(path,session,membership.tenantId,{method,body:JSON.stringify(body)});setRevision(v=>v+1)}catch(e){setError(message(e));setBusy(false)}}
 function submit(e:FormEvent){e.preventDefault();void action('/hr-workflows',{workflowType:form.workflowType,employmentId:form.employmentId||null,title:form.title,description:form.description,effectiveDate:form.effectiveDate,configuration:{rating:form.rating,designationId:form.designationId||null,salaryStructureId:form.salaryStructureId||null,annualCtc:form.annualCtc,settlementEarnings:form.settlementEarnings,settlementDeductions:form.settlementDeductions}})}
 async function showHistory(id:string){setBusy(true);try{setHistory(await apiJson<Event[]>(`/hr-workflows/${id}/history`,session,membership.tenantId));setHistoryId(id)}catch(e){setError(message(e))}finally{setBusy(false)}}
 return <Stack spacing={2}><Typography variant="h5">HR workflows — {membership.legalName}</Typography>
  <Typography>Complete the checklist, submit for independent review, then complete the approved workflow. Promotion and exit completion update employment records. Salary revisions create an effective-dated salary assignment. Exit settlement amounts must be calculated and verified before submission.</Typography>
  {error&&<Alert severity="error">{error}</Alert>}
  <Paper component="form" onSubmit={submit} sx={{p:3}}><Stack spacing={2}>
   <TextField select label="Workflow" value={form.workflowType} onChange={e=>setForm({...form,workflowType:e.target.value})}>{types.map(value=><MenuItem key={value} value={value}>{caption(value)}</MenuItem>)}</TextField>
   <TextField select required={form.workflowType!=='RECRUITMENT'} label="Employee" value={form.employmentId} onChange={e=>setForm({...form,employmentId:e.target.value})}><MenuItem value="">Not yet employed</MenuItem>{employees.map(e=><MenuItem key={e.id} value={e.id}>{e.employeeNumber} — {e.firstName} {e.lastName}</MenuItem>)}</TextField>
   <TextField required label={form.workflowType==='RECRUITMENT'?'Candidate and position':'Title'} value={form.title} inputProps={{maxLength:200}} onChange={e=>setForm({...form,title:e.target.value})}/>
   <TextField required multiline minRows={3} label="Details, objectives and evidence" value={form.description} inputProps={{maxLength:4000}} onChange={e=>setForm({...form,description:e.target.value})}/>
   <TextField required type="date" label="Effective / target date" InputLabelProps={{shrink:true}} value={form.effectiveDate} onChange={e=>setForm({...form,effectiveDate:e.target.value})}/>
   {form.workflowType==='APPRAISAL'&&<TextField required select label="Overall rating" value={form.rating} onChange={e=>setForm({...form,rating:Number(e.target.value)})}>{[1,2,3,4,5].map(r=><MenuItem key={r} value={r}>{r}</MenuItem>)}</TextField>}
   {form.workflowType==='PROMOTION'&&<TextField required select label="New designation" value={form.designationId} onChange={e=>setForm({...form,designationId:e.target.value})}>{designations.map(d=><MenuItem key={d.id} value={d.id}>{d.title??d.name??d.code}</MenuItem>)}</TextField>}
   {form.workflowType==='SALARY_REVISION'&&<><TextField required select label="Salary structure" value={form.salaryStructureId} onChange={e=>setForm({...form,salaryStructureId:e.target.value})}>{structures.map(s=><MenuItem key={s.id} value={s.id}>{s.name}</MenuItem>)}</TextField><TextField required type="number" label="Annual CTC" value={form.annualCtc} inputProps={{min:0,step:0.01}} onChange={e=>setForm({...form,annualCtc:Number(e.target.value)})}/></>}
   {form.workflowType==='EXIT'&&<><TextField required type="number" label="Verified settlement earnings" inputProps={{min:0,step:0.01}} value={form.settlementEarnings} onChange={e=>setForm({...form,settlementEarnings:Number(e.target.value)})}/><TextField required type="number" label="Verified settlement deductions" inputProps={{min:0,step:0.01}} value={form.settlementDeductions} onChange={e=>setForm({...form,settlementDeductions:Number(e.target.value)})}/><Typography>Net settlement: INR {form.settlementEarnings-form.settlementDeductions}</Typography></>}
   <Button type="submit" disabled={busy}>Create workflow</Button>
  </Stack></Paper>
  <TextField label="Task evidence / transition reason (required)" value={comment} inputProps={{maxLength:1000}} onChange={e=>setComment(e.target.value)}/>
  {rows.map(row=><Paper key={row.id} sx={{p:3}}><Stack spacing={1}>
   <Typography variant="h6">{row.title} · {caption(row.workflowType)} · {caption(row.status)}</Typography><Typography>{row.description}</Typography><Typography>Effective / target date: {row.effectiveDate}</Typography>
   {row.configuration.rating!=null&&<Typography>Overall rating: {row.configuration.rating}/5</Typography>}
   {row.configuration.annualCtc!=null&&<Typography>Annual CTC: INR {row.configuration.annualCtc}</Typography>}
   {row.configuration.settlementEarnings!=null&&<Typography>Net settlement: INR {row.configuration.settlementEarnings-(row.configuration.settlementDeductions??0)}</Typography>}
   {row.tasks.map((task,index)=><Stack key={index}><FormControlLabel label={task.title} control={<Checkbox checked={task.completed} disabled={busy||!comment.trim()||row.status!=='IN_PROGRESS'} onChange={e=>void action(`/hr-workflows/${row.id}/tasks/${index}`,{completed:e.target.checked,comment},'PUT')}/>}/><Typography variant="caption">{task.comment}</Typography></Stack>)}
   <Stack direction="row">{(row.status==='IN_PROGRESS'?['SUBMIT']:row.status==='UNDER_REVIEW'?['APPROVE','REJECT']:row.status==='APPROVED'?['COMPLETE']:[]).map(act=><Button key={act} disabled={busy||!comment.trim()} onClick={()=>void action(`/hr-workflows/${row.id}/transition`,{action:act,reason:comment})}>{caption(act)}</Button>)}<Button onClick={()=>void showHistory(row.id)} disabled={busy}>View history</Button></Stack>
   {historyId===row.id&&history.map((event,index)=><Typography variant="body2" key={index}>{event.actedAt} · {event.action} · {event.actor} · {event.reason}</Typography>)}
  </Stack></Paper>)}
 </Stack>
}
function message(e:unknown){return e instanceof Error?e.message:'Operation failed.'}
