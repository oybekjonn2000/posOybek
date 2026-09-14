$loginBody = @{ username = 'waiter1'; password = 'waiter123' } | ConvertTo-Json
$res = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $loginBody -ContentType 'application/json'
Write-Host "Waiter Login Success: $($res.success)"
$token = $res.data.accessToken
Write-Host "User role: $($res.data.user.role)"
Write-Host "Permissions: $($res.data.user.permissions -join ', ')"

$headers = @{ Authorization = "Bearer $token" }
$ordersRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/orders/active' -Method Get -Headers $headers
Write-Host "Active Orders call Success: $($ordersRes.success)"

$tablesRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/tables' -Method Get -Headers $headers
Write-Host "Tables call Success: $($tablesRes.success)"
Write-Host "Total Tables: $($tablesRes.data.Count)"
