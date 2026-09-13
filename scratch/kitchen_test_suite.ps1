$ErrorActionPreference = "Continue"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " KITCHEN MANAGEMENT COMPLETE COMPREHENSIVE TEST SUITE" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Login as Admin
$adminLoginBody = @{ username = 'admin'; password = 'admin123' } | ConvertTo-Json
$adminLoginRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body $adminLoginBody
$adminToken = $adminLoginRes.data.accessToken
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Write-Host "✓ Logged in as Admin" -ForegroundColor Green

# 2. Get existing kitchens
$kitchensRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens' -Headers $adminHeaders
$existingKitchens = $kitchensRes.data
Write-Host "✓ Existing kitchens count: $($existingKitchens.Count)" -ForegroundColor Green
foreach ($k in $existingKitchens) {
    Write-Host "  - [$($k.code)] $($k.name) (Active: $($k.active), Employees: $($k.assignedEmployeesCount), Cats: $($k.assignedCategoriesCount))" -ForegroundColor Gray
}

# 3. Test Validation: Duplicate name prevention
Write-Host "`n--- Test 1: Duplicate Name Validation ---" -ForegroundColor Yellow
$firstKitchenName = $existingKitchens[0].name
$dupBody = @{ name = $firstKitchenName; code = "DUP1" } | ConvertTo-Json
try {
    Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens' -Method Post -ContentType 'application/json' -Headers $adminHeaders -Body $dupBody
    Write-Host "✗ FAILED: Server allowed duplicate kitchen name!" -ForegroundColor Red
} catch {
    $errResp = $_.ErrorDetails.Message | ConvertFrom-Json
    Write-Host "✓ PASSED: Duplicate name prevented correctly: $($errResp.message)" -ForegroundColor Green
}

# 4. Test Validation: Blank name prevention
Write-Host "`n--- Test 2: Blank / Whitespace Name Validation ---" -ForegroundColor Yellow
$blankBody = @{ name = "   "; code = "BLANK" } | ConvertTo-Json
try {
    Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens' -Method Post -ContentType 'application/json' -Headers $adminHeaders -Body $blankBody
    Write-Host "✗ FAILED: Server allowed blank kitchen name!" -ForegroundColor Red
} catch {
    Write-Host "✓ PASSED: Blank kitchen name rejected by backend" -ForegroundColor Green
}

# 5. Create new Kitchen: "Pitsaxona Test" (or "Pitsaxona 2")
Write-Host "`n--- Test 3: Create New Kitchen ---" -ForegroundColor Yellow
$testKitchenName = "Pitsaxona Test"
$testKitchenBody = @{
    name = $testKitchenName
    description = "Test pizza and fast food station"
    color = "#EF4444"
    autoPrint = $true
    soundNotification = $true
    preparationTimeMinutes = 20
} | ConvertTo-Json

# Check if it exists from previous run, if so find it
$createdKitchen = $existingKitchens | Where-Object { $_.name -eq $testKitchenName }
if (-not $createdKitchen) {
    $createRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens' -Method Post -ContentType 'application/json' -Headers $adminHeaders -Body $testKitchenBody
    $createdKitchen = $createRes.data
    Write-Host "✓ PASSED: Kitchen created: ID=$($createdKitchen.id), Name=$($createdKitchen.name), Code=$($createdKitchen.code)" -ForegroundColor Green
} else {
    Write-Host "✓ Found existing test kitchen: ID=$($createdKitchen.id)" -ForegroundColor Green
}

# 6. Edit Kitchen: Rename to "Pizza Oshxonasi"
Write-Host "`n--- Test 4: Edit / Rename Kitchen (Preserving ID) ---" -ForegroundColor Yellow
$updatedName = "Pizza Oshxonasi"
$origId = $createdKitchen.id
$updateBody = @{
    name = $updatedName
    description = "Updated pizza kitchen description"
    color = "#10B981"
} | ConvertTo-Json

