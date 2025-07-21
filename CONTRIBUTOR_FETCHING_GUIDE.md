# 🔄 **How Contributors Will Be Fetched - Complete Guide**

## **Overview**
The system has been updated from static contributor arrays to a dynamic database-driven approach. Here's how contributors are now fetched and managed:

---

## 🎯 **1. Email Parsing Process (Backend)**

### **When an email is processed:**

```java
// OLD WAY (Static Configuration)
app.l3.allowed.contributors=afreena.a@hepl.com,arun.se@hepl.com,kalpana.v@hepl.com...

// NEW WAY (Database-Driven)
1. Extract emails from "TO" field
2. Query active contributors from database
3. Match by email address first
4. Fallback to name pattern matching
5. Auto-create contributor if not found
```

### **Updated Email Parsing Logic:**

```java
// In TextEmailParserServiceImpl.java
private Contributor findContributor(String toEmails) {
    // 1. Extract all email addresses from TO field
    List<String> emailsInTo = extractEmails(toEmails);
    
    // 2. Get all active contributors from database
    List<Contributor> activeContributors = contributorService.getActiveContributors();
    
    // 3. Find exact email match
    for (Contributor contributor : activeContributors) {
        if (emailsInTo.contains(contributor.getEmail().toLowerCase())) {
            return contributor; // Found exact match
        }
    }
    
    // 4. Fallback: pattern matching by name
    // 5. Final fallback: create new contributor
}
```

---

## 🗄️ **2. Database Structure**

### **Contributors Table:**
```sql
CREATE TABLE contributors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE,
    employee_id VARCHAR(50),
    department VARCHAR(100),
    phone VARCHAR(20),
    active BOOLEAN DEFAULT TRUE,
    notes VARCHAR(500),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### **Updated Tickets Table:**
```sql
ALTER TABLE tickets 
ADD COLUMN contributor_id BIGINT,
ADD FOREIGN KEY (contributor_id) REFERENCES contributors(id);

-- Keep contributorName for backward compatibility
-- contributor_name VARCHAR(500) -- existing field
```

---

## 🚀 **3. Frontend Integration**

### **A. Replace Static Array:**

**OLD CODE:**
```typescript
const CONTRIBUTOR_NAMES = [
  'Kalpana V', 'Nandhini P', 'Manoj', 'Afreena', 
  'Arun Prasad', 'Venmani', 'Athithya', 'Others'
];
```

**NEW CODE:**
```typescript
// Fetch from API
const [contributors, setContributors] = useState([]);

useEffect(() => {
  fetch('http://localhost:8080/api/contributors/active')
    .then(res => res.json())
    .then(data => setContributors(data));
}, []);
```

### **B. Dropdown Component:**

```typescript
const ContributorDropdown = ({ value, onChange, onlyActive = true }) => {
  const [contributors, setContributors] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const endpoint = onlyActive 
      ? '/api/contributors/active' 
      : '/api/contributors';
    
    fetch(`http://localhost:8080${endpoint}`)
      .then(res => res.json())
      .then(data => {
        setContributors(data);
        setLoading(false);
      })
      .catch(err => {
        console.error('Error fetching contributors:', err);
        setLoading(false);
      });
  }, [onlyActive]);

  if (loading) return <div>Loading contributors...</div>;

  return (
    <select value={value} onChange={e => onChange(e.target.value)}>
      <option value="">Select Contributor</option>
      {contributors.map(contributor => (
        <option key={contributor.id} value={contributor.id}>
          {contributor.name}
          {contributor.email && ` (${contributor.email})`}
        </option>
      ))}
    </select>
  );
};
```

---

## 📊 **4. API Endpoints for Fetching Contributors**

### **Primary Endpoints:**

```javascript
// 1. GET ACTIVE CONTRIBUTORS (Most Common)
GET /api/contributors/active
// Use this for: Dropdowns, ticket assignment, email parsing

// 2. GET ALL CONTRIBUTORS
GET /api/contributors
// Use this for: Management pages, reports

// 3. SEARCH CONTRIBUTORS
GET /api/contributors/search?name=John
// Use this for: Search functionality, autocomplete

