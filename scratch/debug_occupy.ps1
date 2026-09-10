$baseUrl = "http://localhost:8080"
function Login-User {
    param([string]$Username, [string]$Password)
    $res = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body (@{ username = $Username; password = $Password } | ConvertTo-Json) -ContentType "application/json"
    return $res.data.accessToken
}
$table1Id = "e1000000-0000-0000-0000-000000000001"
$waiter2Token = Login-User "waiter2" "waiter123"

# Check table state
$adminToken = Login-User "admin" "admin123"
$t1 = Invoke-RestMethod -Uri "$baseUrl/api/tables/$table1Id" -Method Get -Headers @{ Authorization = "Bearer $adminToken" }
Write-Host "Table 1 state before waiter2 occupy:" ($t1 | ConvertTo-Json -Depth 5)

try {
    $res = Invoke-RestMethod -Uri "$baseUrl/api/tables/$table1Id/occupy" -Method Post -Headers @{ Authorization = "Bearer $waiter2Token" }
    Write-Host "Success:" ($res | ConvertTo-Json -Depth 5)
} catch {
    Write-Host "Error status:" $_.Exception.Response.StatusCode
    $stream = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    Write-Host "Error body:" $reader.ReadToEnd()
}