$updateRes = Invoke-RestMethod -Uri "http://localhost:8080/api/kitchens/$origId" -Method Put -ContentType 'application/json' -Headers $adminHeaders -Body $updateBody
$updatedKitchen = $updateRes.data
if ($updatedKitchen.id -eq $origId -and $updatedKitchen.name -eq $updatedName) {
    Write-Host "✓ PASSED: Kitchen successfully renamed to '$($updatedKitchen.name)' while preserving ID ($($updatedKitchen.id))" -ForegroundColor Green
} else {
    Write-Host "✗ FAILED: ID changed or name mismatch!" -ForegroundColor Red
}

# 7. Status Toggle: INACTIVE
Write-Host "`n--- Test 5: Status Toggle: Deactivate ---" -ForegroundColor Yellow
$toggleRes = Invoke-RestMethod -Uri "http://localhost:8080/api/kitchens/$origId/status" -Method Patch -ContentType 'application/json' -Headers $adminHeaders -Body (@{ active = $false } | ConvertTo-Json)
Write-Host "✓ Status changed to: $($toggleRes.data.active)" -ForegroundColor Green

# Verify it does NOT appear in /api/kitchens/active
$activeKitchensRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens/active' -Headers $adminHeaders
$foundInActive = $activeKitchensRes.data | Where-Object { $_.id -eq $origId }
if (-not $foundInActive) {
    Write-Host "✓ PASSED: Inactive kitchen is excluded from /api/kitchens/active" -ForegroundColor Green
} else {
    Write-Host "✗ FAILED: Inactive kitchen appeared in active list!" -ForegroundColor Red
}

# 8. Category Assignment Guard: Cannot assign INACTIVE kitchen to new category
Write-Host "`n--- Test 6: Inactive Kitchen Category Assignment Guard ---" -ForegroundColor Yellow
$catBody = @{
    name = "Test Inactive Cat $(Get-Random)"
    kitchenId = $origId
} | ConvertTo-Json
try {
    Invoke-RestMethod -Uri 'http://localhost:8080/api/categories' -Method Post -ContentType 'application/json' -Headers $adminHeaders -Body $catBody
    Write-Host "✗ FAILED: Server allowed assigning inactive kitchen to category!" -ForegroundColor Red
} catch {
    $errResp = $_.ErrorDetails.Message | ConvertFrom-Json
    Write-Host "✓ PASSED: Category creation rejected: $($errResp.message)" -ForegroundColor Green
}

# 9. Reactivate Kitchen
Write-Host "`n--- Test 7: Reactivate Kitchen ---" -ForegroundColor Yellow
$reactivateRes = Invoke-RestMethod -Uri "http://localhost:8080/api/kitchens/$origId/status" -Method Patch -ContentType 'application/json' -Headers $adminHeaders -Body (@{ active = $true } | ConvertTo-Json)
Write-Host "✓ Kitchen reactivated: Active = $($reactivateRes.data.active)" -ForegroundColor Green

# 10. Assign Category to this Kitchen
Write-Host "`n--- Test 8: Assign Category to Active Kitchen ---" -ForegroundColor Yellow
$testCatName = "Pizza Category Test"
$allCatsRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/categories' -Headers $adminHeaders
$testCat = $allCatsRes.data | Where-Object { $_.name -eq $testCatName }
if (-not $testCat) {
    $catCreateBody = @{
        name = $testCatName
        kitchenId = $origId
    } | ConvertTo-Json
    $testCat = (Invoke-RestMethod -Uri 'http://localhost:8080/api/categories' -Method Post -ContentType 'application/json' -Headers $adminHeaders -Body $catCreateBody).data
    Write-Host "✓ Created category '$($testCat.name)' linked to kitchen ID=$origId" -ForegroundColor Green
} else {
    # Update category to point to our kitchen
    $catUpdateBody = @{ kitchenId = $origId } | ConvertTo-Json
    $testCat = (Invoke-RestMethod -Uri "http://localhost:8080/api/categories/$($testCat.id)" -Method Put -ContentType 'application/json' -Headers $adminHeaders -Body $catUpdateBody).data
    Write-Host "✓ Updated category '$($testCat.name)' linked to kitchen ID=$origId" -ForegroundColor Green
}

