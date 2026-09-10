$baseUrl = "http://localhost:8080"

function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token = "",
        [object]$Body = $null
    )
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    $params = @{
        Uri = "$baseUrl$Path"
        Method = $Method
        Headers = $headers
        ContentType = "application/json"
    }
    if ($Body -ne $null) {
        $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
    }
    try {
        $res = Invoke-RestMethod @params
        return @{ StatusCode = 200; Data = $res; Success = $true }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if (-not $statusCode -and $_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        return @{ StatusCode = $statusCode; Error = $_; Success = $false }
    }
}

function Login-User {
    param([string]$Username, [string]$Password)
    $res = Invoke-ApiRequest -Method "Post" -Path "/api/auth/login" -Body @{ username = $Username; password = $Password }
    if ($res.Success -and $res.Data.data.accessToken) {
        return $res.Data.data.accessToken
    }
    throw "Failed to login as $Username"
}

Write-Host "=== RESETTING DATABASE FOR CLEAN TEST EXECUTION ===" -ForegroundColor Magenta
& powershell -ExecutionPolicy Bypass -File e:\posOybek\scratch\reset_test_state.ps1

Write-Host "`n=== STARTING WAITER & KITCHEN ISOLATION TEST SUITE ===" -ForegroundColor Cyan

# 0. Logins
$waiter1Token = Login-User "waiter1" "waiter123"
$waiter2Token = Login-User "waiter2" "waiter123"
$pizzaToken   = Login-User "pizza"   "pizza123"
$somsaToken   = Login-User "somsa"   "somsa123"
$adminToken   = Login-User "admin"   "admin123"
Write-Host "Tokens acquired successfully for waiter1, waiter2, pizza, somsa, admin" -ForegroundColor Green

$table1Id = "e1000000-0000-0000-0000-000000000001"
$table2Id = "e1000000-0000-0000-0000-000000000002"
$margaritaId = "a1000000-0000-0000-0000-000000000031"
$colaId = "a1000000-0000-0000-0000-000000000033"
$somsaId = "a1000000-0000-0000-0000-000000000035"

# =========================================================================
# TEST 1: waiter1 occupies Table 1
# =========================================================================
Write-Host "`n--- TEST 1: Waiter 1 occupies Table 1 ---" -ForegroundColor Yellow
$occ1 = Invoke-ApiRequest -Method "Post" -Path "/api/tables/$table1Id/occupy" -Token $waiter1Token
if ($occ1.Success -and $occ1.Data.data.status -eq "OCCUPIED" -and $occ1.Data.data.myTable -eq $true) {
    $order1Id = $occ1.Data.data.currentOrderId
    Write-Host "PASS: Table 1 occupied by waiter 1! OrderId: $order1Id, Waiter: $($occ1.Data.data.waiterName)" -ForegroundColor Green
} else {
    Write-Host "FAIL: Table 1 occupation failed: $($occ1.Error)" -ForegroundColor Red
    exit 1
}

# =========================================================================
# TEST 2: waiter2 tries to open / occupy Table 1
# =========================================================================
Write-Host "`n--- TEST 2: Waiter 2 blocked from Table 1 ---" -ForegroundColor Yellow
# 2a. Occupy attempt
$occ2 = Invoke-ApiRequest -Method "Post" -Path "/api/tables/$table1Id/occupy" -Token $waiter2Token
if ($occ2.StatusCode -eq 403 -or $occ2.StatusCode -eq 400) {
    Write-Host "PASS: Waiter 2 blocked from occupying Table 1 (HTTP $($occ2.StatusCode))" -ForegroundColor Green
} else {
    Write-Host "FAIL: Waiter 2 was NOT blocked from occupying Table 1! Code: $($occ2.StatusCode)" -ForegroundColor Red
}

# 2b. Direct table details attempt
$tabDetails = Invoke-ApiRequest -Method "Get" -Path "/api/tables/$table1Id" -Token $waiter2Token
if ($tabDetails.StatusCode -eq 403) {
    Write-Host "PASS: Direct GET /api/tables/$table1Id returned 403 Forbidden for waiter 2" -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected 403 Forbidden for waiter 2 accessing Table 1, got $($tabDetails.StatusCode)" -ForegroundColor Red
}

