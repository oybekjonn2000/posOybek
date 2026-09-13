$users = @(
    @{ user = 'admin'; pass = 'admin123' },
    @{ user = 'waiter1'; pass = 'waiter123' }
)

foreach ($u in $users) {
    try {
        $body = @{ username = $u.user; password = $u.pass } | ConvertTo-Json
        $res = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $body -ContentType 'application/json'
        $token = $res.data.accessToken
        $headers = @{ Authorization = "Bearer $token" }

        $prodsActive = Invoke-RestMethod -Uri 'http://localhost:8080/api/products?activeOnly=true' -Headers $headers
        $prodsAll = Invoke-RestMethod -Uri 'http://localhost:8080/api/products?activeOnly=false' -Headers $headers
        $catsActive = Invoke-RestMethod -Uri 'http://localhost:8080/api/categories?activeOnly=true' -Headers $headers
        $catsAll = Invoke-RestMethod -Uri 'http://localhost:8080/api/categories?activeOnly=false' -Headers $headers

        Write-Host "User: $($u.user)"
        Write-Host "  Products (activeOnly=true):  $($prodsActive.data.Count)"
        Write-Host "  Products (activeOnly=false): $($prodsAll.data.Count)"
        Write-Host "  Categories (activeOnly=true):  $($catsActive.data.Count)"
        Write-Host "  Categories (activeOnly=false): $($catsAll.data.Count)"
    } catch {
        Write-Host "Error for $($u.user): $_"
    }
}