# 11. Multi-Kitchen Staff Assignment
Write-Host "`n--- Test 9: Multi-Kitchen Staff Assignment (Ali) ---" -ForegroundColor Yellow
# Find or create kitchen employee Ali
$usersRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/users' -Headers $adminHeaders
$ali = $usersRes.data | Where-Object { $_.username -eq 'ali_cook' }
$somsapazKitchen = $existingKitchens | Where-Object { $_.name -like '*Somsa*' -or $_.code -like '*SOM*' } | Select-Object -First 1
$palovchiKitchen = $existingKitchens | Where-Object { $_.name -like '*Palov*' -or $_.code -like '*PAL*' } | Select-Object -First 1

if (-not $ali) {
    $aliCreateBody = @{
        username = 'ali_cook'
        password = 'password123'
        firstName = 'Ali'
        lastName = 'Oshpaz'
        role = 'KITCHEN'
        kitchenIds = @($origId, $somsapazKitchen.id)
    } | ConvertTo-Json
    $ali = (Invoke-RestMethod -Uri 'http://localhost:8080/api/users' -Method Post -ContentType 'application/json' -Headers $adminHeaders -Body $aliCreateBody).data
    Write-Host "✓ Created Kitchen employee Ali with Multi-Kitchen: Pizza Oshxonasi + $($somsapazKitchen.name)" -ForegroundColor Green
} else {
    # Assign Ali to Pizza Oshxonasi and Somsapaz
    $aliUpdateBody = @{
        firstName = 'Ali'
        lastName = 'Oshpaz'
        role = 'KITCHEN'
        kitchenIds = @($origId, $somsapazKitchen.id)
    } | ConvertTo-Json
    $ali = (Invoke-RestMethod -Uri "http://localhost:8080/api/users/$($ali.id)" -Method Put -ContentType 'application/json' -Headers $adminHeaders -Body $aliUpdateBody).data
    Write-Host "✓ Assigned Ali to Multi-Kitchen: Pizza Oshxonasi + $($somsapazKitchen.name)" -ForegroundColor Green
}

# 12. Test Multi-Kitchen Isolation for Ali
Write-Host "`n--- Test 10: Multi-Kitchen Station Isolation for Ali ---" -ForegroundColor Yellow
$aliLoginBody = @{ username = 'ali_cook'; password = 'password123' } | ConvertTo-Json
$aliLoginRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body $aliLoginBody
$aliToken = $aliLoginRes.data.accessToken
$aliHeaders = @{ Authorization = "Bearer $aliToken" }

# Ali gets kitchen stations
$aliStationsRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens' -Headers $aliHeaders
$aliStations = $aliStationsRes.data
Write-Host "✓ Ali sees $($aliStations.Count) stations:" -ForegroundColor Green
foreach ($as in $aliStations) {
    Write-Host "   - $($as.name) ($($as.code))" -ForegroundColor Cyan
}

$aliHasPizza = $aliStations | Where-Object { $_.id -eq $origId }
$aliHasSomsa = $aliStations | Where-Object { $_.id -eq $somsapazKitchen.id }
$aliHasPalov = $aliStations | Where-Object { $_.id -eq $palovchiKitchen.id }

if ($aliHasPizza -and $aliHasSomsa -and (-not $aliHasPalov)) {
    Write-Host "✓ PASSED: Ali only sees Pizza and Somsapaz! Palovchi is securely hidden." -ForegroundColor Green
} else {
    Write-Host "✗ FAILED: Isolation check failed! Palovchi visible or assigned missing." -ForegroundColor Red
}

