$ErrorActionPreference = "Continue"

Write-Host "=================================================="
Write-Host "  RESTAURANT POS - PAGINATION VERIFICATION SUITE"
Write-Host "=================================================="

$baseUrl = "http://localhost:8080/api"

# Helper for login
function Get-AuthToken($username, $password) {
    $body = @{ username = $username; password = $password } | ConvertTo-Json
    try {
        $res = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $body
        return $res.data.accessToken
    } catch {
        Write-Host "Failed to login as $username : $($_.Exception.Message)"
        return $null
    }
}

# 1. Login as admin
Write-Host "`n[1] Authenticating as admin..."
$adminToken = Get-AuthToken "admin" "admin123"
if (-not $adminToken) {
    Write-Host "Trying password: admin"
    $adminToken = Get-AuthToken "admin" "admin"
}
if (-not $adminToken) {
    Write-Host "[FAIL] Admin authentication failed!" -ForegroundColor Red
    exit 1
}
Write-Host "[PASS] Admin logged in successfully." -ForegroundColor Green

$headers = @{ "Authorization" = "Bearer $adminToken" }

# 2. Test Products Pagination
Write-Host "`n[2] Testing Products Server-Side Pagination..."
$res1 = Invoke-RestMethod -Uri "$baseUrl/products?page=0&size=10" -Method Get -Headers $headers
if ($res1.page -and $res1.page.size -eq 10) {
    Write-Host "[PASS] GET /api/products?page=0&size=10: size=$($res1.page.size), total=$($res1.page.totalElements), totalPages=$($res1.page.totalPages)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Products pagination metadata missing or incorrect: $($res1 | ConvertTo-Json -Depth 2)" -ForegroundColor Red
}

# 3. Test Max Page Size Clamping (Security & DoS Protection)
Write-Host "`n[3] Testing Max Page Size Clamping (size=100000 -> max 100)..."
$resClamp = Invoke-RestMethod -Uri "$baseUrl/products?page=0&size=100000" -Method Get -Headers $headers
if ($resClamp.page -and $resClamp.page.size -eq 100) {
    Write-Host "[PASS] Max page size securely clamped to 100!" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Clamping failed: size=$($resClamp.page.size)" -ForegroundColor Red
}

# 4. Test Unpaged Callers Backward Compatibility (POS Ordering Screen)
Write-Host "`n[4] Testing Backward Compatibility for POS Screen (Unpaged GET /api/products)..."
$resUnpaged = Invoke-RestMethod -Uri "$baseUrl/products" -Method Get -Headers $headers
if ($resUnpaged.data -and ($resUnpaged.data.Count -gt 0)) {
    Write-Host "[PASS] Unpaged /api/products returns all active products for POS table menu ($($resUnpaged.data.Count) items)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Unpaged call failed: $($resUnpaged | ConvertTo-Json -Depth 2)" -ForegroundColor Red
}

# 5. Test Active Orders Pagination
Write-Host "`n[5] Testing Orders Server-Side Pagination..."
$resOrders = Invoke-RestMethod -Uri "$baseUrl/orders?page=0&size=10" -Method Get -Headers $headers
if ($resOrders.page) {
    Write-Host "[PASS] GET /api/orders?page=0&size=10: size=$($resOrders.page.size), total=$($resOrders.page.totalElements)" -ForegroundColor Green
} else {
    Write-Host "[WARN] Orders returned without page: $($resOrders | ConvertTo-Json -Depth 2)"
}

# 6. Test Order History Pagination
Write-Host "`n[6] Testing Order History Server-Side Pagination..."
$resHistory = Invoke-RestMethod -Uri "$baseUrl/orders/history?page=0&size=10" -Method Get -Headers $headers
if ($resHistory.page) {
    Write-Host "[PASS] GET /api/orders/history?page=0&size=10: size=$($resHistory.page.size), total=$($resHistory.page.totalElements)" -ForegroundColor Green
} else {
    Write-Host "[WARN] Order history returned without page: $($resHistory | ConvertTo-Json -Depth 2)"
}

# 7. Test Users Pagination
Write-Host "`n[7] Testing Users Server-Side Pagination..."
$resUsers = Invoke-RestMethod -Uri "$baseUrl/users?page=0&size=10" -Method Get -Headers $headers
if ($resUsers.page) {
    Write-Host "[PASS] GET /api/users?page=0&size=10: size=$($resUsers.page.size), total=$($resUsers.page.totalElements)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Users pagination failed: $($resUsers | ConvertTo-Json -Depth 2)" -ForegroundColor Red
}

# 8. Test Categories Pagination
Write-Host "`n[8] Testing Categories Server-Side Pagination..."
$resCats = Invoke-RestMethod -Uri "$baseUrl/categories?page=0&size=10" -Method Get -Headers $headers
if ($resCats.page) {
    Write-Host "[PASS] GET /api/categories?page=0&size=10: size=$($resCats.page.size), total=$($resCats.page.totalElements)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Categories pagination failed: $($resCats | ConvertTo-Json -Depth 2)" -ForegroundColor Red
}

# 9. Test Kitchens Pagination
Write-Host "`n[9] Testing Kitchens Server-Side Pagination..."
$resKitchens = Invoke-RestMethod -Uri "$baseUrl/kitchens?page=0&size=10" -Method Get -Headers $headers
if ($resKitchens.page) {
    Write-Host "[PASS] GET /api/kitchens?page=0&size=10: size=$($resKitchens.page.size), total=$($resKitchens.page.totalElements)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Kitchens pagination failed: $($resKitchens | ConvertTo-Json -Depth 2)" -ForegroundColor Red
}

# 10. Test Inventory Items Pagination
Write-Host "`n[10] Testing Inventory Server-Side Pagination..."
$resInv = Invoke-RestMethod -Uri "$baseUrl/inventory?page=0&size=10" -Method Get -Headers $headers
if ($resInv.page) {
    Write-Host "[PASS] GET /api/inventory?page=0&size=10: size=$($resInv.page.size), total=$($resInv.page.totalElements)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Inventory pagination failed: $($resInv | ConvertTo-Json -Depth 2)" -ForegroundColor Red
}

# 11. Test Audit Logs Pagination
Write-Host "`n[11] Testing Audit Logs Server-Side Pagination..."
$resLogs = Invoke-RestMethod -Uri "$baseUrl/settings/audit-logs?page=0&size=10" -Method Get -Headers $headers
if ($resLogs.page) {
    Write-Host "[PASS] GET /api/settings/audit-logs?page=0&size=10: size=$($resLogs.page.size), total=$($resLogs.page.totalElements)" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Audit logs pagination failed: $($resLogs | ConvertTo-Json -Depth 2)" -ForegroundColor Red
}

Write-Host "`n=================================================="
Write-Host "  ALL PAGINATION VERIFICATION CHECKS COMPLETED!"
Write-Host "=================================================="
