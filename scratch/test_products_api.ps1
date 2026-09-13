$loginBody = @{
    username = 'admin'
    password = 'admin123'
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $loginBody -ContentType 'application/json'
$token = $loginRes.data.accessToken
$headers = @{
    Authorization = "Bearer $token"
}

$activeRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/products?activeOnly=true' -Method Get -Headers $headers
$allRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/products?activeOnly=false' -Method Get -Headers $headers

Write-Host "=========================================="
Write-Host "ACTIVE ONLY (true) Count: $($activeRes.data.Count)"
Write-Host "ALL PRODUCTS (false) Count: $($allRes.data.Count)"
Write-Host "=========================================="

Write-Host "--- ACTIVE ONLY (true) Products ---"
foreach ($p in $activeRes.data) {
    Write-Host " - [$($p.id)] $($p.name) (category: $($p.categoryName), active: $($p.active), available: $($p.available))"
}

Write-Host "`n--- ALL PRODUCTS (false) ---"
foreach ($p in $allRes.data) {
    Write-Host " - [$($p.id)] $($p.name) (category: $($p.categoryName), active: $($p.active), available: $($p.available))"
}
