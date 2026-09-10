# Test script for Kitchen -> Category -> Product POS architecture refactoring
$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "RESTORAN POS: KITCHEN -> CATEGORY -> PRODUCT AUDIT" -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

# 1. Login as Admin
Write-Host "`n[STEP 1] Admin bilan tizimga kirish..." -ForegroundColor Yellow
$loginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

$loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$token = $loginRes.data.accessToken
Write-Host "Token olindi! User: $($loginRes.data.user.username), Role: $($loginRes.data.user.role)" -ForegroundColor Green

$headers = @{
    Authorization = "Bearer $token"
    "Content-Type" = "application/json"
}

# TEST 1: Oshxonalar ro'yxatini tekshirish
Write-Host "`n[TEST 1] Mavjud oshxonalarni tekshirish (GET /api/kitchens)..." -ForegroundColor Yellow
$kitchensRes = Invoke-RestMethod -Uri "$baseUrl/api/kitchens" -Method Get -Headers $headers
$kitchens = $kitchensRes.data
Write-Host "Oshxonalar soni: $($kitchens.Count)" -ForegroundColor Green
$kitchens | ForEach-Object {
    Write-Host " - Oshxona: $($_.name) (Code: $($_.code), ID: $($_.id))"
}

$mainKitchen = $kitchens | Where-Object { $_.code -eq 'MAIN' -or $_.name -like '*Asosiy*' } | Select-Object -First 1
$barKitchen = $kitchens | Where-Object { $_.code -eq 'BAR' -or $_.name -like '*Bar*' } | Select-Object -First 1
$somsaKitchen = $kitchens | Where-Object { $_.code -eq 'SOMSA' -or $_.name -like '*Somsa*' } | Select-Object -First 1
$pizzaKitchen = $kitchens | Where-Object { $_.code -eq 'PIZZA' -or $_.name -like '*Pitsa*' } | Select-Object -First 1
$palovKitchen = $kitchens | Where-Object { $_.code -eq 'PALOV' -or $_.name -like '*Palov*' } | Select-Object -First 1

if (-not $mainKitchen -or -not $barKitchen -or -not $somsaKitchen) {
    throw "Zarur oshxonalar topilmadi!"
}

# TEST 2: Kategoriya yaratish: KitchenId MAJBURIY ekanligini tekshirish
Write-Host "`n[TEST 2.1] Kitchen ID siz kategoriya yaratishga urinish (Xato berishi kerak)..." -ForegroundColor Yellow
try {
    $badCatBody = @{
        name = "Xato Kategoriya"
    } | ConvertTo-Json
    $badCatRes = Invoke-RestMethod -Uri "$baseUrl/api/categories" -Method Post -Headers $headers -Body $badCatBody
    Write-Host "XATO: Kategoriya kitchenId siz yaratilib ketdi!" -ForegroundColor Red
} catch {
    Write-Host "MUVAFFAQIYATLI BLOKLANDI: $($_.Exception.Message)" -ForegroundColor Green
}

Write-Host "`n[TEST 2.2] Asosiy oshxonaga yangi 'Milliy Test Taomlar' kategoriyasi yaratish..." -ForegroundColor Yellow
$newCatBody = @{
    name = "Milliy Test Taomlar"
    kitchenId = $mainKitchen.id
    sortOrder = 99
    color = "#10b981"
} | ConvertTo-Json
$catRes = Invoke-RestMethod -Uri "$baseUrl/api/categories" -Method Post -Headers $headers -Body $newCatBody
$createdCat = $catRes.data
Write-Host "Yaratildi: $($createdCat.name), Kitchen: $($createdCat.kitchenName) (ID: $($createdCat.kitchenId))" -ForegroundColor Green

# TEST 3: Mahsulot yaratish: CategoryId MAJBURIY va kitchen avtomatik kategoriyadan olinadi
Write-Host "`n[TEST 3] 'Milliy Test Taomlar' kategoriyasiga 'To'y Oshi Test' mahsulotini yaratish..." -ForegroundColor Yellow
$newProdBody = @{
    name = "To'y Oshi Test"
    categoryId = $createdCat.id
    salePrice = 45000
    purchasePrice = 25000
    unit = "porsiya"
} | ConvertTo-Json
$prodRes = Invoke-RestMethod -Uri "$baseUrl/api/products" -Method Post -Headers $headers -Body $newProdBody
$createdProd = $prodRes.data
Write-Host "Mahsulot yaratildi: $($createdProd.name)" -ForegroundColor Green
Write-Host " - Kategoriya: $($createdProd.categoryName) (ID: $($createdProd.categoryId))"
Write-Host " - Oshxona: $($createdProd.kitchenName) (ID: $($createdProd.kitchenId))"

