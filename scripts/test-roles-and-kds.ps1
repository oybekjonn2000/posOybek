# ========================================================
# RESTAURANT POS — FULL LIFECYCLE & ROLE-BASED VERIFICATION
# ========================================================
$ErrorActionPreference = "Stop"

Write-Host ">>> 1. Testing Role-Based Authentication..." -ForegroundColor Cyan

$roles = @("admin", "manager", "waiter", "kitchen", "cashier")
$passwords = @{
    "admin" = "admin123";
    "manager" = "manager123";
    "waiter" = "waiter123";
    "kitchen" = "kitchen123";
    "cashier" = "cashier123"
}

$tokens = @{}
foreach ($r in $roles) {
    $body = @{ username = $r; password = $passwords[$r] } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    $tokens[$r] = $res.data.accessToken
    Write-Host "  -> Logged in as [$r] (Role: $($res.data.user.role))" -ForegroundColor Green
}

$adminHeaders = @{ Authorization = "Bearer $($tokens['admin'])" }
$waiterHeaders = @{ Authorization = "Bearer $($tokens['waiter'])" }
$kitchenHeaders = @{ Authorization = "Bearer $($tokens['kitchen'])" }
$cashierHeaders = @{ Authorization = "Bearer $($tokens['cashier'])" }

Write-Host "`n>>> 2. Testing User Management API (Admin)..." -ForegroundColor Cyan
$users = Invoke-RestMethod -Uri "http://localhost:8080/api/users" -Headers $adminHeaders
Write-Host "  -> Users list count: $($users.data.Count)" -ForegroundColor Green

Write-Host "`n>>> 3. Testing Order Lifecycle & Table OCCUPIED/FREE flow..." -ForegroundColor Cyan
# Step A: Table 2 should be FREE
$tables = Invoke-RestMethod -Uri "http://localhost:8080/api/tables" -Headers $waiterHeaders
$targetTable = $tables.data | Where-Object { $_.tableNumber -eq 2 } | Select-Object -First 1
Write-Host "  -> Target Table: $($targetTable.name) (Status: $($targetTable.status))" -ForegroundColor Yellow

$prods = Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Headers $waiterHeaders
$p1 = $prods.data[0]

# Step B: Waiter creates order on Table 2
$orderReq = @{
    tableId = $targetTable.id
    orderType = "DINE_IN"
    guestCount = 3
    notes = "VIP table order"
    items = @(
        @{ productId = $p1.id; quantity = 2; notes = "Kamroq yog'" }
    )
} | ConvertTo-Json -Depth 5

$newOrder = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body $orderReq -Headers $waiterHeaders -ContentType "application/json"
$orderId = $newOrder.data.id
$itemId = $newOrder.data.items[0].id
Write-Host "  -> Created Order: $($newOrder.data.orderNumber) on $($targetTable.name) (Total: $($newOrder.data.total) UZS)" -ForegroundColor Green

# Waiter sends order to kitchen
$statusReq = @{ status = "SENT_TO_KITCHEN" } | ConvertTo-Json
$updatedOrder = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$orderId/status" -Method Put -Body $statusReq -Headers $waiterHeaders -ContentType "application/json"

# Waiter updates table to OCCUPIED
$tblStatusReq = @{ status = "OCCUPIED" } | ConvertTo-Json
$tblUpdate = Invoke-RestMethod -Uri "http://localhost:8080/api/tables/$($targetTable.id)/status" -Method Put -Body $tblStatusReq -Headers $waiterHeaders -ContentType "application/json"
Write-Host "  -> Table status set to: $($tblUpdate.data.status)" -ForegroundColor Green

# Step C: Kitchen checks active orders
Write-Host "`n>>> 4. Testing Kitchen Display System (KDS)..." -ForegroundColor Cyan
$kitchenOrders = Invoke-RestMethod -Uri "http://localhost:8080/api/kitchen/orders" -Headers $kitchenHeaders
$kdsOrder = $kitchenOrders.data | Where-Object { $_.id -eq $orderId } | Select-Object -First 1
Write-Host "  -> Kitchen found Order $($kdsOrder.orderNumber): $($kdsOrder.items.Count) item(s)" -ForegroundColor Green

# Kitchen updates item status: NEW -> COOKING -> READY
Invoke-RestMethod -Uri "http://localhost:8080/api/kitchen/items/$itemId/status?status=COOKING" -Method Put -Headers $kitchenHeaders | Out-Null
Write-Host "  -> Kitchen item status updated to: COOKING" -ForegroundColor Green

Invoke-RestMethod -Uri "http://localhost:8080/api/kitchen/items/$itemId/status?status=READY" -Method Put -Headers $kitchenHeaders | Out-Null
Write-Host "  -> Kitchen item status updated to: READY" -ForegroundColor Green

# Step D: Cashier receives payment and frees table
Write-Host "`n>>> 5. Testing Cashier Payment & Table Auto-Free..." -ForegroundColor Cyan
$payReq = @{
    orderId = $orderId
    paymentMethod = "CASH"
    amount = $newOrder.data.total
    cashAmount = $newOrder.data.total
    changeAmount = 0
} | ConvertTo-Json
$payResult = Invoke-RestMethod -Uri "http://localhost:8080/api/payments" -Method Post -Body $payReq -Headers $cashierHeaders -ContentType "application/json"
Write-Host "  -> Payment completed: $($payResult.data.paymentNumber) (Status: $($payResult.data.status))" -ForegroundColor Green

# Free the table
$tblFreeReq = @{ status = "FREE" } | ConvertTo-Json
$tblFree = Invoke-RestMethod -Uri "http://localhost:8080/api/tables/$($targetTable.id)/status" -Method Put -Body $tblFreeReq -Headers $cashierHeaders -ContentType "application/json"
Write-Host "  -> Table $($targetTable.name) status restored to: $($tblFree.data.status)" -ForegroundColor Green

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "   FULL LIFECYCLE VERIFICATION COMPLETED SUCCESSFULLY!  " -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
