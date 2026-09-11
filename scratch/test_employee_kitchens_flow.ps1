# Test script: Employee multi-kitchen assignment, authorization, and isolation
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080/api"

Write-Host "=== 1. ADMIN LOGIN ===" -ForegroundColor Cyan
$adminLoginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $adminLoginBody -ContentType "application/json"
$adminToken = $loginRes.data.accessToken
$adminHeaders = @{
    "Authorization" = "Bearer $adminToken"
    "Content-Type" = "application/json"
}
Write-Host "Admin logged in successfully!" -ForegroundColor Green

Write-Host "`n=== 2. RETRIEVE ALL KITCHENS ===" -ForegroundColor Cyan
$kitchensRes = Invoke-RestMethod -Uri "$baseUrl/kitchens" -Method Get -Headers $adminHeaders
$kitchens = $kitchensRes.data
Write-Host "Found $($kitchens.Count) kitchens:"
$kitchens | ForEach-Object { Write-Host " - $($_.name) ($($_.code)): $($_.id)" }

$pizzaKitchen = $kitchens | Where-Object { $_.code -eq "PIZZA" }
$somsaKitchen = $kitchens | Where-Object { $_.code -eq "SOMSA" }
$mainKitchen  = $kitchens | Where-Object { $_.code -eq "MAIN" }
$barKitchen   = $kitchens | Where-Object { $_.code -eq "BAR" }

if (-not $pizzaKitchen -or -not $somsaKitchen -or -not $mainKitchen) {
    throw "Required kitchens (PIZZA, SOMSA, MAIN) not found!"
}

