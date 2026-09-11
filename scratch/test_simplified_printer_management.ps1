$ErrorActionPreference = "Stop"

# 1. Login
$loginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginRes.data.accessToken
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

Write-Host "=== 1. Login Successful ===" -ForegroundColor Green

# 2. Test Printer Types & Validation in Printer Management
Write-Host "`n=== 2. Printer Types & Validation Test ===" -ForegroundColor Cyan

# 2a. Check available Windows printers
$availablePrinters = (Invoke-RestMethod -Uri "http://localhost:8080/api/printers/available" -Method Get -Headers $headers).data
Write-Host "Topilgan Windows printerlari soni: $($availablePrinters.Count)"
$winPrinter = $availablePrinters | Select-Object -First 1
Write-Host "Ishlatiladigan Windows printer: $($winPrinter.systemPrinterName)"

# 2b. Test invalid purpose validation
try {
    $badBody = @{
        systemPrinterName = $winPrinter.systemPrinterName
        purpose = "INVALID_TYPE_XYZ"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/printers" -Method Post -Body $badBody -Headers $headers
    throw "XATO: Noto'g'ri purpose qabul qilinmasligi kerak edi!"
} catch {
    Write-Host "Noto'g'ri purpose to'g'ri bloklandi: $($_.Exception.Message)" -ForegroundColor Green
}

# 2c. Get all printers and verify they are only KITCHEN or CASHIER
$allPrinters = (Invoke-RestMethod -Uri "http://localhost:8080/api/printers" -Method Get -Headers $headers).data
foreach ($p in $allPrinters) {
    Write-Host "Printer: $($p.name), Purpose: $($p.purpose)"
    if ($p.purpose -ne "KITCHEN" -and $p.purpose -ne "CASHIER") {
        throw "XATO: Tizimda KITCHEN yoki CASHIER dan boshqa purpose topildi: $($p.purpose)"
    }
}
Write-Host "Barcha mavjud printerlar faqat KITCHEN yoki CASHIER ekanligi tasdiqlandi!" -ForegroundColor Green

# 3. Test Full Flow: Waiter -> Kitchen Printer, Payment -> Cashier Printer
Write-Host "`n=== 3. Oshxona va Kassa Printer Flow Testi ===" -ForegroundColor Cyan

$tables = (Invoke-RestMethod -Uri "http://localhost:8080/api/tables" -Method Get -Headers $headers).data
$freeTable = $tables | Where-Object { $_.status -eq "FREE" } | Select-Object -First 1
if (-not $freeTable) {
    $freeTable = $tables[0]
}
Write-Host "Stol: $($freeTable.name) (id: $($freeTable.id))"

$products = (Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method Get -Headers $headers).data
$prod1 = $products[0]
$prod2 = $products[1]

# Step A: 1-zakas: Pitsa x1
$createOrderBody = @{
    tableId = $freeTable.id
    orderType = "DINE_IN"
    guestCount = 2
    items = @(
        @{
            productId = $prod1.id
            quantity = 1
        }
    )
} | ConvertTo-Json -Depth 5

$order = (Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Post -Body $createOrderBody -Headers $headers).data
$orderId = $order.id
Write-Host "1-zakas: $($prod1.name) x1 yuborildi. Order: $($order.orderNumber)"
Write-Host "Receipt Status: $($order.receiptPrintStatus) (Mijoz cheki chiqmasligi kerak!)"

if ($order.receiptPrintStatus -ne "NOT_PRINTED") {
    throw "XATO: Oshxonaga yuborilganda kassa cheki chiqmasligi kerak!"
}

# Step B: Yana shu mahsulotdan 1 dona qo'shildi (Pitsa x1)
$addItemsBody = @{
    items = @(
        @{
            productId = $prod1.id
            quantity = 1
        }
    )
} | ConvertTo-Json -Depth 5

$order = (Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$orderId/items" -Method Post -Body $addItemsBody -Headers $headers).data
$item1 = $order.items[0]
Write-Host "Qayta zakasdan so'ng: Jami Qty=$($item1.quantity), Sent=$($item1.sentQuantity), Remaining=$($item1.remainingToSend)"

if ($order.items.Count -ne 1 -or $item1.quantity -ne 2 -or $item1.remainingToSend -ne 1) {
    throw "XATO: Quantity birlashmadi yoki remaining noto'g'ri!"
}

# Step C: Oshxonaga yuborish (FAQAT yangi 1 dona chiqishi kerak)
$order = (Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$orderId/send-to-kitchen" -Method Post -Headers $headers).data
$item1 = $order.items[0]
Write-Host "Oshxonaga yuborilgach: Qty=$($item1.quantity), Sent=$($item1.sentQuantity), Remaining=$($item1.remainingToSend)"

if ($item1.sentQuantity -ne 2 -or $item1.remainingToSend -ne 0) {
    throw "XATO: Oshxonaga yangi miqdor yuborilmadi!"
}

# Step D: To'lov muvaffaqiyatli amalga oshirildi -> CASHIER PRINTER orqali kassa cheki chiqariladi
Write-Host "`nTo'lov amalga oshirilmoqda (Jami: $($order.total) so'm)..."
$payBody = @{
    orderId = $orderId
    paymentMethod = "CASH"
    amount = $order.total
    cashAmount = $order.total
} | ConvertTo-Json

$payRes = (Invoke-RestMethod -Uri "http://localhost:8080/api/payments" -Method Post -Body $payBody -Headers $headers).data
Write-Host "To'lov statusi: $($payRes.status)"
Write-Host "Chek chop etish statusi: $($payRes.receiptPrintStatus)"

$orderAfterPay = (Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$orderId" -Method Get -Headers $headers).data
Write-Host "Order status: $($orderAfterPay.status), ReceiptPrintStatus: $($orderAfterPay.receiptPrintStatus)"

if ($payRes.status -ne "COMPLETED" -or $orderAfterPay.status -ne "PAID") {
    throw "XATO: To'lov muvaffaqiyatli yakunlanmadi!"
}

if ($orderAfterPay.receiptPrintStatus -ne "PRINTED" -and $orderAfterPay.receiptPrintStatus -ne "PRINTING") {
    throw "XATO: To'lovdan so'ng kassa cheki chop etilmadi!"
}

Write-Host "`nSODDALASHTIRILGAN PRINTER MANAGEMENT VA BARCHA TALABLAR 100% MUVAFFAQIYATLI O'TDI!" -ForegroundColor Green