# 2c. Table list sanitization for waiter 2
$tabList2 = Invoke-ApiRequest -Method "Get" -Path "/api/tables" -Token $waiter2Token
$t1ForW2 = $tabList2.Data.data | Where-Object { $_.id -eq $table1Id }
if ($t1ForW2.status -eq "OCCUPIED" -and $t1ForW2.myTable -eq $false -and $t1ForW2.currentOrderId -eq $null -and $t1ForW2.itemCount -eq 0 -and $t1ForW2.totalAmount -eq 0) {
    Write-Host "PASS: In Table list, Table 1 data is properly sanitized for waiter 2 (no order details leaked)" -ForegroundColor Green
} else {
    Write-Host "FAIL: Leaked Table 1 data to waiter 2: $($t1ForW2 | ConvertTo-Json -Depth 2)" -ForegroundColor Red
}

# =========================================================================
# TEST 3: waiter1 adds items to Table 1, waiter2 cannot see them
# =========================================================================
Write-Host "`n--- TEST 3: Waiter 1 adds items, Waiter 2 cannot see them ---" -ForegroundColor Yellow
$itemsBody = @{
    items = @(
        @{ productId = $margaritaId; quantity = 2 },
        @{ productId = $colaId; quantity = 1 }
    )
}
$addRes = Invoke-ApiRequest -Method "Post" -Path "/api/orders/$order1Id/items" -Token $waiter1Token -Body $itemsBody
$sendRes = Invoke-ApiRequest -Method "Post" -Path "/api/orders/$order1Id/send-to-kitchen" -Token $waiter1Token
if ($addRes.Success -and $sendRes.Success) {
    Write-Host "PASS: Waiter 1 added Margarita x2 and Cola x1, and sent to kitchen. Total: $($sendRes.Data.data.totalAmount)" -ForegroundColor Green
} else {
    Write-Host "FAIL: Adding items or sending to kitchen failed" -ForegroundColor Red
}

# Check waiter 2 table list again: items and total must remain 0
$tabList2Again = Invoke-ApiRequest -Method "Get" -Path "/api/tables" -Token $waiter2Token
$t1ForW2Again = $tabList2Again.Data.data | Where-Object { $_.id -eq $table1Id }
if ($t1ForW2Again.itemCount -eq 0 -and $t1ForW2Again.totalAmount -eq 0 -and $t1ForW2Again.currentOrderId -eq $null) {
    Write-Host "PASS: Waiter 2 still sees 0 items and 0 sum for Table 1" -ForegroundColor Green
} else {
    Write-Host "FAIL: Waiter 2 saw Table 1 items/sum: $($t1ForW2Again | ConvertTo-Json -Depth 2)" -ForegroundColor Red
}

# =========================================================================
# TEST 4: waiter2 occupies Table 2, adds Somsa x2; waiter1 cannot see it
# =========================================================================
Write-Host "`n--- TEST 4: Waiter 2 occupies Table 2, adds Somsa; Waiter 1 cannot see it ---" -ForegroundColor Yellow
$occTable2 = Invoke-ApiRequest -Method "Post" -Path "/api/tables/$table2Id/occupy" -Token $waiter2Token
$order2Id = $occTable2.Data.data.currentOrderId
Write-Host "Waiter 2 occupied Table 2, orderId: $order2Id" -ForegroundColor Cyan

$somsaBody = @{
    items = @(
        @{ productId = $somsaId; quantity = 2 }
    )
}
$addSomsa = Invoke-ApiRequest -Method "Post" -Path "/api/orders/$order2Id/items" -Token $waiter2Token -Body $somsaBody
$sendSomsa = Invoke-ApiRequest -Method "Post" -Path "/api/orders/$order2Id/send-to-kitchen" -Token $waiter2Token
Write-Host "Waiter 2 added Somsa x2 and sent to kitchen" -ForegroundColor Cyan

# Check waiter 1 table list: Table 2 items and total must be 0
$tabList1 = Invoke-ApiRequest -Method "Get" -Path "/api/tables" -Token $waiter1Token
$t2ForW1 = $tabList1.Data.data | Where-Object { $_.id -eq $table2Id }
if ($t2ForW1.status -eq "OCCUPIED" -and $t2ForW1.myTable -eq $false -and $t2ForW1.itemCount -eq 0 -and $t2ForW1.totalAmount -eq 0 -and $t2ForW1.currentOrderId -eq $null) {
    Write-Host "PASS: Waiter 1 cannot see Waiter 2's Table 2 order details (sanitized)" -ForegroundColor Green
} else {
    Write-Host "FAIL: Waiter 1 saw Table 2 data: $($t2ForW1 | ConvertTo-Json -Depth 2)" -ForegroundColor Red
}

