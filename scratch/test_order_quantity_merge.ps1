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

# 2. Get Tables and Products
$tables = (Invoke-RestMethod -Uri "http://localhost:8080/api/tables" -Method Get -Headers $headers).data
$freeTable = $tables | Where-Object { $_.status -eq "FREE" } | Select-Object -First 1

if (-not $freeTable) {
    $freeTable = $tables[0]
}
Write-Host "Using table: $($freeTable.name) (id: $($freeTable.id))"

$products = (Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method Get -Headers $headers).data
$prod1 = $products[0]
$prod2 = $products[1]

Write-Host "Product 1: $($prod1.name) (id: $($prod1.id), salePrice: $($prod1.salePrice))"
Write-Host "Product 2: $($prod2.name) (id: $($prod2.id), salePrice: $($prod2.salePrice))"

# -------------------------------------------------------------------------------------------------
# 1-BOSQICH
# Ofitsiant Pitsa x1 qo'shdi. "Oshxonaga" bosildi (yoki stolga yangi order ochildi). Oshxona: Pitsa x1 qabul qildi.
# Database: orderedQuantity = 1, sentToKitchenQuantity = 1, remainingToSend = 0
# -------------------------------------------------------------------------------------------------
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
Write-Host "`n=== 1-BOSQICH: Buyurtma yaratildi va Oshxonaga yuborildi ($($order.orderNumber)) ===" -ForegroundColor Cyan
Write-Host "Order items soni: $($order.items.Count)"
$item1 = $order.items[0]
Write-Host "Item 1: $($item1.productName) x$($item1.quantity), Sent: $($item1.sentQuantity), Remaining: $($item1.remainingToSend), Status: $($item1.kitchenStatus)"

if ($order.items.Count -ne 1 -or $item1.quantity -ne 1 -or $item1.sentQuantity -ne 1 -or $item1.remainingToSend -ne 0 -or $item1.kitchenStatus -ne "SENT_TO_KITCHEN") {
    throw "1-bosqich xato: Kutilgan Qty: 1, Sent: 1, Remaining: 0, Status: SENT_TO_KITCHEN!"
}

# -------------------------------------------------------------------------------------------------
# 2-BOSQICH
# Ofitsiant stolga yana Pitsa x1 qo'shdi.
# Ekranda/Cartda: Pitsa x2 bo'lishi kerak (yangi qator emas, mavjud qator oshadi!).
# Backendda: totalQuantity = 2, sentQuantity = 1, remainingToSend = 1, kitchenStatus = PARTIALLY_SENT.
# -------------------------------------------------------------------------------------------------
Write-Host "`n=== 2-BOSQICH: Ofitsiant yana shu mahsulotdan qo'shdi (Pitsa x1) ===" -ForegroundColor Cyan
$addItemsBody = @{
    items = @(
        @{
            productId = $prod1.id
            quantity = 1
        }
    )
} | ConvertTo-Json -Depth 5

