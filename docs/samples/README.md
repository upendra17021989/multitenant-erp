# HR workflow sample data

[`hr-workflows.json`](hr-workflows.json) contains seven fictional create-request examples, one for every supported workflow type. You can copy their values into **HR workflows** or send each object separately to `POST /api/hr-workflows`. The endpoint does not accept the entire array as one request.

## Prepare the examples

Use employees in your demo/test company. Replace these placeholder UUIDs throughout the JSON before submitting:

| Placeholder ending | Replace with | Lookup endpoint |
|---|---|---|
| `000000000001` | Employment ID for the onboarding, training, appraisal, promotion and salary examples | `GET /api/employees` |
| `000000000002` | New designation ID in the same company | `GET /api/organization/designations` |
| `000000000003` | Active salary structure ID in the same company | `GET /api/payroll/structure/templates` |
| `000000000004` | Employment ID for a separate exit example employee | `GET /api/employees` |

Use each lookup's `id` field, not an employee number or person ID. You may use different employees for each example. Recruitment requires no existing employee. Adjust the dates to suit your demo; the exit date must not precede the employee's joining date.

Every request needs `Authorization: Bearer <access-token>` and `X-Tenant-Id: <company-UUID>`. JSON writes also need `Content-Type: application/json`. Company, group or system administrators can create all seven examples. HR managers can create the five nonfinancial types; payroll managers can create salary revision and exit workflows.

## Create through PowerShell

After replacing the IDs, run this from the repository root with `$accessToken` and `$tenantId` set for your demo company. Use your backend URL for `$apiBase`.

```powershell
$apiBase = 'http://localhost:8080/api'
$headers = @{
    Authorization = "Bearer $accessToken"
    'X-Tenant-Id' = $tenantId
}
$samples = Get-Content -Raw docs/samples/hr-workflows.json | ConvertFrom-Json
foreach ($sample in $samples) {
    Invoke-RestMethod -Method Post -Uri "$apiBase/hr-workflows" `
        -Headers $headers -ContentType 'application/json' `
        -Body ($sample | ConvertTo-Json -Depth 5) -ErrorAction Stop
}
```

Each successful request creates a workflow in `IN_PROGRESS`, generates its standard unchecked checklist and records a `CREATE` history event. Re-running creates duplicates; if a request fails, inspect existing workflows before retrying just the missing examples. The loop is not a batch transaction.

## Try the review flow

1. Add a task evidence comment and complete every checklist item. Through the API, use `PUT /api/hr-workflows/{id}/tasks/{index}` with `{"completed":true,"comment":"SAMPLE: reviewed fictional supporting evidence."}`. Indexes start at zero; use the tasks returned by creation.
2. Submit with `POST /api/hr-workflows/{id}/transition` and `{"action":"SUBMIT","reason":"SAMPLE: all checklist evidence recorded."}`. Status becomes `UNDER_REVIEW`.
3. Sign in as a second authorized user who neither created the case nor updated any task. Use the same transition endpoint with `{"action":"APPROVE","reason":"SAMPLE: independent review completed."}` or `{"action":"REJECT","reason":"SAMPLE: supporting evidence is insufficient."}`.
4. An approved workflow can transition using `{"action":"COMPLETE","reason":"SAMPLE: approved workflow finalized."}`. Promotion and exit cannot complete before their effective date.

Completion of a promotion changes the employee's designation; a salary revision creates a salary assignment; an exit marks employment as exited. Use demo employees for these examples. Exit settlement figures are recorded values and do not themselves issue a payment. Rejected and completed workflows have no further transitions in the current implementation.

No records are inserted automatically by these files.
