# 1. Login as Admin/Manager to update Pizza Margarita to 85000
$loginAdmin = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body (@{ username = 'admin'; password = 'admin123' } | ConvertTo-Json) -ContentType 'application/json'
$adminToken = $loginAdmin.data.accessToken

$pizzaId = 'a1000000-0000-0000-0000-000000000022'
$updateBody = @{
    name = 'Pizza Margarita'
    categoryId = 'f0000000-0000-0000-0000-000000000013'
    salePrice = 85000
    active = $true
    available = $true
    imageUrl = '/uploads/products/5229e45a-d480-4c46-98c5-d2c6c7f96f79-ban.png'
} | ConvertTo-Json

$updated = Invoke-RestMethod -Uri "http://localhost:8080/api/products/$pizzaId" -Method Put -Body $updateBody -Headers @{ Authorization = "Bearer $adminToken" } -ContentType 'application/json'

Write-Host "Product update result:"
Write-Host "  Name: $($updated.data.name)"
Write-Host "  Price: $($updated.data.salePrice)"
Write-Host "  Category: $($updated.data.categoryName)"
Write-Host "  Kitchen: $($updated.data.kitchenName)"
Write-Host "  Image: $($updated.data.imageUrl)"
Write-Host "  Active: $($updated.data.active)"
Write-Host "  Available: $($updated.data.available)"

# 2. Login as Waiter to verify product appears in Order Screen API
$loginWaiter = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body (@{ username = 'waiter1'; password = 'waiter123' } | ConvertTo-Json) -ContentType 'application/json'
$waiterToken = $loginWaiter.data.accessToken

$posProducts = Invoke-RestMethod -Uri 'http://localhost:8080/api/products?activeOnly=true' -Headers @{ Authorization = "Bearer $waiterToken" }
$foundPizza = $posProducts.data | Where-Object { $_.id -eq $pizzaId }

if ($foundPizza) {
    Write-Host "SUCCESS: Pizza Margarita found in POS order list!"
    Write-Host "  Found Name: $($foundPizza.name)"
    Write-Host "  Found Price: $($foundPizza.salePrice)"
    Write-Host "  Found Image: $($foundPizza.imageUrl)"
    Write-Host "  Found Kitchen: $($foundPizza.kitchenName)"
} else {
    Write-Host "ERROR: Pizza Margarita NOT found in POS order list!"
}

# 3. Verify Burger, Cola, and Lavash are also in the POS order list
$burger = $posProducts.data | Where-Object { $_.name -like '*Burger*' }
$cola = $posProducts.data | Where-Object { $_.name -like '*Cola*' }
$lavash = $posProducts.data | Where-Object { $_.name -like '*Lavash*' }

Write-Host "Other key menu items in POS order list:"
Write-Host "  Burger: $(if ($burger) { $burger.name + ' (' + $burger.salePrice + ' so''m)' } else { 'NOT FOUND' })"
Write-Host "  Cola: $(if ($cola) { ($cola | ForEach-Object { $_.name }) -join ', ' } else { 'NOT FOUND' })"
Write-Host "  Lavash: $(if ($lavash) { $lavash.name + ' (' + $lavash.salePrice + ' so''m)' } else { 'NOT FOUND' })"
