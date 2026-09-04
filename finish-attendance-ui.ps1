$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

$appPath = Join-Path $repoRoot 'frontend\src\App.tsx'
$app = [IO.File]::ReadAllText($appPath)
$renderPattern = [regex]::Escape("          {module === 'organization' || !canViewEmployees") + '\r?\n' +
    [regex]::Escape('            ? <OrganizationAdmin session={session} membership={activeMembership} />') + '\r?\n' +
    [regex]::Escape('            : <EmployeeAdmin session={session} membership={activeMembership} />}')
$renderReplacement = "          {(module === 'organization' || !canViewEmployees) && <OrganizationAdmin session={session} membership={activeMembership} />}`r`n" +
    "          {module === 'employees' && canViewEmployees && <EmployeeAdmin session={session} membership={activeMembership} />}`r`n" +
    "          {module === 'attendance' && canViewEmployees && <AttendanceAdmin session={session} membership={activeMembership} />}"
$updatedApp = [regex]::Replace($app, $renderPattern, $renderReplacement, 1)
if ($updatedApp -eq $app) { throw 'The unfinished App.tsx render block was not found.' }
[IO.File]::WriteAllText($appPath, $updatedApp, [Text.UTF8Encoding]::new($false))

$trackerPath = Join-Path $repoRoot 'docs\IMPLEMENTATION_TRACKER.md'
$tracker = [IO.File]::ReadAllText($trackerPath)
$oldCsv = '- [ ] Add CSV attendance import and validation'
$newCsv = "- [x] Add backend CSV attendance import with row-level validation and summary`r`n- [x] Add CSV attendance upload controls to the attendance UI"
$oldUi = '- [ ] Add attendance administration screens'
$newUi = '- [x] Add attendance administration screens'
$verification = '| 2026-09-04 | `mvn test` after attendance evaluation | PASS - 19 tests, 0 failures, 1 environment-dependent test skipped |'
$newVerification = $verification + "`r`n| 2026-09-04 | ``mvn test`` after CSV attendance import | PASS - 21 tests, 0 failures, 1 environment-dependent test skipped |`r`n| 2026-09-04 | ``npm.cmd run build`` after attendance administration UI | PASS - TypeScript and Vite production build; bundle-size warning remains |"
foreach ($required in @($oldCsv, $oldUi, $verification)) {
    if (-not $tracker.Contains($required)) { throw "Tracker text not found: $required" }
}
$tracker = $tracker.Replace($oldCsv, $newCsv).Replace($oldUi, $newUi).Replace($verification, $newVerification)
[IO.File]::WriteAllText($trackerPath, $tracker, [Text.UTF8Encoding]::new($false))

Write-Host 'Remaining Attendance UI changes applied.'
