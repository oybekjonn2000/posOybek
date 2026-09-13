$body = @{ username = 'admin'; password = 'admin123' } | ConvertTo-Json
$token = (Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -ContentType 'application/json' -Body $body).data.accessToken
$headers = @{ Authorization = "Bearer $token" }
$kitchens = (Invoke-RestMethod -Uri 'http://localhost:8080/api/kitchens' -Headers $headers).data
$kitchens | Select-Object id, name, code, description, active, sortOrder, printerName | Format-Table -AutoSize