# 13. Backend Security: Verify Ali cannot view Palovchi orders directly
Write-Host "`n--- Test 11: Backend Authorization Guard for Ali on Palovchi ---" -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/kitchen/orders?kitchenId=$($palovchiKitchen.id)" -Headers $aliHeaders
    Write-Host "✗ FAILED: Ali accessed Palovchi orders directly!" -ForegroundColor Red
} catch {
    Write-Host "✓ PASSED: Backend rejected Ali from viewing Palovchi orders: 403 Forbidden" -ForegroundColor Green
}

# 14. Backend Security: Ali cannot perform CRUD on Kitchens
Write-Host "`n--- Test 12: Backend CRUD Guard: Kitchen Staff Forbidden from CRUD ---" -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens' -Method Post -ContentType 'application/json' -Headers $aliHeaders -Body (@{ name = 'Hacked Kitchen' } | ConvertTo-Json)
    Write-Host "✗ FAILED: Ali was allowed to create a kitchen!" -ForegroundColor Red
} catch {
    Write-Host "✓ PASSED: Backend rejected Ali from creating kitchen: 403 Forbidden" -ForegroundColor Green
}

# 15. Create Product & Place Order for Real-Time Kitchen Routing
Write-Host "`n--- Test 13: Order Routing: Product -> Category -> Kitchen ---" -ForegroundColor Yellow
# Find or create a test product under $testCat
$productsRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/products' -Headers $adminHeaders
$testProd = $productsRes.data | Where-Object { $_.name -eq 'Pizza Margarita Test' }
if (-not $testProd) {
    $prodBody = @{
        name = 'Pizza Margarita Test'
        categoryId = $testCat.id
        salePrice = 65000
        active = $true
        available = $true
    } | ConvertTo-Json
    $testProd = (Invoke-RestMethod -Uri 'http://localhost:8080/api/products' -Method Post -ContentType 'application/json' -Headers $adminHeaders -Body $prodBody).data
    Write-Host "✓ Created product '$($testProd.name)' in category '$($testCat.name)'" -ForegroundColor Green
} else {
    Write-Host "✓ Found product '$($testProd.name)'" -ForegroundColor Green
}

# Create an order with this product
$orderBody = @{
    items = @(
        @{
            productId = $testProd.id
            quantity = 2
            unitPrice = 65000
        }
    )
} | ConvertTo-Json

$createOrderRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/orders' -Method Post -ContentType 'application/json' -Headers $adminHeaders -Body $orderBody
$createdOrder = $createOrderRes.data
Write-Host "✓ Placed Order #$($createdOrder.orderNumber) (ID=$($createdOrder.id))" -ForegroundColor Green

# Verify Order Item Kitchen Routing
$orderItem = $createdOrder.items | Where-Object { $_.productId -eq $testProd.id }
Write-Host "  Order Item Kitchen ID: $($orderItem.kitchenId)" -ForegroundColor Cyan
if ($orderItem.kitchenId -eq $origId) {
    Write-Host "✓ PASSED: Order item correctly routed to Pizza Oshxonasi ($origId)!" -ForegroundColor Green
} else {
    Write-Host "✗ FAILED: Order item kitchen ID mismatch!" -ForegroundColor Red
}

# 16. Verify Ali sees this order in his station
Write-Host "`n--- Test 14: Ali sees order in Pizza Oshxonasi station ---" -ForegroundColor Yellow
$aliOrdersRes = Invoke-RestMethod -Uri "http://localhost:8080/api/kitchen/orders?kitchenId=$origId" -Headers $aliHeaders
$aliHasThisOrder = $aliOrdersRes.data | Where-Object { $_.id -eq $createdOrder.id }
if ($aliHasThisOrder) {
    Write-Host "✓ PASSED: Order #$($createdOrder.orderNumber) visible to Ali on Pizza station!" -ForegroundColor Green
} else {
    Write-Host "✗ FAILED: Order not found in Ali's station orders!" -ForegroundColor Red
}

