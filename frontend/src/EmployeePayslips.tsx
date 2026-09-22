import {useEffect, useState} from 'react'
import type {Session} from '@supabase/supabase-js'
import {Alert, Button, Paper, Stack, Typography} from '@mui/material'
import {apiFetch, apiJson, type TenantMembership} from './api'

type Payslip = {id:string; year:number; month:number; fileName:string; acknowledgedAt:string|null}

export default function EmployeePayslips({session, membership}:{session:Session; membership:TenantMembership}) {
  const [payslips, setPayslips] = useState<Payslip[]>([])
  const [loading, setLoading] = useState(true)
  const [downloading, setDownloading] = useState(false)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    let active = true
    setLoading(true); setPayslips([]); setError('')
    void apiJson<Payslip[]>('/self/payslips', session, membership.tenantId)
      .then(rows => {if (active) setPayslips(rows)})
      .catch((reason:unknown) => {if (active) setError(message(reason))})
      .finally(() => {if (active) setLoading(false)})
    return () => {active = false}
  }, [session, membership.tenantId, revision])

  async function acknowledge(slip:Payslip) {
    setDownloading(true); setError('')
    try {
      const updated = await apiJson<Payslip>(`/self/payslips/${slip.id}/acknowledgement`, session, membership.tenantId, {method:'POST'})
      setPayslips(rows => rows.map(row => row.id === updated.id ? updated : row))
    } catch (reason) {setError(message(reason))} finally {setDownloading(false)}
  }
  async function download(slip:Payslip) {
    setDownloading(true); setError('')
    try {
      const response = await apiFetch(`/self/payslips/${slip.id}/content`, session, membership.tenantId)
      if (!response.ok) throw new Error(`Unable to download payslip (${response.status}).`)
      const url = URL.createObjectURL(await response.blob())
      const anchor = document.createElement('a')
      anchor.href = url; anchor.download = slip.fileName
      document.body.appendChild(anchor); anchor.click(); anchor.remove()
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (reason) {setError(message(reason))} finally {setDownloading(false)}
  }

  return <Paper sx={{p:3}}><Stack spacing={2}>
    <Typography variant="h6">Released payslips</Typography><Typography color="text.secondary">Acknowledgement confirms receipt of the payslip, not agreement with its calculation.</Typography>
    {error && <Alert severity="error">{error} If your employee profile is not linked, contact HR.</Alert>}
    {loading && <Typography role="status">Loading payslips...</Typography>}
    {!loading && !error && !payslips.length && <Typography>No released payslips available.</Typography>}
    {payslips.map(slip => <Stack key={slip.id} direction={{xs:'column',sm:'row'}} spacing={2} alignItems={{sm:'center'}}><Button disabled={loading || downloading} onClick={() => void download(slip)}>
      Download {slip.year}-{String(slip.month).padStart(2,'0')} PDF
    </Button>
      {slip.acknowledgedAt ? <Typography role="status">Receipt acknowledged on {new Date(slip.acknowledgedAt).toLocaleString()}</Typography>
        : <Button variant="outlined" disabled={loading || downloading} onClick={() => void acknowledge(slip)}>Acknowledge receipt</Button>}
    </Stack>)}
    <Button disabled={loading || downloading} onClick={() => setRevision(value => value + 1)}>Refresh payslips</Button>
  </Stack></Paper>
}

function message(reason:unknown) {return reason instanceof Error ? reason.message : 'Unable to load payslips.'}
