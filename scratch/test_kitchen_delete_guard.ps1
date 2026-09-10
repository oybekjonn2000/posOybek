$baseUrl = "http://localhost:8080"
$loginBody = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
$headers = @{ Authorization = "Bearer $($loginRes.data.accessToken)" }

Write-Host "Attempting to delete 'Palovchi' kitchen (has categories & products)..."
try {
    Invoke-RestMethod -Uri "$baseUrl/api/kitchens/d0000000-0000-0000-0000-000000000001" -Method Delete -Headers $headers
    Write-Host "XATO: Kitchen deleted when it should have been blocked!" -ForegroundColor Red
} catch {
    Write-Host "MUVAFFAQIYATLI BLOKLANDI:" -ForegroundColor Green
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    Write-Host $reader.ReadToEnd() -ForegroundColor Green
}