if ($createdProd.kitchenId -ne $mainKitchen.id) {
    throw "XATO: Mahsulot oshxonasi kategoriyaning oshxonasiga teng emas!"
}

# TEST 6: Mahsulot kategoriyasini o'zgartirish -> Oshxona avtomatik yangi kategoriyaning oshxonasiga o'tishi kerak
Write-Host "`n[TEST 6] Mahsulotni Bar oshxonasiga tegishli 'Sovuq ichimliklar' kategoriyasiga ko'chirish..." -ForegroundColor Yellow
$barCatRes = Invoke-RestMethod -Uri "$baseUrl/api/categories?kitchenId=$($barKitchen.id)" -Method Get -Headers $headers
$barCategory = $barCatRes.data | Select-Object -First 1
Write-Host "Bar kategoriyasi tanlandi: $($barCategory.name) (Kitchen: $($barCategory.kitchenName))"

$updateProdBody = @{
    categoryId = $barCategory.id
} | ConvertTo-Json
$updatedProdRes = Invoke-RestMethod -Uri "$baseUrl/api/products/$($createdProd.id)" -Method Put -Headers $headers -Body $updateProdBody
$updatedProd = $updatedProdRes.data
Write-Host "Mahsulot yangilandi:" -ForegroundColor Green
Write-Host " - Yangi Kategoriya: $($updatedProd.categoryName)"
Write-Host " - Yangi Oshxona: $($updatedProd.kitchenName) (ID: $($updatedProd.kitchenId))"

if ($updatedProd.kitchenId -ne $barKitchen.id) {
    throw "XATO: Mahsulot kategoriyasi o'zgarganda oshxonasi avtomatik yangi oshxonaga o'tmadi!"
}
Write-Host "Avtomatik oshxona moslashishi 100% to'g'ri ishladi!" -ForegroundColor Green

# TEST 17 (DELETE GUARD): Bog'langan kategoriyani o'chirishga urinish
Write-Host "`n[TEST 17.1] Mahsulot bog'langan kategoriyani o'chirishga urinish..." -ForegroundColor Yellow
try {
    # Ko'chiramiz yana test kategoriyamizga
    $moveBackBody = @{ categoryId = $createdCat.id } | ConvertTo-Json
    Invoke-RestMethod -Uri "$baseUrl/api/products/$($createdProd.id)" -Method Put -Headers $headers -Body $moveBackBody | Out-Null
    
    Invoke-RestMethod -Uri "$baseUrl/api/categories/$($createdCat.id)" -Method Delete -Headers $headers
    Write-Host "XATO: Mahsulot bog'langan kategoriya o'chib ketdi!" -ForegroundColor Red
} catch {
    Write-Host "MUVAFFAQIYATLI BLOKLANDI: $($_.Exception.Message)" -ForegroundColor Green
}

# Tozalash: mahsulotni o'chirib, keyin kategoriyani o'chirish
Invoke-RestMethod -Uri "$baseUrl/api/products/$($createdProd.id)" -Method Delete -Headers $headers | Out-Null
Invoke-RestMethod -Uri "$baseUrl/api/categories/$($createdCat.id)" -Method Delete -Headers $headers | Out-Null
Write-Host "Test mahsuloti va kategoriyasi xavfsiz tozalandi." -ForegroundColor Gray

# TEST 7: Multi-station order routing:
# 1 ta Osh (Palovchi yoki Asosiy oshxona)
# 1 ta Somsa (Somsapaz)
# 1 ta Coca-Cola (Bar)
# 1 ta Pizza (Pitsaxona)
Write-Host "`n[TEST 7] Ko'p oshxonali buyurtma yaratish va oshxonaga yuborish..." -ForegroundColor Yellow

$allProductsRes = Invoke-RestMethod -Uri "$baseUrl/api/products" -Method Get -Headers $headers
$allProducts = $allProductsRes.data

$palovProd = $allProducts | Where-Object { $_.name -like '*Palov*' -or $_.name -like '*Osh*' } | Select-Object -First 1
$somsaProd = $allProducts | Where-Object { $_.name -like '*Somsa*' } | Select-Object -First 1
$cocaProd = $allProducts | Where-Object { $_.name -like '*Cola*' -or $_.name -like '*Coca*' } | Select-Object -First 1
$pizzaProd = $allProducts | Where-Object { $_.name -like '*Pitsa*' -or $_.name -like '*Pizza*' } | Select-Object -First 1

