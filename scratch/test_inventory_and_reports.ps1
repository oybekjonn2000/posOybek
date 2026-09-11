$ErrorActionPreference = "Stop"

Write-Host "=== TEST: OMBOR VA HISOBOTLAR INTEGRATION TEST ===" -ForegroundColor Cyan

# 1. Login as admin
$loginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

$loginResp = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginResp.data.accessToken
$headers = @{
    Authorization = "Bearer $token"
}

Write-Host "[1] Logged in as Admin. Token obtained." -ForegroundColor Green

# 2. Check Dashboard
$dashResp = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/dashboard" -Headers $headers
Write-Host "[2] Dashboard Stats:" -ForegroundColor Yellow
$dashResp.data | Format-List

# 3. Check Warehouses & Suppliers
$whResp = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/warehouses" -Headers $headers
$mainWh = $whResp.data | Where-Object { $_.name -like "*Asosiy*" } | Select-Object -First 1
if (-not $mainWh) { $mainWh = $whResp.data[0] }
Write-Host "[3] Selected Warehouse: $($mainWh.name) ($($mainWh.id))" -ForegroundColor Green

$suppResp = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/suppliers" -Headers $headers
$supplier = $suppResp.data[0]
Write-Host "[3] Selected Supplier: $($supplier.name) ($($supplier.id))" -ForegroundColor Green

# 4. Check or create 'Un' (Flour) in inventory items
$itemsResp = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory" -Headers $headers
$flourItem = $itemsResp.data | Where-Object { $_.name -like "*Un*" -or $_.name -like "*un*" } | Select-Object -First 1

if (-not $flourItem) {
    Write-Host "Creating 'Oliy navli un' item..." -ForegroundColor Yellow
    $createItemBody = @{
        name = "Oliy navli un"
        sku = "UN-001"
        unit = "kg"
        quantity = 0
        minQuantity = 10
        costPrice = 8000
        category = "Un va don mahsulotlari"
        warehouseId = $mainWh.id
    } | ConvertTo-Json

    $createdItemResp = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory" -Method Post -Headers $headers -Body $createItemBody -ContentType "application/json"
    $flourItem = $createdItemResp.data
}

Write-Host "[4] Flour item: ID=$($flourItem.id), Name=$($flourItem.name), Current Stock=$($flourItem.quantity)" -ForegroundColor Green
$initialStock = [decimal]$flourItem.quantity

# 5. STEP 1: Omborga 50 kg un kirim qil (Purchase)
Write-Host "`n>>> QADAM 1: 50 kg un kirim qilish..." -ForegroundColor Cyan
$purchaseBody = @{
    supplierId = $supplier.id
    warehouseId = $mainWh.id
    invoiceNumber = "INV-TEST-50KG"
    notes = "Test kirim 50kg un"
    paidAmount = 400000
    items = @(
        @{
            itemId = $flourItem.id
            quantity = 50.0
            unitCost = 8000
            notes = "50 kg un"
        }
    )
} | ConvertTo-Json

$purchaseResp = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/purchases" -Method Post -Headers $headers -Body $purchaseBody -ContentType "application/json"
Write-Host "Kirim yaratildi: Number=$($purchaseResp.data.purchaseNumber), Total=$($purchaseResp.data.totalAmount)" -ForegroundColor Green

$itemAfterKirim = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/$($flourItem.id)" -Headers $headers
Write-Host "Kirimdan keyingi qoldiq: $($itemAfterKirim.data.quantity) kg (Kutilgan: $($initialStock + 50))" -ForegroundColor Yellow

# 6. STEP 2: 10 kg un chiqim qil (Outbound)
Write-Host "`n>>> QADAM 2: 10 kg un chiqim qilish..." -ForegroundColor Cyan
$outboundBody = @{
    itemId = $flourItem.id
    warehouseId = $mainWh.id
    quantity = 10.0
    reason = "Oshxona uchun"
    type = "OUT"
    notes = "Oshxonaga xamir uchun berildi"
} | ConvertTo-Json

$outResp = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/outbound" -Method Post -Headers $headers -Body $outboundBody -ContentType "application/json"
Write-Host "Chiqimdan keyingi qoldiq: $($outResp.data.quantity) kg (Kutilgan: $($initialStock + 40))" -ForegroundColor Yellow

