# L3 Support Email Processing Demo Setup Script

Write-Host "🚀 Setting up L3 Support Email Processing Demo..." -ForegroundColor Green

# Create directories
Write-Host "📁 Creating email directories..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path "emails" -Force | Out-Null
New-Item -ItemType Directory -Path "emails/processed" -Force | Out-Null

# Check if sample files exist
$sampleFiles = @("emails/sample_ticket_1.txt", "emails/sample_ticket_2.txt", "emails/sample_ticket_3.txt")

foreach ($file in $sampleFiles) {
    if (Test-Path $file) {
        Write-Host "✅ Sample file exists: $file" -ForegroundColor Green
    } else {
        Write-Host "❌ Sample file missing: $file" -ForegroundColor Red
    }
}

# Display folder structure
Write-Host "`n📂 Current folder structure:" -ForegroundColor Cyan
Get-ChildItem -Path "emails" -Recurse | Format-Table Name, Length, LastWriteTime

# Instructions
Write-Host "`n📋 Next Steps:" -ForegroundColor Magenta
Write-Host "1. Start application: ./mvnw spring-boot:run"
Write-Host "2. Test connection: GET http://localhost:8080/api/emails/test-connection"
Write-Host "3. Check file status: GET http://localhost:8080/api/emails/file-status"
Write-Host "4. Process files: GET http://localhost:8080/api/emails/fetch"
Write-Host "5. View Swagger UI: http://localhost:8080/swagger-ui/index.html"

Write-Host "`n📧 To add more emails:" -ForegroundColor Blue
Write-Host "- Save Outlook emails as .txt files in the 'emails' folder"
Write-Host "- Ensure they contain proper L3 support ticket format"
Write-Host "- Call the /fetch endpoint to process them"

Write-Host "`n✨ Demo ready! No permissions or registrations needed." -ForegroundColor Green