try {
    $login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body (@{ username = 'waiter1'; password = 'waiter123' } | ConvertTo-Json) -ContentType 'application/json'
    Write-Host "Waiter permissions:" ($login.data.user.permissions -join ', ')
    Write-Host "Waiter role:" $login.data.user.role

    $orderPayload = @{
        tableId = 'e1000000-0000-0000-0000-000000000002'
        orderType = 'DINE_IN'
        items = @(
            @{
                productId = 'a1000000-0000-0000-0000-000000000022'
                quantity = 2
            }
        )
    } | ConvertTo-Json -Depth 5

    $orderRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/orders' -Method Post -Body $orderPayload -Headers @{ Authorization = "Bearer $($login.data.accessToken)" } -ContentType 'application/json'
    Write-Host "Order Success: $($orderRes.data.orderNumber) (Total: $($orderRes.data.total))"
} catch {
    Write-Host "Caught error:" $_
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "Server response body:" $reader.ReadToEnd()
    }
}
