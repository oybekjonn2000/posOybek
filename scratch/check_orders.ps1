$body = @{ username = 'admin'; password = 'admin123' } | ConvertTo-Json
$token = (Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body $body).data.accessToken
$headers = @{ Authorization = "Bearer $token" }

Write-Host "=========================================="
Write-Host " ACTIVE ORDERS SUMMARY"
Write-Host "=========================================="
$activeRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/orders' -Headers $headers
$activeOrders = $activeRes.data
Write-Host "Active orders count: $($activeOrders.Count)"
$activeOrders | Select-Object orderNumber, status, total | Format-Table -AutoSize
$activeTotal = 0
foreach ($ao in $activeOrders) {
    $activeTotal += [double]$ao.total
}
Write-Host "TOTAL ACTIVE ORDERS SUM: $activeTotal so'm"

Write-Host "`n=========================================="
Write-Host " PAID / HISTORY ORDERS SUMMARY"
Write-Host "=========================================="
$res = Invoke-RestMethod -Uri 'http://localhost:8080/api/orders/history' -Headers $headers
$orders = $res.data
Write-Host "History orders count: $($orders.Count)"

$todayTotal = 0
$todayPaid = 0
$allTotal = 0
$allPaid = 0
$now = Get-Date

foreach ($o in $orders) {
    $paid = $o.total
    if ($o.paidAmount) { $paid = $o.paidAmount }
    
    $allTotal += [double]$o.total
    $allPaid += [double]$paid
    
    $dStr = $o.closedAt
    if (-not $dStr) { $dStr = $o.createdAt }
    $d = [DateTime]::Parse($dStr)
    
    if ($d.Date -eq $now.Date) {
        $todayTotal += [double]$o.total
        $todayPaid += [double]$paid
    }
}

Write-Host "ALL History Orders Sum (total field): $allTotal so'm"
Write-Host "ALL History Orders Paid (paidAmount field): $allPaid so'm"
Write-Host "TODAY History Orders Sum (total field): $todayTotal so'm"
Write-Host "TODAY History Orders Paid (paidAmount field): $todayPaid so'm"