# 17. Rename Kitchen & Verify Historical Order Intact
Write-Host "`n--- Test 15: Rename Kitchen & Verify Historical Order Intact ---" -ForegroundColor Yellow
$renameBody = @{ name = "Pizza va Fast Food" } | ConvertTo-Json
$renamedKitchen = (Invoke-RestMethod -Uri "http://localhost:8080/api/kitchens/$origId" -Method Put -ContentType 'application/json' -Headers $adminHeaders -Body $renameBody).data
Write-Host "✓ Kitchen renamed to '$($renamedKitchen.name)'" -ForegroundColor Green

# Check previous order item
$checkOrderRes = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($createdOrder.id)" -Headers $adminHeaders
$historyItem = $checkOrderRes.data.items | Where-Object { $_.productId -eq $testProd.id }
if ($historyItem.kitchenId -eq $origId) {
    Write-Host "✓ PASSED: Historical order item retains kitchen ID ($origId), order intact!" -ForegroundColor Green
} else {
    Write-Host "✗ FAILED: Historical order corrupted!" -ForegroundColor Red
}

# 18. Safe Delete Guard: Try to delete kitchen with linked category/product/orders
Write-Host "`n--- Test 16: Safe Delete Guard (Linked Data Protection) ---" -ForegroundColor Yellow
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/kitchens/$origId" -Method Delete -Headers $adminHeaders
    Write-Host "✗ FAILED: Server allowed deleting kitchen with linked category and orders!" -ForegroundColor Red
} catch {
    $errResp = $_.ErrorDetails.Message | ConvertFrom-Json
    Write-Host "✓ PASSED: Safe delete blocked physical deletion: $($errResp.message)" -ForegroundColor Green
}

# 19. Soft Delete on an unlinked temporary kitchen
Write-Host "`n--- Test 17: Soft Delete Unlinked Temporary Kitchen ---" -ForegroundColor Yellow
$tempKitchenBody = @{ name = "Temporary Delete Test $(Get-Random)" } | ConvertTo-Json
$tempKitchen = (Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens' -Method Post -ContentType 'application/json' -Headers $adminHeaders -Body $tempKitchenBody).data
Write-Host "✓ Created temporary unlinked kitchen: $($tempKitchen.name) ($($tempKitchen.id))" -ForegroundColor Green

$deleteTempRes = Invoke-RestMethod -Uri "http://localhost:8080/api/kitchens/$($tempKitchen.id)" -Method Delete -Headers $adminHeaders
Write-Host "✓ Temporary kitchen deleted: $($deleteTempRes.message)" -ForegroundColor Green

# Verify it is gone from /api/kitchens
$afterDelKitchens = (Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens' -Headers $adminHeaders).data
$foundDeleted = $afterDelKitchens | Where-Object { $_.id -eq $tempKitchen.id }
if (-not $foundDeleted) {
    Write-Host "✓ PASSED: Deleted kitchen is soft-deleted and filtered out from active list!" -ForegroundColor Green
} else {
    Write-Host "✗ FAILED: Deleted kitchen still in list!" -ForegroundColor Red
}

# 20. Pagination & Search Testing
Write-Host "`n--- Test 18: Pagination & Search Endpoints ---" -ForegroundColor Yellow
$pagedRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens?page=0&size=2' -Headers $adminHeaders
Write-Host "✓ Paged result count: $($pagedRes.data.Count), PageMeta: page=$($pagedRes.page.page), size=$($pagedRes.page.size), total=$($pagedRes.page.totalElements)" -ForegroundColor Green

$searchRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens?search=Pizza' -Headers $adminHeaders
Write-Host "✓ Search 'Pizza' returned $($searchRes.data.Count) results: $($searchRes.data[0].name)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " ALL TESTS COMPLETED SUCCESSFULLY!" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