// 4. GET BY DEPARTMENT
GET /api/contributors/department/L3%20Support
// Use this for: Department-specific filtering
```

### **Response Format:**
```json
[
  {
    "id": 1,
    "name": "Kalpana V",
    "email": "kalpana.v@hepl.com",
    "employeeId": "EMP001",
    "department": "L3 Support",
    "phone": "+1234567890",
    "active": true,
    "notes": "Senior developer",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
]
```

---

## 🔄 **5. Migration Process**

### **Automatic Migration:**
1. **Default Contributors Created:** The system automatically creates contributors from your static list
2. **Email Mapping:** Known contributors get their emails assigned based on application.properties
3. **Ticket Migration:** Run `POST /api/tickets/migrate-contributors` to link existing tickets

### **Migration Mapping:**
```java
// Auto-created contributors with emails:
"Kalpana V" → kalpana.v@hepl.com
"Nandhini P" → nandhini.p@hepl.com  
"Manoj" → manoj.a@hepl.com
"Afreena" → afreena.a@hepl.com
"Arun Prasad" → arun.se@hepl.com
// Others created without emails initially
```

---

## ⚡ **6. Real-Time Fetching Scenarios**

### **Scenario 1: Email Processing**
```
Email arrives → Extract TO emails → Query database → Find contributor → Assign to ticket
```

### **Scenario 2: Frontend Dropdown**
```
Page loads → Fetch active contributors → Populate dropdown → User selects
```

### **Scenario 3: Ticket Assignment**
```
User selects contributor → Send contributor ID → Update ticket relationship
```

### **Scenario 4: New Contributor**
```
Unknown email found → Auto-create contributor → Assign to ticket → Notify admin
```

---

## 🛠️ **7. Implementation Steps for Frontend**

### **Step 1: Update Service Layer**
```typescript
// services/contributorService.ts
export class ContributorService {
  private baseUrl = 'http://localhost:8080/api/contributors';

  async getActiveContributors(): Promise<Contributor[]> {
    const response = await fetch(`${this.baseUrl}/active`);
    if (!response.ok) throw new Error('Failed to fetch contributors');
    return response.json();
  }

  async getAllContributors(): Promise<Contributor[]> {
    const response = await fetch(this.baseUrl);
    if (!response.ok) throw new Error('Failed to fetch contributors');
    return response.json();
  }

  // Add other CRUD methods...
}
```

### **Step 2: Update Components**
```typescript
// Replace all static CONTRIBUTOR_NAMES usage with API calls
// Update dropdowns to use contributor IDs instead of names
// Add loading states and error handling
```

### **Step 3: Add Management UI**
```typescript
// Create Contributors management page
// Add CRUD operations
// Include search and filter functionality
```

---

## 🔍 **8. Testing the New System**

### **Test API Endpoints:**
```bash
# Get active contributors
curl http://localhost:8080/api/contributors/active

# Create new contributor
curl -X POST http://localhost:8080/api/contributors \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@hepl.com","active":true}'

# Test email parsing (should now use database)
curl -X POST http://localhost:8080/api/text-email/parse \
  -H "Content-Type: text/plain" \
  -d "From: user@example.com
To: kalpana.v@hepl.com
Subject: Test Issue

This is a test email."
```

### **Verify Database:**
```sql
-- Check contributors table
SELECT * FROM contributors;

-- Check ticket-contributor relationships
SELECT t.id, t.ticket_summary, c.name as contributor_name, t.contributor_name as old_contributor
FROM tickets t 
LEFT JOIN contributors c ON t.contributor_id = c.id;
```

---

## ⚠️ **9. Important Notes**

### **Backward Compatibility:**
- Old `contributorName` field is kept for migration period
- Configuration-based fallback still works
- Existing tickets won't break

### **Performance:**
- Database queries are optimized with indexes
- Active contributors are cached
- Lazy loading for ticket relationships

### **Error Handling:**
- Graceful fallback to configuration if database fails
- Auto-creation of missing contributors
- Validation for duplicate emails/employee IDs

---

## 🎯 **10. Next Steps**

1. **Frontend Team:** Implement the API integration
2. **Testing:** Verify all endpoints work correctly
3. **Migration:** Run the migration script once
4. **Monitoring:** Check logs for any issues
5. **Cleanup:** Remove static arrays after successful migration

The system is now ready for dynamic contributor management! 🚀