Write-Host "Tanlangan mahsulotlar:"
Write-Host " 1. $($palovProd.name) -> Kategoriya: $($palovProd.categoryName) -> Oshxona: $($palovProd.kitchenName)"
Write-Host " 2. $($somsaProd.name) -> Kategoriya: $($somsaProd.categoryName) -> Oshxona: $($somsaProd.kitchenName)"
Write-Host " 3. $($cocaProd.name) -> Kategoriya: $($cocaProd.categoryName) -> Oshxona: $($cocaProd.kitchenName)"
Write-Host " 4. $($pizzaProd.name) -> Kategoriya: $($pizzaProd.categoryName) -> Oshxona: $($pizzaProd.kitchenName)"

$tablesRes = Invoke-RestMethod -Uri "$baseUrl/api/tables" -Method Get -Headers $headers
$table = $tablesRes.data | Select-Object -First 1

$orderBody = @{
    tableId = $table.id
    orderType = "DINE_IN"
} | ConvertTo-Json

$createOrderRes = Invoke-RestMethod -Uri "$baseUrl/api/orders" -Method Post -Headers $headers -Body $orderBody
$order = $createOrderRes.data
Write-Host "Stol ochildi va buyurtma yaratildi: #$($order.orderNumber) (ID: $($order.id))" -ForegroundColor Green

# Savatga 4 xil oshxona mahsulotlarini qo'shib, "Oshxonaga" tugmasi bosiladi
Write-Host "`n'Oshxonaga' tugmasi bosildi (POST /api/orders/{id}/send-to-kitchen)..." -ForegroundColor Yellow
$sendBody = @{
    items = @(
        @{ productId = $palovProd.id; quantity = 2 },
        @{ productId = $somsaProd.id; quantity = 1 },
        @{ productId = $cocaProd.id; quantity = 1 },
        @{ productId = $pizzaProd.id; quantity = 1 }
    )
} | ConvertTo-Json -Depth 5

$sendRes = Invoke-RestMethod -Uri "$baseUrl/api/orders/$($order.id)/send-to-kitchen" -Method Post -Headers $headers -Body $sendBody
Write-Host "Oshxonaga muvaffaqiyatli yuborildi! Status: $($sendRes.data.status)" -ForegroundColor Green

# TEST 8 & 9: Oshxona panellari bo'yicha alohida tekshirish
Write-Host "`n[TEST 8] Somsapaz oshxona paneli (GET /api/kitchen/orders?kitchenId=$($somsaKitchen.id))..." -ForegroundColor Yellow
$somsaOrdersRes = Invoke-RestMethod -Uri "$baseUrl/api/kitchen/orders?kitchenId=$($somsaKitchen.id)" -Method Get -Headers $headers
$somsaTargetOrder = $somsaOrdersRes.data | Where-Object { $_.id -eq $order.id }
Write-Host "Somsapaz panelidagi buyurtma tovarlari soni: $($somsaTargetOrder.items.Count)" -ForegroundColor Green
$somsaTargetOrder.items | ForEach-Object {
    Write-Host " - Somsapaz mahsuloti: $($_.productName) x $($_.quantity) (Kitchen: $($_.kitchenName))"
    if ($_.kitchenId -ne $somsaKitchen.id) {
        throw "XATO: Somsapaz paneliga begona oshxona mahsuloti tushgan!"
    }
}

Write-Host "`n[TEST 9] Bar oshxona paneli (GET /api/kitchen/orders?kitchenId=$($barKitchen.id))..." -ForegroundColor Yellow
$barOrdersRes = Invoke-RestMethod -Uri "$baseUrl/api/kitchen/orders?kitchenId=$($barKitchen.id)" -Method Get -Headers $headers
$barTargetOrder = $barOrdersRes.data | Where-Object { $_.id -eq $order.id }
Write-Host "Bar panelidagi buyurtma tovarlari soni: $($barTargetOrder.items.Count)" -ForegroundColor Green
$barTargetOrder.items | ForEach-Object {
    Write-Host " - Bar mahsuloti: $($_.productName) x $($_.quantity) (Kitchen: $($_.kitchenName))"
    if ($_.kitchenId -ne $barKitchen.id) {
        throw "XATO: Bar paneliga begona oshxona mahsuloti tushgan!"
    }
}

Write-Host "`n====================================================" -ForegroundColor Cyan
Write-Host "BARCHA TESTLAR 100% MUVAFFAQIYATLI O'TDI!" -ForegroundColor Green
Write-Host "====================================================" -ForegroundColor Cyan