$order = (Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$orderId/items" -Method Post -Body $addItemsBody -Headers $headers).data
Write-Host "Items soni (DUPLICATE EMAS, BIRLASHGAN): $($order.items.Count)"
$item1 = $order.items[0]
Write-Host "Item 1: $($item1.productName) x$($item1.quantity), Sent: $($item1.sentQuantity), Remaining: $($item1.remainingToSend), Status: $($item1.kitchenStatus)"

if ($order.items.Count -ne 1) {
    throw "2-bosqich xato: Dublikat qator yaratildi! Kutilgan 1, lekin $($order.items.Count)"
}
if ($item1.quantity -ne 2 -or $item1.sentQuantity -ne 1 -or $item1.remainingToSend -ne 1 -or $item1.kitchenStatus -ne "PARTIALLY_SENT") {
    throw "2-bosqich xato: Kutilgan Qty: 2, Sent: 1, Remaining: 1, Status: PARTIALLY_SENT!"
}

# "Oshxonaga" tugmasi bosildi -> Oshxonaga FAQAT yangi 1 dona yuboriladi!
Write-Host "`n=== 2-BOSQICH: 'Oshxonaga' tugmasi bosildi (FAQAT yangi 1 dona yuboriladi) ===" -ForegroundColor Cyan
$order = (Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$orderId/send-to-kitchen" -Method Post -Headers $headers).data
$item1 = $order.items[0]
Write-Host "Item 1 yuborilgandan so'ng: $($item1.productName) x$($item1.quantity), Sent: $($item1.sentQuantity), Remaining: $($item1.remainingToSend), Status: $($item1.kitchenStatus)"

if ($item1.quantity -ne 2 -or $item1.sentQuantity -ne 2 -or $item1.remainingToSend -ne 0 -or $item1.kitchenStatus -ne "SENT_TO_KITCHEN") {
    throw "2-bosqich send-to-kitchen xato: Kutilgan Qty: 2, Sent: 2, Remaining: 0, Status: SENT_TO_KITCHEN!"
}

# -------------------------------------------------------------------------------------------------
# 3-BOSQICH
# Yana Pitsa x2 qo'shildi.
# Cart: Pitsa x4 bo'lishi kerak. Sent: 2, Remaining: 2.
# -------------------------------------------------------------------------------------------------
Write-Host "`n=== 3-BOSQICH: Yana shu pitsadan x2 dona qo'shildi ===" -ForegroundColor Cyan
$addMoreBody = @{
    items = @(
        @{
            productId = $prod1.id
            quantity = 2
        }
    )
} | ConvertTo-Json -Depth 5

$order = (Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$orderId/items" -Method Post -Body $addMoreBody -Headers $headers).data
Write-Host "Items soni: $($order.items.Count)"
$item1 = $order.items[0]
Write-Host "Item 1: $($item1.productName) x$($item1.quantity), Sent: $($item1.sentQuantity), Remaining: $($item1.remainingToSend), Status: $($item1.kitchenStatus)"

if ($order.items.Count -ne 1) {
    throw "3-bosqich xato: Qatorlar soni 1 bo'lishi kerak!"
}
if ($item1.quantity -ne 4 -or $item1.sentQuantity -ne 2 -or $item1.remainingToSend -ne 2 -or $item1.kitchenStatus -ne "PARTIALLY_SENT") {
    throw "3-bosqich xato: Kutilgan Qty: 4, Sent: 2, Remaining: 2, Status: PARTIALLY_SENT!"
}

# -------------------------------------------------------------------------------------------------
# 4-BOSQICH: Boshqa mahsulot qo'shilsa (Product 2 x 1), u ALOHIDA qator bo'lib qo'shiladi
# -------------------------------------------------------------------------------------------------
Write-Host "`n=== 4-BOSQICH: Boshqa mahsulot qo'shildi (Product 2 x 1) ===" -ForegroundColor Cyan
$addDiffBody = @{
    items = @(
        @{
            productId = $prod2.id
            quantity = 1
        }
    )
} | ConvertTo-Json -Depth 5

$order = (Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$orderId/items" -Method Post -Body $addDiffBody -Headers $headers).data
Write-Host "Items soni (har xil mahsulotlar): $($order.items.Count)"

if ($order.items.Count -ne 2) {
    throw "4-bosqich xato: 2 xil mahsulot uchun 2 ta qator bo'lishi kerak!"
}
$it1 = $order.items | Where-Object { $_.productId -eq $prod1.id }
$it2 = $order.items | Where-Object { $_.productId -eq $prod2.id }

Write-Host "Item 1 ($($it1.productName)): Qty=$($it1.quantity), Sent=$($it1.sentQuantity), Remaining=$($it1.remainingToSend), Status=$($it1.kitchenStatus)"
Write-Host "Item 2 ($($it2.productName)): Qty=$($it2.quantity), Sent=$($it2.sentQuantity), Remaining=$($it2.remainingToSend), Status=$($it2.kitchenStatus)"

if ($it1.quantity -ne 4 -or $it1.remainingToSend -ne 2 -or $it2.quantity -ne 1 -or $it2.remainingToSend -ne 1) {
    throw "4-bosqich xato: Mahsulot miqdorlari noto'g'ri!"
}

# -------------------------------------------------------------------------------------------------
# 5-BOSQICH: "Oshxonaga" tugmasi bosildi -> faqat yangi qo'shilganlar (Pitsa x2, Mahsulot2 x1) yuboriladi
# -------------------------------------------------------------------------------------------------
Write-Host "`n=== 5-BOSQICH: Yangi qo'shilganlarni oshxonaga yuborish ===" -ForegroundColor Cyan
$order = (Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$orderId/send-to-kitchen" -Method Post -Headers $headers).data
$it1 = $order.items | Where-Object { $_.productId -eq $prod1.id }
$it2 = $order.items | Where-Object { $_.productId -eq $prod2.id }

Write-Host "Item 1 yuborilgach: Qty=$($it1.quantity), Sent=$($it1.sentQuantity), Remaining=$($it1.remainingToSend), Status=$($it1.kitchenStatus)"
Write-Host "Item 2 yuborilgach: Qty=$($it2.quantity), Sent=$($it2.sentQuantity), Remaining=$($it2.remainingToSend), Status=$($it2.kitchenStatus)"

if ($it1.sentQuantity -ne 4 -or $it1.remainingToSend -ne 0 -or $it1.kitchenStatus -ne "SENT_TO_KITCHEN") {
    throw "5-bosqich xato: Item 1 uchun kutilgan Sent: 4, Remaining: 0, SENT_TO_KITCHEN!"
}
if ($it2.sentQuantity -ne 1 -or $it2.remainingToSend -ne 0 -or $it2.kitchenStatus -ne "SENT_TO_KITCHEN") {
    throw "5-bosqich xato: Item 2 uchun kutilgan Sent: 1, Remaining: 0, SENT_TO_KITCHEN!"
}

# -------------------------------------------------------------------------------------------------
# 6-BOSQICH: Umumiy to'lov hisobi (Payment Subtotal = totalQuantity * price)
# -------------------------------------------------------------------------------------------------
Write-Host "`n=== 6-BOSQICH: To'lov summasini tekshirish ===" -ForegroundColor Cyan
$expectedSubtotal = (4 * $prod1.salePrice) + (1 * $prod2.salePrice)
Write-Host "Kutilgan Subtotal: $expectedSubtotal"
Write-Host "Buyurtma Subtotal: $($order.subtotal)"
Write-Host "Buyurtma Total: $($order.total)"

if ([Math]::Abs($order.subtotal - $expectedSubtotal) -gt 0.01) {
    throw "6-bosqich xato: Subtotal mos kelmadi! Kutilgan $expectedSubtotal, olingan $($order.subtotal)"
}

# -------------------------------------------------------------------------------------------------
# 7-BOSQICH: To'lovni amalga oshirish va stol holatini FREE qilish
# -------------------------------------------------------------------------------------------------
Write-Host "`n=== 7-BOSQICH: To'lovni tasdiqlash ===" -ForegroundColor Cyan
$payBody = @{
    orderId = $orderId
    paymentMethod = "CASH"
    amount = $order.total
    cashAmount = $order.total
} | ConvertTo-Json

$payRes = (Invoke-RestMethod -Uri "http://localhost:8080/api/payments" -Method Post -Body $payBody -Headers $headers).data
Write-Host "Payment status: $($payRes.status), Amount: $($payRes.amount)"

$orderCheck = (Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$orderId" -Method Get -Headers $headers).data
Write-Host "Order status to'lovdan so'ng: $($orderCheck.status)"

$tableCheck = (Invoke-RestMethod -Uri "http://localhost:8080/api/tables/$($freeTable.id)" -Method Get -Headers $headers).data
Write-Host "Stol holati to'lovdan so'ng: $($tableCheck.status)"

if ($orderCheck.status -ne "PAID") {
    throw "Buyurtma statusi PAID bo'lmadi!"
}
if ($tableCheck.status -ne "FREE") {
    throw "Stol to'lovdan so'ng FREE holatga o'tmadi!"
}

Write-Host "`nBARCHA ORDER QUANTITY VA BIRLASHTIRISH TESTLARI 100% MUVAFFAQIYATLI O'TDI!" -ForegroundColor Green
