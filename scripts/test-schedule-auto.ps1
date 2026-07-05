# 自动排班功能联调脚本（Windows PowerShell）
# 用法：先启动 Nacos、Redis、backend-service、registration-service、gateway-service，再执行：
#   powershell -ExecutionPolicy Bypass -File scripts/test-schedule-auto.ps1

$GatewayUrl = if ($env:GATEWAY_URL) { $env:GATEWAY_URL } else { "http://127.0.0.1:10010" }
$RegistrationUrl = if ($env:REGISTRATION_URL) { $env:REGISTRATION_URL } else { "http://127.0.0.1:10023" }
$Username = if ($env:LOGIN_USERNAME) { $env:LOGIN_USERNAME } else { "admin_demo" }
$Password = if ($env:LOGIN_PASSWORD) { $env:LOGIN_PASSWORD } else { "password123" }
$DoctorId = if ($env:DOCTOR_ID) { [long]$env:DOCTOR_ID } else { 9201 }
$DepartmentId = if ($env:DEPARTMENT_ID) { [long]$env:DEPARTMENT_ID } else { 9101 }

function Write-Step($msg) { Write-Host "`n== $msg ==" -ForegroundColor Cyan }

function Invoke-Api {
    param(
        [string]$Method = "GET",
        [string]$Url,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )
    $params = @{
        Method      = $Method
        Uri         = $Url
        Headers     = $Headers
        ContentType = "application/json"
        ErrorAction = "Stop"
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 6)
    }
    return Invoke-RestMethod @params
}

function Get-AdminHeadersDirect {
    return @{
        "X-User-Id"    = "9501"
        "X-Username"   = "admin_demo"
        "X-User-Roles" = "ADMIN"
        "X-User-Type"  = "MANAGEMENT"
    }
}

Write-Step "1. 健康检查 registration-service"
try {
    Invoke-RestMethod -Uri "$RegistrationUrl/actuator/health" -ErrorAction Stop | Out-Null
    Write-Host "registration-service OK"
} catch {
    Write-Host "registration-service 未启动，请先启动 registration-service (10023)" -ForegroundColor Red
    exit 1
}

Write-Step "2. 管理员登录（经网关）"
$token = $null
try {
    $login = Invoke-Api -Method POST -Url "$GatewayUrl/api/auth/login" -Body @{
        username = $Username
        password = $Password
    }
    $token = $login.accessToken
    Write-Host "login ok, user=$($login.username), role=$($login.role)"
} catch {
    Write-Host "网关登录失败，后续改走 registration-service 直连 + X-User 头" -ForegroundColor Yellow
}

$authHeaders = if ($token) { @{ Authorization = "Bearer $token" } } else { Get-AdminHeadersDirect }
$base = if ($token) { $GatewayUrl } else { $RegistrationUrl }

Write-Step "3. 创建排班模板（今天星期几就填几，1=周一 ... 7=周日）"
$todayDow = [int][DayOfWeek](Get-Date).DayOfWeek
if ($todayDow -eq 0) { $todayDow = 7 }  # PowerShell Sunday=0，模板用 ISO 7
$templateBody = @{
    doctorId     = $DoctorId
    departmentId = $DepartmentId
    dayOfWeek    = $todayDow
    timeSlot     = "MORNING"
    sourceCount  = 15
    feeAmount    = 30.00
    sourceType   = "NORMAL"
}
try {
    $template = Invoke-Api -Method POST -Url "$base/api/schedule-templates" -Headers $authHeaders -Body $templateBody
    Write-Host "template created id=$($template.data.id), dayOfWeek=$todayDow"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 500 -or $_.ErrorDetails.Message -match "已有排班模板|duplicate|unique") {
        Write-Host "模板可能已存在，继续后续步骤" -ForegroundColor Yellow
    } elseif ($_.ErrorDetails.Message -match "doctor_schedule_template") {
        Write-Host "请先执行 SQL: infra/postgres/init/004-schedule-template.sql" -ForegroundColor Red
        exit 1
    } else {
        throw
    }
}

Write-Step "4. 一键生成未来 7 天排班"
$generate = Invoke-Api -Method POST -Url "$base/api/schedules/generate" -Headers $authHeaders -Body @{ days = 7 }
$data = $generate.data
Write-Host "created=$($data.createdCount), skipped=$($data.skippedCount), range=$($data.startDate)~$($data.endDate)"
if ($data.createdScheduleIds.Count -gt 0) {
    Write-Host "created ids: $($data.createdScheduleIds -join ', ')"
}

Write-Step "5. 管理端查看全部排班"
$all = Invoke-Api -Url "$base/api/schedules?pageNo=1&pageSize=5&doctorId=$DoctorId" -Headers $authHeaders
Write-Host "total=$($all.data.total)"
$all.data.records | ForEach-Object {
    Write-Host "  id=$($_.id) date=$($_.scheduleDate) slot=$($_.timeSlot) avail=$($_.availableCount) status=$($_.status)"
}

Write-Step "6. 患者视角：bookableOnly=true（7天窗口）"
$bookable = Invoke-Api -Url "$base/api/schedules?bookableOnly=true&doctorId=$DoctorId&pageNo=1&pageSize=10" -Headers $authHeaders
Write-Host "bookable total=$($bookable.data.total)"

Write-Step "7. 手动改号源（验证自动+手动）"
if ($data.createdScheduleIds.Count -gt 0) {
    $sid = $data.createdScheduleIds[0]
    $updated = Invoke-Api -Method PUT -Url "$base/api/schedules/$sid" -Headers $authHeaders -Body @{ sourceCount = 18 }
    Write-Host "updated schedule $sid -> sourceCount=$($updated.data.sourceCount), available=$($updated.data.availableCount)"
} else {
    Write-Host "本次未新建排班（可能已存在），跳过改号源" -ForegroundColor Yellow
}

Write-Step "8. 再次 generate（应 skipped>0）"
$generate2 = Invoke-Api -Method POST -Url "$base/api/schedules/generate" -Headers $authHeaders -Body @{ days = 7 }
Write-Host "created=$($generate2.data.createdCount), skipped=$($generate2.data.skippedCount)"

Write-Host "`nDone. 自动排班联调完成。" -ForegroundColor Green
