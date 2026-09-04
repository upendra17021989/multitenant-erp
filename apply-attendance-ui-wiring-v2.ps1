$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

function Replace-Line {
    param([string] $RelativePath, [string] $OldLine, [string] $NewLine)
    $target = Join-Path $repoRoot $RelativePath
    $content = [IO.File]::ReadAllText($target)
    if (-not $content.Contains($OldLine)) {
        throw "Expected text was not found in $RelativePath`: $OldLine"
    }
    [IO.File]::WriteAllText($target, $content.Replace($OldLine, $NewLine), [Text.UTF8Encoding]::new($false))
}

$app = 'frontend\src\App.tsx'
Replace-Line $app "import EmployeeAdmin from './EmployeeAdmin'" "import EmployeeAdmin from './EmployeeAdmin'`r`nimport AttendanceAdmin from './AttendanceAdmin'"
Replace-Line $app "  const [module, setModule] = useState<'organization' | 'employees'>('organization')" "  const [module, setModule] = useState<'organization' | 'employees' | 'attendance'>('organization')"
Replace-Line $app "          <Paper><Tabs value={module} onChange={(_event, value: 'organization' | 'employees') => setModule(value)}>" "          <Paper><Tabs value={module} onChange={(_event, value: 'organization' | 'employees' | 'attendance') => setModule(value)}>"
Replace-Line $app '            {canViewEmployees && <Tab value="employees" label="Employees" />}' "            {canViewEmployees && <Tab value=`"employees`" label=`"Employees`" />}`r`n            {canViewEmployees && <Tab value=`"attendance`" label=`"Attendance`" />}"
Replace-Line $app "          {module === 'organization' || !canViewEmployees`r`n            ? <OrganizationAdmin session={session} membership={activeMembership} />`r`n            : <EmployeeAdmin session={session} membership={activeMembership} />}" "          {(module === 'organization' || !canViewEmployees) && <OrganizationAdmin session={session} membership={activeMembership} />}`r`n          {module === 'employees' && canViewEmployees && <EmployeeAdmin session={session} membership={activeMembership} />}`r`n          {module === 'attendance' && canViewEmployees && <AttendanceAdmin session={session} membership={activeMembership} />}"

$tracker = 'docs\IMPLEMENTATION_TRACKER.md'
Replace-Line $tracker '- [ ] Add CSV attendance import and validation' "- [x] Add backend CSV attendance import with row-level validation and summary`r`n- [x] Add CSV attendance upload controls to the attendance UI"
Replace-Line $tracker '- [ ] Add attendance administration screens' '- [x] Add attendance administration screens'
Replace-Line $tracker '| 2026-09-04 | `mvn test` after attendance evaluation | PASS - 19 tests, 0 failures, 1 environment-dependent test skipped |' "| 2026-09-04 | ``mvn test`` after attendance evaluation | PASS - 19 tests, 0 failures, 1 environment-dependent test skipped |`r`n| 2026-09-04 | ``mvn test`` after CSV attendance import | PASS - 21 tests, 0 failures, 1 environment-dependent test skipped |`r`n| 2026-09-04 | ``npm.cmd run build`` after attendance administration UI | PASS - TypeScript and Vite production build; bundle-size warning remains |"

Write-Host 'Attendance UI navigation and tracker updates applied successfully.'
