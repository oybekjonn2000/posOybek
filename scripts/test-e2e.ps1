# ========================================================
# RESTAURANT POS — END-TO-END AUTOMATED VERIFICATION SUITE
# ========================================================

$ErrorActionPreference = "Stop"
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "       RESTAURANT POS - END-TO-END VERIFICATION" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# 1. Health Check
Write-Host "`n[1/8] Verifying API Health..." -ForegroundColor Yellow
$health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"
if ($health.status -ne "UP") { throw "API Health check failed: $($health.status)" }
Write-Host "  -> Health Status: UP" -ForegroundColor Green

# 2. Authentication
Write-Host "`n[2/8] Authenticating Admin User (admin / admin123)..." -ForegroundColor Yellow
$loginBody = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$auth = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $auth.data.accessToken
$headers = @{ Authorization = "Bearer $token" }
Write-Host "  -> Authenticated as: $($auth.data.user.fullName) (Tenant: $($auth.data.user.tenantId))" -ForegroundColor Green

# 3. Categories & Products
Write-Host "`n[3/8] Loading Categories and Products..." -ForegroundColor Yellow
$categories = Invoke-RestMethod -Uri "http://localhost:8080/api/categories" -Headers $headers
$products = Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Headers $headers
Write-Host "  -> Loaded $($categories.data.Count) Categories and $($products.data.Count) Products" -ForegroundColor Green

# 4. Tables and Dining Zones
Write-Host "`n[4/8] Loading Dining Zones and Tables..." -ForegroundColor Yellow
$zones = Invoke-RestMethod -Uri "http://localhost:8080/api/tables/zones" -Headers $headers
$tables = Invoke-RestMethod -Uri "http://localhost:8080/api/tables" -Headers $headers
Write-Host "  -> Loaded $($zones.data.Count) Zones and $($tables.data.Count) Tables" -ForegroundColor Green

# 5. Work Shift
Write-Host "`n[5/8] Checking / Opening Work Shift..." -ForegroundColor Yellow
$currentShift = Invoke-RestMethod -Uri "http://localhost:8080/api/shifts/current" -Headers $headers
if (-not $currentShift.data) {
    $shiftBody = @{ openingCash = 500000; notes = "Daily Shift" } | ConvertTo-Json
    $currentShift = Invoke-RestMethod -Uri "http://localhost:8080/api/shifts/open" -Method Post -Body $shiftBody -Headers $headers -ContentType "application/json"
}
Write-Host "  -> Active Shift: $($currentShift.data.shiftNumber) (Cash: $($currentShift.data.openingCash) UZS)" -ForegroundColor Green

# 6. Order Placement
Write-Host "`n[6/8] Placing Order on Table $($tables.data[0].tableNumber)..." -ForegroundColor Yellow
$orderBody = @{
    tableId = $tables.data[0].id
    orderType = "DINE_IN"
    guestCount = 2
    notes = "Automated test order"
    items = @(
        @{ productId = $products.data[0].id; quantity = 1 },
        @{ productId = $products.data[1].id; quantity = 1 }
    )
} | ConvertTo-Json -Depth 5
$order = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body $orderBody -Headers $headers -ContentType "application/json"
Write-Host "  -> Order Created: $($order.data.orderNumber) (Total: $($order.data.total) UZS)" -ForegroundColor Green

# 7. Payment Processing
Write-Host "`n[7/8] Processing Cash Payment..." -ForegroundColor Yellow
$payBody = @{
    orderId = $order.data.id
    paymentMethod = "CASH"
    amount = $order.data.total
    cashAmount = $order.data.total
    changeAmount = 0
} | ConvertTo-Json
$payment = Invoke-RestMethod -Uri "http://localhost:8080/api/payments" -Method Post -Body $payBody -Headers $headers -ContentType "application/json"
Write-Host "  -> Payment Succeeded: $($payment.data.paymentNumber) (Amount: $($payment.data.amount) UZS)" -ForegroundColor Green

# 8. Offline-First Sync Status
Write-Host "`n[8/8] Checking Offline Sync Status..." -ForegroundColor Yellow
$sync = Invoke-RestMethod -Uri "http://localhost:8080/api/sync/status" -Headers $headers
Write-Host "  -> Sync Engine Status: $($sync.data.status) (Pending Events: $($sync.data.pendingEvents))" -ForegroundColor Green

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "   ALL 8 VERIFICATION TESTS PASSED SUCCESSFULLY!       " -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
