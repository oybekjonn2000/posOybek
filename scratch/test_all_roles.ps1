$roles = @(
    @{ user = 'admin'; pass = 'admin123' },
    @{ user = 'manager'; pass = 'manager123' },
    @{ user = 'waiter'; pass = 'waiter123' },
    @{ user = 'waiter1'; pass = 'waiter123' },
    @{ user = 'waiter2'; pass = 'waiter123' },
    @{ user = 'cashier'; pass = 'cashier123' }
)

foreach ($r in $roles) {
    try {
        $body = @{ username = $r.user; password = $r.pass } | ConvertTo-Json
        $res = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body $body -ContentType 'application/json'
        $token = $res.data.accessToken
        $headers = @{ Authorization = "Bearer $token" }

        $prods = Invoke-RestMethod -Uri 'http://localhost:8080/api/products?activeOnly=true' -Headers $headers
        Write-Host "Role check: User '$($r.user)' - Logged in successfully! Active products returned: $($prods.data.Count)"
    } catch {
        Write-Host "Role check: User '$($r.user)' - Error: $_"
    }
}
