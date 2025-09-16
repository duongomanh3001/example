# Fix JOBE Server Configuration
Write-Host "Fixing JOBE Server Apache Configuration..." -ForegroundColor Yellow

# Stop container first
Write-Host "Stopping JOBE container..." -ForegroundColor Cyan
docker-compose -f docker-compose.jobe.yml down

# Create new Apache site config for JOBE
$jobeConfig = @"
<VirtualHost *:80>
    ServerAdmin webmaster@localhost
    DocumentRoot /var/www/html/jobe/public
    
    <Directory /var/www/html/jobe/public>
        AllowOverride All
        Require all granted
        DirectoryIndex index.php
    </Directory>
    
    # Enable rewrite module
    RewriteEngine On
    
    ErrorLog `${APACHE_LOG_DIR}/error.log
    CustomLog `${APACHE_LOG_DIR}/access.log combined
</VirtualHost>
"@

# Write config to temporary file
$jobeConfig | Out-File -FilePath "jobe-site.conf" -Encoding UTF8

Write-Host "Starting JOBE container..." -ForegroundColor Cyan
docker-compose -f docker-compose.jobe.yml up -d

# Wait for container to start
Start-Sleep -Seconds 5

Write-Host "Copying new Apache config..." -ForegroundColor Cyan
docker cp jobe-site.conf cscore-jobe-server:/etc/apache2/sites-available/000-default.conf

Write-Host "Enabling Apache modules..." -ForegroundColor Cyan
docker exec cscore-jobe-server a2enmod rewrite

Write-Host "Restarting Apache..." -ForegroundColor Cyan
docker exec cscore-jobe-server service apache2 restart

Write-Host "Cleaning up temporary file..." -ForegroundColor Cyan
Remove-Item "jobe-site.conf" -Force

Write-Host "Testing JOBE server..." -ForegroundColor Green
Start-Sleep -Seconds 3

try {
    $response = Invoke-WebRequest "http://localhost:4000/" -Method GET
    Write-Host "✓ JOBE Server Status: $($response.StatusCode)" -ForegroundColor Green
    
    $response = Invoke-WebRequest "http://localhost:4000/restapi/languages" -Method GET  
    Write-Host "✓ Languages API Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Languages: $($response.Content)" -ForegroundColor Gray
} catch {
    Write-Host "⚠ Error testing JOBE: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "JOBE Configuration Fixed!" -ForegroundColor Green