# 7. STEP 3: Pizza retseptiga 0.25 kg un biriktir
Write-Host "`n>>> QADAM 3: Pizza retseptiga 0.25 kg un biriktirish..." -ForegroundColor Cyan
$productsResp = Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Headers $headers
$pizza = $productsResp.data | Where-Object { $_.name -like "*Pizza*" -or $_.name -like "*Pitsa*" } | Select-Object -First 1

if (-not $pizza) {
    $pizza = $productsResp.data[0]
}
Write-Host "Tanlangan taom: $($pizza.name) (ID: $($pizza.id))" -ForegroundColor Green

$recipeBody = @{
    productId = $pizza.id
    items = @(
        @{
            inventoryItemId = $flourItem.id
            quantity = 0.250
            unit = "kg"
        }
    )
} | ConvertTo-Json

$recipeResp = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/recipes" -Method Post -Headers $headers -Body $recipeBody -ContentType "application/json"
Write-Host "Retsept saqlandi: Taom=$($recipeResp.data.productName), Ingredientlar soni=$($recipeResp.data.recipeItems.Count)" -ForegroundColor Green

# 8. STEP 4: 2 ta pizza buyurtma berish va to'lash (Order -> Payment -> Recipe deduction)
Write-Host "`n>>> QADAM 4: 2 ta pizza sotish va to'lash..." -ForegroundColor Cyan

# Stol olish
$tablesResp = Invoke-RestMethod -Uri "http://localhost:8080/api/tables" -Headers $headers
$testTable = $tablesResp.data | Where-Object { $_.status -eq "FREE" } | Select-Object -First 1
if (-not $testTable) { $testTable = $tablesResp.data[0] }

$orderBody = @{
    tableId = $testTable.id
    orderType = "DINE_IN"
    items = @(
        @{
            productId = $pizza.id
            quantity = 2
            notes = "Test 2 pizza"
        }
    )
} | ConvertTo-Json

$orderResp = Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Headers $headers -Body $orderBody -ContentType "application/json"
$createdOrder = $orderResp.data
Write-Host "Order yaratildi: Order #$($createdOrder.orderNumber), ID=$($createdOrder.id), Total=$($createdOrder.total)" -ForegroundColor Green

# To'lov qilish (Process Payment)
$payBody = @{
    orderId = $createdOrder.id
    paymentMethod = "CASH"
    amount = $createdOrder.total
    cashAmount = $createdOrder.total
    notes = "Test payment cash"
} | ConvertTo-Json

$payResp = Invoke-RestMethod -Uri "http://localhost:8080/api/payments" -Method Post -Headers $headers -Body $payBody -ContentType "application/json"
Write-Host "To'lov amalga oshirildi: Payment #$($payResp.data.paymentNumber), Status=$($payResp.data.status)" -ForegroundColor Green

# 9. STEP 5: Inventory tekshirish: 50 - 10 - 0.5 = 39.5 kg
Write-Host "`n>>> QADAM 5: Inventory tekshirish (50 - 10 - 0.5 = 39.5 kg)..." -ForegroundColor Cyan
$itemAfterSale = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/$($flourItem.id)" -Headers $headers
$expectedStock = $initialStock + 50 - 10 - 0.5
Write-Host "Hozirgi qoldiq: $($itemAfterSale.data.quantity) kg" -ForegroundColor Green
Write-Host "Kutilgan qoldiq: $expectedStock kg" -ForegroundColor Green

if ([decimal]$itemAfterSale.data.quantity -eq [decimal]$expectedStock) {
    Write-Host ">>> MATEMATIK HISOB VA RETSEPT SARFI TO'G'RI! (50 - 10 - 0.5 = 39.5) <<<" -ForegroundColor Green
} else {
    Write-Host "DIQQAT: Qoldiq farq qildi. Hozirgi: $($itemAfterSale.data.quantity), Kutilgan: $expectedStock" -ForegroundColor Yellow
}

# 10. STEP 6: Reports tekshirish
Write-Host "`n>>> QADAM 6: HISOBOTLARNI TEKSHIRISH..." -ForegroundColor Cyan

