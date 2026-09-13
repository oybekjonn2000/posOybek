# Full Security Audit End-to-End Verification Suite
# Tests OWASP API Security Top 10 defenses on live Spring Boot instance

$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  RESTAURANT POS - FULL SECURITY VERIFICATION SUITE" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

function Login-User($username, $password) {
    $body = @{ username = $username; password = $password } | ConvertTo-Json
    try {
        $res = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
        return $res.data.accessToken
    } catch {
        Write-Host "[-] Failed login for $username : $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

$adminToken = Login-User "admin" "admin123"
$cashierToken = Login-User "cashier" "cashier123"
$waiterToken = Login-User "waiter" "waiter123"
$waiter1Token = Login-User "waiter1" "waiter123"

if (-not $adminToken -or -not $waiterToken -or -not $waiter1Token) {
    Write-Host "[-] Critical: Tokens could not be obtained. Aborting." -ForegroundColor Red
    exit 1
}
Write-Host "[+] Authentication successful for admin, cashier, waiter, waiter1" -ForegroundColor Green

$headersAdmin = @{ Authorization = "Bearer $adminToken" }
$headersCashier = @{ Authorization = "Bearer $cashierToken" }
$headersWaiter = @{ Authorization = "Bearer $waiterToken" }
$headersWaiter1 = @{ Authorization = "Bearer $waiter1Token" }

$passCount = 0
$failCount = 0

function Assert-Status($testName, $expectedStatus, $scriptBlock) {
    try {
        $result = & $scriptBlock
        if ($expectedStatus -eq 200) {
            Write-Host "[PASS] $testName (Status 200 OK)" -ForegroundColor Green
            $global:passCount++
        } else {
            Write-Host "[FAIL] $testName (Expected $expectedStatus, got 200)" -ForegroundColor Red
            $global:failCount++
        }
    } catch {
        $actualStatus = 0
        if ($_.Exception.Response) {
            $actualStatus = [int]$_.Exception.Response.StatusCode
        }
        if ($actualStatus -eq $expectedStatus) {
            Write-Host "[PASS] $testName (Status $actualStatus as expected)" -ForegroundColor Green
            $global:passCount++
        } else {
            Write-Host "[FAIL] $testName (Expected $expectedStatus, got $actualStatus) - $($_.Exception.Message)" -ForegroundColor Red
            $global:failCount++
        }
    }
}

# -------------------------------------------------------------
# 1. Broken Authentication / Public endpoint audit
# -------------------------------------------------------------
Assert-Status "SEC-AUTH-01: Access /api/users without token returns 401" 401 {
    Invoke-RestMethod -Uri "$baseUrl/api/users" -Method Get
}

Assert-Status "SEC-AUTH-02: Access /api/setup/anything returns 401 or 404 (not publicly exposed)" 401 {
    Invoke-RestMethod -Uri "$baseUrl/api/setup/test" -Method Get
}

# -------------------------------------------------------------
# 2. Broken Function Level Authorization (RBAC)
# -------------------------------------------------------------
Assert-Status "SEC-RBAC-01: Waiter calling Admin /api/users returns 403" 403 {
    Invoke-RestMethod -Uri "$baseUrl/api/users" -Method Get -Headers $headersWaiter
}

Assert-Status "SEC-RBAC-02: Waiter calling /api/settings/system/info returns 403" 403 {
    Invoke-RestMethod -Uri "$baseUrl/api/settings/system/info" -Method Get -Headers $headersWaiter
}

Assert-Status "SEC-RBAC-03: Cashier calling /api/settings/system/info returns 403" 403 {
    Invoke-RestMethod -Uri "$baseUrl/api/settings/system/info" -Method Get -Headers $headersCashier
}

Assert-Status "SEC-RBAC-04: Admin calling /api/settings/system/info returns 200" 200 {
    Invoke-RestMethod -Uri "$baseUrl/api/settings/system/info" -Method Get -Headers $headersAdmin
}

# -------------------------------------------------------------
# 3. Waiter Order & Table Isolation (BOLA)
# -------------------------------------------------------------
# Find free table
$tables = Invoke-RestMethod -Uri "$baseUrl/api/tables" -Method Get -Headers $headersWaiter
$freeTable = $tables.data | Where-Object { $_.status -eq "FREE" } | Select-Object -First 1
$tableId = $freeTable.id

# Waiter 1 occupies table
$occupied = Invoke-RestMethod -Uri "$baseUrl/api/tables/$tableId/occupy" -Method Post -Headers $headersWaiter
$orderId = $occupied.data.currentOrderId
Write-Host "[+] Waiter 1 occupied table $($freeTable.name), active orderId: $orderId" -ForegroundColor Cyan

# Waiter 2 (waiter1) attempts to release Waiter 1's table -> 403
Assert-Status "SEC-BOLA-01: Waiter 2 attempting to release Waiter 1's table returns 403" 403 {
    Invoke-RestMethod -Uri "$baseUrl/api/tables/$tableId/release" -Method Post -Headers $headersWaiter1
}

# Waiter 2 attempts to update status of Waiter 1's table -> 403
Assert-Status "SEC-BOLA-02: Waiter 2 attempting to modify Waiter 1's table status returns 403" 403 {
    $body = @{ status = "FREE" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/tables/$tableId/status" -Method Put -Body $body -ContentType "application/json" -Headers $headersWaiter1
}

# Waiter 2 attempts to read Waiter 1's order -> 403
Assert-Status "SEC-BOLA-03: Waiter 2 attempting to read Waiter 1's order returns 403" 403 {
    Invoke-RestMethod -Uri "$baseUrl/api/orders/$orderId" -Method Get -Headers $headersWaiter1
}

# Waiter 2 attempts to add items to Waiter 1's order -> 403
$products = Invoke-RestMethod -Uri "$baseUrl/api/products?activeOnly=true" -Method Get -Headers $headersWaiter
$sampleProduct = $products.data | Select-Object -First 1
$prodId = $sampleProduct.id

Assert-Status "SEC-BOLA-04: Waiter 2 attempting to add items to Waiter 1's order returns 403" 403 {
    $body = @{ items = @(@{ productId = $prodId; quantity = 1 }) } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/orders/$orderId/items" -Method Post -Body $body -ContentType "application/json" -Headers $headersWaiter1
}

# -------------------------------------------------------------
# 4. Quantity & Price Manipulation
# -------------------------------------------------------------
# Waiter 1 attempts to add item with negative quantity -> 400
Assert-Status "SEC-INPUT-01: Adding item with negative quantity returns 400" 400 {
    $body = @{ items = @(@{ productId = $prodId; quantity = -5 }) } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/orders/$orderId/items" -Method Post -Body $body -ContentType "application/json" -Headers $headersWaiter
}

# Waiter 1 attempts to add item with zero quantity -> 400
Assert-Status "SEC-INPUT-02: Adding item with zero quantity returns 400" 400 {
    $body = @{ items = @(@{ productId = $prodId; quantity = 0 }) } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/orders/$orderId/items" -Method Post -Body $body -ContentType "application/json" -Headers $headersWaiter
}

# Waiter 1 adds legitimate item with price tampering in payload
$tamperedBody = @{
    items = @(@{
        productId = $prodId
        quantity = 2
        unitPrice = 1   # Malicious attempt to change price to 1 UZS
        price = 1
    })
} | ConvertTo-Json
$updatedOrder = Invoke-RestMethod -Uri "$baseUrl/api/orders/$orderId/items" -Method Post -Body $tamperedBody -ContentType "application/json" -Headers $headersWaiter

$dbPrice = [decimal]$sampleProduct.salePrice
$expectedTotal = $dbPrice * 2
$actualTotal = [decimal]$updatedOrder.data.total
if ($actualTotal -eq $expectedTotal) {
    Write-Host "[PASS] SEC-PRICE-01: Server computed total from database price ($actualTotal UZS), ignored client price" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "[FAIL] SEC-PRICE-01: Client manipulated price accepted! Total: $actualTotal, expected: $expectedTotal" -ForegroundColor Red
    $failCount++
}

# -------------------------------------------------------------
# 5. Status & Discount Manipulation
# -------------------------------------------------------------
# Waiter 1 attempts to set status to PAID directly -> 400
Assert-Status "SEC-STATUS-01: Manually updating status to PAID returns 400" 400 {
    $body = @{ status = "PAID" } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/orders/$orderId/status" -Method Put -Body $body -ContentType "application/json" -Headers $headersWaiter
}

# Negative discount attempt -> 400
Assert-Status "SEC-DISC-01: Negative discount percent returns 400" 400 {
    $body = @{ percent = -20.0 } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/orders/$orderId/discount" -Method Put -Body $body -ContentType "application/json" -Headers $headersCashier
}

# -------------------------------------------------------------
# 6. Payment Security & Underpayment Guard
# -------------------------------------------------------------
# Cashier attempts underpayment (1 UZS on multi-thousand order) -> 400
Assert-Status "SEC-PAY-01: Underpayment (amount: 1 UZS) returns 400" 400 {
    $payBody = @{
        orderId = $orderId
        paymentMethod = "CASH"
        amount = 1
        cashAmount = 1
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/payments" -Method Post -Body $payBody -ContentType "application/json" -Headers $headersCashier
}

# Legitimate payment covering full order total -> 200
Assert-Status "SEC-PAY-02: Legitimate full payment completes successfully (200 OK)" 200 {
    $payBody = @{
        orderId = $orderId
        paymentMethod = "CASH"
        amount = $actualTotal
        cashAmount = $actualTotal
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/payments" -Method Post -Body $payBody -ContentType "application/json" -Headers $headersCashier
}

# Duplicate payment attempt on already paid order -> 400
Assert-Status "SEC-PAY-03: Duplicate payment on already paid order returns 400" 400 {
    $payBody = @{
        orderId = $orderId
        paymentMethod = "CASH"
        amount = $actualTotal
        cashAmount = $actualTotal
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/payments" -Method Post -Body $payBody -ContentType "application/json" -Headers $headersCashier
}

# -------------------------------------------------------------
# 7. File Upload & Magic Bytes
# -------------------------------------------------------------
$fakeImagePath = "e:/posOybek/scratch/fake_shell.png"
[System.IO.File]::WriteAllText($fakeImagePath, "<?php echo 'malicious'; ?>")
$uploadCode = (curl.exe -s -o NUL -w "%{http_code}" -X POST "$baseUrl/api/products/upload-image" -H "Authorization: Bearer $adminToken" -F "file=@$fakeImagePath;type=image/png")
if ($uploadCode -eq "400") {
    Write-Host "[PASS] SEC-UPLOAD-01: Uploading spoofed file without valid image magic bytes returns 400" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "[FAIL] SEC-UPLOAD-01: Uploading spoofed file expected 400, got $uploadCode" -ForegroundColor Red
    $failCount++
}

# Valid 1x1 PNG upload test
$validPngBytes = [byte[]]@(
    0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
    0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
    0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4, 0x89, 0x00, 0x00, 0x00,
    0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
    0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49,
    0x45, 0x4E, 0x44, 0xAE, 0x42, 0x60, 0x82
)
$validPngPath = "e:/posOybek/scratch/valid_dot.png"
[System.IO.File]::WriteAllBytes($validPngPath, $validPngBytes)
$validUploadCode = (curl.exe -s -o NUL -w "%{http_code}" -X POST "$baseUrl/api/products/upload-image" -H "Authorization: Bearer $adminToken" -F "file=@$validPngPath;type=image/png")
if ($validUploadCode -eq "200") {
    Write-Host "[PASS] SEC-UPLOAD-02: Uploading valid PNG with genuine magic bytes returns 200" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "[FAIL] SEC-UPLOAD-02: Uploading valid PNG expected 200, got $validUploadCode" -ForegroundColor Red
    $failCount++
}

# -------------------------------------------------------------
# 8. Inventory Security (RBAC)
# -------------------------------------------------------------
Assert-Status "SEC-INV-01: Waiter attempting inventory adjustment returns 403" 403 {
    $invBody = @{
        newQuantity = 100
        reason = "Unauthorized tamper"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/inventory/$prodId/adjust" -Method Patch -Body $invBody -ContentType "application/json" -Headers $headersWaiter
}

# -------------------------------------------------------------
# 9. HTTP Security Headers
# -------------------------------------------------------------
$headResponse = curl.exe -s -I "$baseUrl/actuator/health"
if ($headResponse -match "X-Frame-Options: DENY" -and
    $headResponse -match "X-Content-Type-Options: nosniff" -and
    $headResponse -match "Content-Security-Policy:" -and
    $headResponse -match "Referrer-Policy: strict-origin-when-cross-origin") {
    Write-Host "[PASS] SEC-HEAD-01: Strict security headers verified (X-Frame-Options, nosniff, CSP, Referrer-Policy)" -ForegroundColor Green
    $passCount++
} else {
    Write-Host "[FAIL] SEC-HEAD-01: Missing expected HTTP security headers!" -ForegroundColor Red
    $failCount++
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  TEST RESULTS SUMMARY" -ForegroundColor Cyan
Write-Host "  Passed: $passCount" -ForegroundColor Green
Write-Host "  Failed: $failCount" -ForegroundColor $(if ($failCount -gt 0) { "Red" } else { "Green" })
Write-Host "==========================================================" -ForegroundColor Cyan