# =========================================================================
# TEST 5: waiter1 order history / active orders (only waiter1's orders)
# =========================================================================
Write-Host "`n--- TEST 5: Waiter 1 orders list isolation ---" -ForegroundColor Yellow
$w1Active = Invoke-ApiRequest -Method "Get" -Path "/api/orders" -Token $waiter1Token
$w1Ids = $w1Active.Data.data | ForEach-Object { $_.id }
if (($w1Ids -contains $order1Id) -and (-not ($w1Ids -contains $order2Id))) {
    Write-Host "PASS: Waiter 1 active orders contains Order 1 and DOES NOT contain Order 2" -ForegroundColor Green
} else {
    Write-Host "FAIL: Waiter 1 active orders leaked or missing: $w1Ids" -ForegroundColor Red
}

# =========================================================================
# TEST 6: waiter2 order history / active orders (only waiter2's orders)
# =========================================================================
Write-Host "`n--- TEST 6: Waiter 2 orders list isolation ---" -ForegroundColor Yellow
$w2Active = Invoke-ApiRequest -Method "Get" -Path "/api/orders" -Token $waiter2Token
$w2Ids = $w2Active.Data.data | ForEach-Object { $_.id }
if (($w2Ids -contains $order2Id) -and (-not ($w2Ids -contains $order1Id))) {
    Write-Host "PASS: Waiter 2 active orders contains Order 2 and DOES NOT contain Order 1" -ForegroundColor Green
} else {
    Write-Host "FAIL: Waiter 2 active orders leaked or missing: $w2Ids" -ForegroundColor Red
}

# =========================================================================
# TEST 7: waiter2 tries to access waiter1's order directly by ID -> 403
# =========================================================================
Write-Host "`n--- TEST 7: Waiter 2 directly accessing Waiter 1 Order -> 403 Forbidden ---" -ForegroundColor Yellow
$w2GetW1Order = Invoke-ApiRequest -Method "Get" -Path "/api/orders/$order1Id" -Token $waiter2Token
if ($w2GetW1Order.StatusCode -eq 403) {
    Write-Host "PASS: GET /api/orders/$order1Id by waiter2 returned 403 Forbidden" -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected 403 Forbidden, got $($w2GetW1Order.StatusCode)" -ForegroundColor Red
}

# Waiter 2 tries to tamper with waiter 1 order items
$w2AddW1 = Invoke-ApiRequest -Method "Post" -Path "/api/orders/$order1Id/items" -Token $waiter2Token -Body $somsaBody
if ($w2AddW1.StatusCode -eq 403) {
    Write-Host "PASS: POST /api/orders/$order1Id/items by waiter2 returned 403 Forbidden" -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected 403 Forbidden, got $($w2AddW1.StatusCode)" -ForegroundColor Red
}

# =========================================================================
# TEST 8: pizza login -> only Pitsaxona tickets, amounts sanitized
# =========================================================================
Write-Host "`n--- TEST 8: Pizza kitchen station ticket isolation ---" -ForegroundColor Yellow
$pizzaOrders = Invoke-ApiRequest -Method "Get" -Path "/api/kitchen/orders" -Token $pizzaToken
$pizzaTickets = $pizzaOrders.Data.data
Write-Host "Pizza tickets received: $($pizzaTickets.Count)" -ForegroundColor Cyan
$pizzaItemNames = @()
foreach ($t in $pizzaTickets) {
    foreach ($item in $t.items) {
        $pizzaItemNames += $item.productName
    }
}
Write-Host "Pizza items: $($pizzaItemNames -join ', ')" -ForegroundColor Cyan
if (($pizzaItemNames -contains "Margarita Pizza") -and (-not ($pizzaItemNames -contains "Go‘shtli Somsa"))) {
    Write-Host "PASS: Pizza station received ONLY Pizza items and NOT Somsa items" -ForegroundColor Green
} else {
    Write-Host "FAIL: Cross-station leakage in pizza station! Items: $($pizzaItemNames -join ', ')" -ForegroundColor Red
}
$hasFinancialLeak = $pizzaTickets | Where-Object { $_.totalAmount -gt 0 }
if (-not $hasFinancialLeak) {
    Write-Host "PASS: Financial totals sanitized for kitchen station (totalAmount == 0)" -ForegroundColor Green
} else {
    Write-Host "FAIL: Financial total leaked to kitchen!" -ForegroundColor Red
}