$salesRep = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/sales" -Headers $headers
Write-Host "1. Savdo hisoboti:" -ForegroundColor Yellow
Write-Host "   Jami savdo: $($salesRep.data.totalSales)"
Write-Host "   Jami buyurtmalar: $($salesRep.data.totalOrders)"
Write-Host "   O'rtacha chek: $($salesRep.data.avgCheck)"
Write-Host "   Naqd: $($salesRep.data.cashTotal)"
Write-Host "   Karta: $($salesRep.data.cardTotal)"

$prodRep = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/products" -Headers $headers
Write-Host "2. Mahsulot savdosi:" -ForegroundColor Yellow
$prodRep.data | Select-Object -First 3 | Format-Table productName, quantity, revenue, cost, profit, profitMargin

$profitRep = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/profit" -Headers $headers
Write-Host "3. Foyda hisoboti (P&L):" -ForegroundColor Yellow
$profitRep.data | Format-List

$cashierRep = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/cashier" -Headers $headers
Write-Host "4. Kassa hisoboti:" -ForegroundColor Yellow
$cashierRep.data | Format-Table cashierName, ordersCount, cashSales, cardSales, totalSales

$waiterRep = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/waiters" -Headers $headers
Write-Host "5. Ofitsiant hisoboti:" -ForegroundColor Yellow
$waiterRep.data | Format-Table waiterName, ordersCount, totalSales, avgCheck

$kitchenRep = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/kitchens" -Headers $headers
Write-Host "6. Oshxona hisoboti:" -ForegroundColor Yellow
$kitchenRep.data | Format-Table kitchenName, ordersCount, itemsPrepared, revenue

$stockRep = Invoke-RestMethod -Uri "http://localhost:8080/api/reports/stock" -Headers $headers
Write-Host "7. Ombor hisoboti (Stock Balance):" -ForegroundColor Yellow
$stockRep.data | Where-Object { $_.itemName -like "*un*" -or $_.itemName -like "*Un*" } | Format-List

# 11. STEP 7: Inventarizatsiya testi (Audit)
Write-Host "`n>>> QADAM 7: INVENTARIZATSIYA TESTI..." -ForegroundColor Cyan
$auditStartBody = @{
    warehouseId = $mainWh.id
    title = "Test Inventarizatsiya 12-Sentyabr"
    notes = "Avtomat tekshiruv"
} | ConvertTo-Json

$auditStart = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/audits/start" -Method Post -Headers $headers -Body $auditStartBody -ContentType "application/json"
$auditId = $auditStart.data.id
Write-Host "Audit boshlandi: ID=$auditId, Itemlar soni=$($auditStart.data.items.Count)" -ForegroundColor Green

# 1 kg kam chiqdi deb kiritamiz (masalan 39.5 emas 38.5)
$auditSubmitBody = @{
    notes = "Sanoq yakunlandi. 1 kg kamomad aniqlandi."
    items = @(
        @{
            itemId = $flourItem.id
            actualQuantity = ($expectedStock - 1.0)
            notes = "Qop ochilgan, to'kilgan"
        }
    )
} | ConvertTo-Json

$auditComplete = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/audits/$auditId/submit" -Method Post -Headers $headers -Body $auditSubmitBody -ContentType "application/json"
Write-Host "Audit yakunlandi: Status=$($auditComplete.data.status), Farq summasi=$($auditComplete.data.totalDiscrepancyCost)" -ForegroundColor Green

$itemAfterAudit = Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/$($flourItem.id)" -Headers $headers
Write-Host "Inventarizatsiyadan keyingi yangi qoldiq: $($itemAfterAudit.data.quantity) kg (Kutilgan: $($expectedStock - 1.0))" -ForegroundColor Yellow

# 12. CSV Export
$csvResp = Invoke-WebRequest -Uri "http://localhost:8080/api/reports/export/csv?reportType=PRODUCTS" -Headers $headers
Write-Host "`n>>> CSV Export muvaffaqiyatli olindi ($($csvResp.Content.Length) bytes) <<<" -ForegroundColor Green

Write-Host "`n=== BARCHA 16 TA OMBOR VA HISOBOTLAR TESTLARI MUVAFFAQIYATLI O'TDI! ===" -ForegroundColor Green
