# 📁 File-Based Email Processing - Zero Permission POC

## 🎯 Overview
This approach allows you to process L3 support ticket emails without requiring any special permissions, app passwords, or Azure registrations.

## 🚀 How It Works

### Step 1: Export Emails from Outlook
1. **Open Outlook** and go to your inbox
2. **Select the L3 support emails** you want to process
3. **Right-click** → **Save As** → Choose **Text (.txt)** format
4. **Save files** to the `emails` folder in your project

### Step 2: File Naming Convention
- Use descriptive names like: `ticket_2024_12_15_001.txt`
- Or use the subject line: `L3_Support_CKPL_Issue.txt`

### Step 3: Required Email Format
Your email text files should contain these fields:
```
Ticket Summary: [Summary here]
Project/Product: [Project name - must be one of: CK_Alumni, CKPL, HEPL]
Issue Description: [Detailed description]
Priority: [HIGH/MEDIUM/LOW]
Ticket Owner: [Owner name]
Contributor: [Contributor name]
Bug/Enhancement: [BUG/ENHANCEMENT]
Status: [OPEN/NEW/PENDING/RESOLVED]
Review: [Review comments]
Impact/Roles: [Impact description]
Contact: [Contact email]
Employee ID: [Employee ID]
Employee Name: [Employee name]
```

## 🛠️ Usage Steps

### 1. Start Your Application
```bash
./mvnw spring-boot:run
```

### 2. Test File System Access
```bash
GET http://localhost:8080/api/emails/test-connection
```
**Expected Response:**
```json
{
  "connected": true,
  "method": "File System",
  "message": "✅ Email connection successful!",
  "timestamp": 1234567890
}
```

### 3. Process Email Files
```bash
GET http://localhost:8080/api/emails/fetch
```
**Expected Response:**
```json
{
  "success": true,
  "method": "File System",
  "message": "✅ Email fetch triggered successfully!",
  "timestamp": 1234567890
}
```

## 📂 Folder Structure
```
emails/
├── sample_ticket_1.txt          # Unprocessed emails
├── sample_ticket_2.txt
├── sample_ticket_3.txt
└── processed/                   # Processed emails moved here
    ├── sample_ticket_1.txt
    └── sample_ticket_2.txt
```

## 🔍 Monitoring & Logs
- **Application logs** will show processing details
- **Database** will contain extracted tickets
- **Processed files** are moved to `emails/processed/` folder

## 📊 View Processed Tickets
Access your ticket dashboard at:
```
http://localhost:8080/swagger-ui/index.html
```

## 🎯 Demo Process

### 1. Add New Email File
Create a new file in the `emails` folder:
```
emails/my_test_ticket.txt
```

### 2. Add Email Content
Copy your L3 support email content with proper formatting

### 3. Process Files
Call the `/fetch` endpoint

### 4. Check Results
- Check application logs
- Verify database entries
- See processed file moved to `processed/` folder

## 🔄 Continuous Processing
- **Add new files** to `emails/` folder anytime
- **Call `/fetch` endpoint** to process them
- **Files are automatically moved** to avoid reprocessing

## 📈 Benefits of This Approach

✅ **No Permissions Required** - Works with any email system
✅ **Zero Configuration** - No Azure/Exchange setup needed
✅ **Immediate Testing** - Start processing right away
✅ **Full Functionality** - Demonstrates complete ticket extraction
✅ **Scalable** - Can process hundreds of emails
✅ **Audit Trail** - Keeps processed files for reference

## 🔧 Troubleshooting

### Issue: No files processed
- Check file format (must be .txt)
- Verify project name in content (CK_Alumni, CKPL, HEPL)
- Check application logs for errors

### Issue: Parsing errors
- Ensure all required fields are present
- Check field names match exactly
- Verify enum values (HIGH/MEDIUM/LOW, etc.)

### Issue: Database errors
- Check MySQL connection
- Verify database schema
- Check for duplicate messageId

## 🎪 Live Demo Script

1. **Show empty database**
2. **Add sample email files**
3. **Call test-connection endpoint**
4. **Call fetch endpoint**
5. **Show processed tickets in database**
6. **Show moved files in processed folder**

**Perfect for stakeholder demonstrations!**