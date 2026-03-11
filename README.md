## AI-Assisted Personal Finance Tracker (Backend)

This is a Spring Boot backend for an AI-assisted personal finance tracker.  
It provides secure REST APIs to manage:

- **User accounts**: register & login with JWT-based authentication
- **Transactions**: income & expenses with amount, category, date, and description
- **Budgets**: monthly spending limits with budget alerts
- **Analytics**: dashboard totals, top spending category, and anomaly detection for unusual transactions

### Tech Stack

- **Java 17**
- **Spring Boot 3**
- **Spring Security + JWT**
- **Spring Data JPA**
- **H2 in-memory database** (for development)

---

### Running the Backend

1. **Prerequisites**
   - Java 17 installed
   - Maven installed and on your `PATH` (or run from IntelliJ as a Spring Boot app)

2. **From the project root** (`finance-tracker` folder):

   ```bash
   mvn spring-boot:run
   ```

3. The server starts on **`http://localhost:8080`**.

4. Optional: H2 console is enabled at `http://localhost:8080/h2-console`  
   - JDBC URL: `jdbc:h2:mem:financedb`
   - User: `sa`
   - Password: (empty)

#### Windows quick setup (PowerShell)

If `mvn` is not recognized, install Java + Maven via `winget`:

```powershell
winget install Microsoft.OpenJDK.17
winget install Apache.Maven
```

Then open a new terminal and run:

```powershell
cd "C:\Users\ADMIN\Desktop\jp"
mvn spring-boot:run
```

---

### Authentication & Security

- JWT-based auth with `Spring Security`.
- Public endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- All other `/api/**` endpoints require an `Authorization: Bearer <token>` header.

#### Register

`POST /api/auth/register`

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "secret123"
}
```

**Response**

```json
{
  "token": "<JWT_TOKEN>"
}
```

#### Login

`POST /api/auth/login`

```json
{
  "username": "alice",
  "password": "secret123"
}
```

**Response**

```json
{
  "token": "<JWT_TOKEN>"
}
```

Use this token in subsequent requests:

```http
Authorization: Bearer <JWT_TOKEN>
```

---

### Transactions APIs

Base path: `/api/transactions`

- **Create transaction**

  `POST /api/transactions`

  ```json
  {
    "amount": 1200.00,
    "type": "INCOME",   // or "EXPENSE"
    "category": "Salary",
    "description": "Monthly pay",
    "date": "2026-03-01"
  }
  ```

- **List transactions**

  `GET /api/transactions`

  Optional filters:

  - `GET /api/transactions?from=2026-03-01&to=2026-03-31`

- **Update transaction**

  `PUT /api/transactions/{id}`

- **Delete transaction**

  `DELETE /api/transactions/{id}`

Transactions are always scoped to the **currently authenticated user**.

---

### Budget APIs

Base path: `/api/budgets`

- **Create/update monthly budget**

  `POST /api/budgets`

  ```json
  {
    "month": "2026-03",      // Year-Month (yyyy-MM)
    "monthlyLimit": 1500.00
  }
  ```

- **Get budget for a month**

  `GET /api/budgets/{month}`

  Example:

  - `GET /api/budgets/2026-03`

---

### Analytics & Dashboard

Base path: `/api/analytics`

- **Dashboard summary**

  `GET /api/analytics/dashboard`

  Example response:

  ```json
  {
    "totalIncome": 4000.00,
    "totalExpenses": 2400.00,
    "balance": 1600.00,
    "topSpendingCategory": "Food",
    "topSpendingAmount": 600.00,
    "monthlyBudgetLimit": 1500.00,
    "monthlyExpenses": 1300.00,
    "nearBudgetLimit": true
  }
  ```

- **Anomaly detection**

  `GET /api/analytics/anomalies`

  Detects unusually large expense transactions relative to your average expenses.

  Example response:

  ```json
  [
    {
      "transactionId": 42,
      "amount": 900.00,
      "category": "Electronics",
      "date": "2026-03-10",
      "reason": "Unusually large expense vs average"
    }
  ]
  ```

---

### Notes & Next Steps

- This backend uses an in-memory H2 DB for simplicity; in a real deployment you would switch to PostgreSQL/MySQL and configure credentials.
- The JWT secret in `application.yml` should be replaced with a long, random secret and stored securely (e.g. environment variable). This project accepts either a raw secret string or a BASE64-encoded secret.
- You can now build a frontend (web/mobile) that connects to these APIs for a full personal finance experience.