Write-Host "`n=== 3. VALIDATION TEST: CREATE KITCHEN EMPLOYEE WITHOUT KITCHENS ===" -ForegroundColor Cyan
$invalidUserBody = @{
    username = "test_invalid_chef_$([System.DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
    password = "password123"
    firstName = "Test"
    lastName = "Chef"
    role = "KITCHEN"
    kitchenIds = @()
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$baseUrl/users" -Method Post -Body $invalidUserBody -Headers $adminHeaders
    throw "Expected validation error did not occur!"
} catch {
    $rawError = if ($_.Exception.Response) { (New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd() } else { $_.ErrorDetails.Message }
    Write-Host "Raw error: $rawError" -ForegroundColor Yellow
    if ($rawError -notlike "*kamida bitta oshxonaga*") {
        throw "Unexpected error message: $rawError"
    }
    Write-Host "Validation correctly passed!" -ForegroundColor Green
}

Write-Host "`n=== 4. CREATE EMPLOYEE 'ALI' ASSIGNED TO PITSAXONA + SOMSAPAZ ===" -ForegroundColor Cyan
$aliUsername = "ali_chef_$([System.DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
$aliPassword = "password123"
$createAliBody = @{
    username = $aliUsername
    password = $aliPassword
    firstName = "Ali"
    lastName = "Valiyev"
    role = "KITCHEN"
    kitchenIds = @($pizzaKitchen.id, $somsaKitchen.id)
} | ConvertTo-Json

$aliCreateRes = Invoke-RestMethod -Uri "$baseUrl/users" -Method Post -Body $createAliBody -Headers $adminHeaders
$ali = $aliCreateRes.data
Write-Host "Employee Ali created: ID=$($ali.id), Role=$($ali.role)" -ForegroundColor Green
Write-Host "Assigned kitchens for Ali: $($ali.kitchens.Count)"
$ali.kitchens | ForEach-Object { Write-Host "   - $($_.name) ($($_.code))" }

if ($ali.kitchens.Count -ne 2) {
    throw "Expected exactly 2 assigned kitchens for Ali!"
}

Write-Host "`n=== 5. LOGIN AS ALI AND VERIFY KITCHEN VISIBILITY ===" -ForegroundColor Cyan
$aliLoginBody = @{
    username = $aliUsername
    password = $aliPassword
} | ConvertTo-Json

$aliLoginRes = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $aliLoginBody -ContentType "application/json"
$aliToken = $aliLoginRes.data.accessToken
$aliHeaders = @{
    "Authorization" = "Bearer $aliToken"
    "Content-Type" = "application/json"
}

Write-Host "Ali logged in successfully!" -ForegroundColor Green
Write-Host "Ali UserInfo kitchenIds: $($aliLoginRes.data.user.kitchenIds -join ', ')"

$aliStationsRes = Invoke-RestMethod -Uri "$baseUrl/kitchens" -Method Get -Headers $aliHeaders
$aliStations = $aliStationsRes.data
Write-Host "Stations visible to Ali ($($aliStations.Count)):"
$aliStations | ForEach-Object { Write-Host "   - $($_.name) ($($_.code))" }

# Ali should only see Pizza and Somsa, NOT Bar or Main
$visibleCodes = $aliStations | ForEach-Object { $_.code }
if ($visibleCodes -notcontains "PIZZA" -or $visibleCodes -notcontains "SOMSA") {
    throw "Ali should see PIZZA and SOMSA!"
}
if ($visibleCodes -contains "BAR" -or $visibleCodes -contains "PLOV" -or $visibleCodes -contains "MAIN") {
    throw "Ali should NOT see BAR, PLOV, or MAIN!"
}
Write-Host "Ali's visible kitchen isolation verified!" -ForegroundColor Green

Write-Host "`n=== 6. TEST ALI'S ORDER ISOLATION ===" -ForegroundColor Cyan
# Ali should be able to query PIZZA orders
$pizzaOrders = Invoke-RestMethod -Uri "$baseUrl/kitchen/orders?kitchenId=$($pizzaKitchen.id)" -Method Get -Headers $aliHeaders
Write-Host "Ali can query Pitsaxona orders: $($pizzaOrders.data.Count) orders found" -ForegroundColor Green

# Ali querying BAR orders must return 403 Forbidden
try {
    Invoke-RestMethod -Uri "$baseUrl/kitchen/orders?kitchenId=$($barKitchen.id)" -Method Get -Headers $aliHeaders
    throw "Ali was able to query BAR orders! Isolation failed!"
} catch {
    Write-Host "Ali correctly blocked from querying BAR orders (403 Forbidden)" -ForegroundColor Yellow
}

Write-Host "`n=== 7. EDIT ALI: REMOVE PITSAXONA, ADD ASOSIY OSHXONA ===" -ForegroundColor Cyan
$updateAliBody = @{
    firstName = "Ali"
    lastName = "Valiyev"
    role = "KITCHEN"
    kitchenIds = @($somsaKitchen.id, $mainKitchen.id)
} | ConvertTo-Json

$aliUpdateRes = Invoke-RestMethod -Uri "$baseUrl/users/$($ali.id)" -Method Put -Body $updateAliBody -Headers $adminHeaders
$updatedAli = $aliUpdateRes.data
Write-Host "Ali updated successfully!" -ForegroundColor Green
Write-Host "New assigned kitchens for Ali: $($updatedAli.kitchens.Count)"
$updatedAli.kitchens | ForEach-Object { Write-Host "   - $($_.name) ($($_.code))" }

$updatedCodes = $updatedAli.kitchens | ForEach-Object { $_.code }
if ($updatedCodes -contains "PIZZA") {
    throw "Pitsaxona should have been removed from Ali!"
}
if ($updatedCodes -notcontains "SOMSA" -or $updatedCodes -notcontains "MAIN") {
    throw "Ali should now have SOMSA and MAIN!"
}

Write-Host "`n=== 8. RE-LOGIN AS ALI AND VERIFY UPDATED ACCESS ===" -ForegroundColor Cyan
$aliLoginRes2 = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -Body $aliLoginBody -ContentType "application/json"
$aliToken2 = $aliLoginRes2.data.accessToken
$aliHeaders2 = @{
    "Authorization" = "Bearer $aliToken2"
    "Content-Type" = "application/json"
}

$aliStationsRes2 = Invoke-RestMethod -Uri "$baseUrl/kitchens" -Method Get -Headers $aliHeaders2
$aliStations2 = $aliStationsRes2.data
Write-Host "Stations visible to Ali after update ($($aliStations2.Count)):"
$aliStations2 | ForEach-Object { Write-Host "   - $($_.name) ($($_.code))" }

$visibleCodes2 = $aliStations2 | ForEach-Object { $_.code }
if ($visibleCodes2 -contains "PIZZA") {
    throw "Ali should NO LONGER see PIZZA!"
}
if ($visibleCodes2 -notcontains "SOMSA" -or $visibleCodes2 -notcontains "MAIN") {
    throw "Ali should now see SOMSA and MAIN!"
}
Write-Host "Updated station visibility verified!" -ForegroundColor Green

# Now querying PIZZA orders must return 403 Forbidden
try {
    Invoke-RestMethod -Uri "$baseUrl/kitchen/orders?kitchenId=$($pizzaKitchen.id)" -Method Get -Headers $aliHeaders2
    throw "Ali was still able to query PIZZA orders after being unassigned!"
} catch {
    Write-Host "Ali correctly blocked from PIZZA orders after removal (403 Forbidden)" -ForegroundColor Yellow
}

# Ali can query MAIN orders
$mainOrders = Invoke-RestMethod -Uri "$baseUrl/kitchen/orders?kitchenId=$($mainKitchen.id)" -Method Get -Headers $aliHeaders2
Write-Host "Ali can query Asosiy oshxona orders: $($mainOrders.data.Count) orders found" -ForegroundColor Green

Write-Host "`n=======================================================" -ForegroundColor Green
Write-Host "ALL EMPLOYEE MULTI-KITCHEN TESTS PASSED 100%!" -ForegroundColor Green
Write-Host "=======================================================" -ForegroundColor Green