# =========================================================================
# TEST 9: pizza user attempts to access /api/orders -> 403 Forbidden
# =========================================================================
Write-Host "`n--- TEST 9: Pizza user blocked from general Orders API ---" -ForegroundColor Yellow
$pizzaOrderAccess = Invoke-ApiRequest -Method "Get" -Path "/api/orders" -Token $pizzaToken
if ($pizzaOrderAccess.StatusCode -eq 403) {
    Write-Host "PASS: GET /api/orders by pizza user returned 403 Forbidden" -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected 403 Forbidden for pizza user on /api/orders, got $($pizzaOrderAccess.StatusCode)" -ForegroundColor Red
}

# =========================================================================
# TEST 10: pizza user requests somsa ticket -> 403 Forbidden
# =========================================================================
Write-Host "`n--- TEST 10: Cross-station ticket access blocked ---" -ForegroundColor Yellow
$somsaOrders = Invoke-ApiRequest -Method "Get" -Path "/api/kitchen/orders" -Token $somsaToken
$somsaTicketId = $somsaOrders.Data.data[0].id
Write-Host "Somsa station ticket ID: $somsaTicketId" -ForegroundColor Cyan

$pizzaSomsaAccess = Invoke-ApiRequest -Method "Get" -Path "/api/kitchen/orders/$somsaTicketId" -Token $pizzaToken
if ($pizzaSomsaAccess.StatusCode -eq 403) {
    Write-Host "PASS: GET /api/kitchen/orders/$somsaTicketId by pizza user returned 403 Forbidden" -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected 403 Forbidden for pizza accessing somsa ticket, got $($pizzaSomsaAccess.StatusCode)" -ForegroundColor Red
}

$pizzaSomsaUpdate = Invoke-ApiRequest -Method "Put" -Path "/api/kitchen/orders/$somsaTicketId/status" -Token $pizzaToken -Body @{ status = "PREPARING" }
if ($pizzaSomsaUpdate.StatusCode -eq 403) {
    Write-Host "PASS: PUT status on somsa ticket by pizza user returned 403 Forbidden" -ForegroundColor Green
} else {
    Write-Host "FAIL: Expected 403 Forbidden on updating somsa ticket by pizza, got $($pizzaSomsaUpdate.StatusCode)" -ForegroundColor Red
}

# =========================================================================
# TEST 11: Payment frees table -> Table 1 becomes FREE & waiter_id = null -> Waiter 2 can occupy Table 1!
# =========================================================================
Write-Host "`n--- TEST 11: Payment frees table and waiter2 occupies Table 1 ---" -ForegroundColor Yellow
# Admin processes payment for Order 1
$payBody = @{
    orderId = $order1Id
    paymentMethod = "CASH"
    amount = 102000
    cashAmount = 102000
}
$payRes = Invoke-ApiRequest -Method "Post" -Path "/api/payments" -Token $adminToken -Body $payBody
if ($payRes.Success) {
    Write-Host "PASS: Payment processed successfully for Order 1" -ForegroundColor Green
} else {
    Write-Host "FAIL: Payment processing failed: $($payRes.Error)" -ForegroundColor Red
    exit 1
}

# Check Table 1 status via Admin/Waiter
$t1AfterPay = Invoke-ApiRequest -Method "Get" -Path "/api/tables/$table1Id" -Token $adminToken
if ($t1AfterPay.Data.data.status -eq "FREE" -and $t1AfterPay.Data.data.waiterId -eq $null) {
    Write-Host "PASS: Table 1 status is FREE and waiterId is NULL after payment" -ForegroundColor Green
} else {
    Write-Host "FAIL: Table 1 not freed after payment! Status: $($t1AfterPay.Data.data.status), WaiterId: $($t1AfterPay.Data.data.waiterId)" -ForegroundColor Red
}

# Waiter 2 now occupies Table 1
$w2OccupyT1 = Invoke-ApiRequest -Method "Post" -Path "/api/tables/$table1Id/occupy" -Token $waiter2Token
if ($w2OccupyT1.Success -and $w2OccupyT1.Data.data.status -eq "OCCUPIED" -and $w2OccupyT1.Data.data.myTable -eq $true -and ($w2OccupyT1.Data.data.waiterName -like "*Ikkinchi*" -or $w2OccupyT1.Data.data.waiterName -like "*2*")) {
    Write-Host "PASS: Table 1 successfully occupied by Waiter 2 ($($w2OccupyT1.Data.data.waiterName)) after being freed by payment!" -ForegroundColor Green
} else {
    Write-Host "FAIL: Waiter 2 failed to occupy freed Table 1: $($w2OccupyT1.Error)" -ForegroundColor Red
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "   ALL 11 ISOLATION SCENARIOS COMPLETED AND TESTED!" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
