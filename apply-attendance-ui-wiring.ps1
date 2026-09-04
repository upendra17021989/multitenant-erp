$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

function Replace-RequiredText {
    param(
        [Parameter(Mandatory)] [string] $Path,
        [Parameter(Mandatory)] [string] $OldText,
        [Parameter(Mandatory)] [string] $NewText
    )

    $resolvedPath = Join-Path $repoRoot $Path
    $content = [IO.File]::ReadAllText($resolvedPath)
    if (-not $content.Contains($OldText)) {
        throw "Expected source block was not found in $Path. No changes were written to that file."
    }
    $updated = $content.Replace($OldText, $NewText)
    [IO.File]::WriteAllText($resolvedPath, $updated, [Text.UTF8Encoding]::new($false))
}

$appPath = 'frontend\src\App.tsx'
Replace-RequiredText $appPath @'
import EmployeeAdmin from './EmployeeAdmin'
'@ @'
import EmployeeAdmin from './EmployeeAdmin'
import AttendanceAdmin from './AttendanceAdmin'
'@

Replace-RequiredText $appPath @'
  const [module, setModule] = useState<'organization' | 'employees'>('organization')
'@ @'
  const [module, setModule] = useState<'organization' | 'employees' | 'attendance'>('organization')
'@

Replace-RequiredText $appPath @'
          <Paper><Tabs value={module} onChange={(_event, value: 'organization' | 'employees') => setModule(value)}>
            <Tab value="organization" label="Organisation" />
            {canViewEmployees && <Tab value="employees" label="Employees" />}
          </Tabs></Paper>
          {module === 'organization' || !canViewEmployees
            ? <OrganizationAdmin session={session} membership={activeMembership} />
            : <EmployeeAdmin session={session} membership={activeMembership} />}
'@ @'
          <Paper><Tabs value={module} onChange={(_event, value: 'organization' | 'employees' | 'attendance') => setModule(value)}>
            <Tab value="organization" label="Organisation" />
            {canViewEmployees && <Tab value="employees" label="Employees" />}
            {canViewEmployees && <Tab value="attendance" label="Attendance" />}
          </Tabs></Paper>
          {(module === 'organization' || !canViewEmployees) && <OrganizationAdmin session={session} membership={activeMembership} />}
          {module === 'employees' && canViewEmployees && <EmployeeAdmin session={session} membership={activeMembership} />}
          {module === 'attendance' && canViewEmployees && <AttendanceAdmin session={session} membership={activeMembership} />}
'@

$trackerPath = 'docs\IMPLEMENTATION_TRACKER.md'
Replace-RequiredText $trackerPath @'
- [ ] Add CSV attendance import and validation
'@ @'
- [x] Add backend CSV attendance import with row-level validation and summary
- [x] Add CSV attendance upload controls to the attendance UI
'@

Replace-RequiredText $trackerPath @'
- [ ] Add attendance administration screens
'@ @'
- [x] Add attendance administration screens
'@

Replace-RequiredText $trackerPath @'
| 2026-09-04 | `mvn test` after attendance evaluation | PASS - 19 tests, 0 failures, 1 environment-dependent test skipped |
'@ @'
| 2026-09-04 | `mvn test` after attendance evaluation | PASS - 19 tests, 0 failures, 1 environment-dependent test skipped |
| 2026-09-04 | `mvn test` after CSV attendance import | PASS - 21 tests, 0 failures, 1 environment-dependent test skipped |
| 2026-09-04 | `npm.cmd run build` after attendance administration UI | PASS - TypeScript and Vite production build; bundle-size warning remains |
'@

Write-Host 'Attendance UI navigation and tracker updates applied successfully.'
