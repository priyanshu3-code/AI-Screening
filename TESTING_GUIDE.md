# JUnit Testing Guide - AI Resume Screener

Complete guide to run, write, and analyze tests for the application.

---

## 🧪 Run All Tests

### **Option 1: Maven Command (Recommended)**

```bash
cd C:\Users\varshita.yerva\Desktop\FDE\project-3\backend
mvn clean test
```

**Expected output:**
```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.resumescreener.service.HuggingFaceClientTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] BUILD SUCCESS
```

### **Option 2: IntelliJ IDE**

1. Right-click on `backend` folder
2. Select **Run → All Tests** (or `Ctrl+Shift+F10`)
3. Tests run in IDE with visual feedback

### **Option 3: Run Specific Test Class**

```bash
# Run only HuggingFaceClient tests
mvn -Dtest=HuggingFaceClientTest test

# Run only SessionManager tests
mvn -Dtest=SessionManagerTest test

# Run only Controller tests
mvn -Dtest=AnalysisControllerTest test
```

### **Option 4: Generate Coverage Report**

```bash
mvn clean test jacoco:report
```

Reports generated in: `backend/target/site/jacoco/index.html`

---

## 📋 Test Cases Overview

### **1. HuggingFaceClientTest** (5 tests)
Tests the LLM API client:

| Test | Purpose |
|------|---------|
| `testCallLLMSuccess` | Verify API call returns valid response |
| `testCallLLMFailure` | Verify retry logic on failure |
| `testExtractJsonFromResponse_OpenAIFormat` | Parse OpenAI chat format |
| `testExtractJsonFromResponse_GeneratedTextFormat` | Parse alternative format |
| `testExtractJsonFromResponse_InvalidJSON` | Handle invalid responses |

**Run:** `mvn -Dtest=HuggingFaceClientTest test`

---

### **2. SessionManagerTest** (7 tests)
Tests session management:

| Test | Purpose |
|------|---------|
| `testCreateSession` | Create session with unique ID |
| `testGetSession` | Retrieve session by ID |
| `testGetSession_NotFound` | Error handling for missing session |
| `testUpdateSession` | Update session data |
| `testDeleteSession` | Delete session |
| `testGetActiveSessionCount` | Count active sessions |
| `testSessionNotExpired` | Verify 24h expiry |

**Run:** `mvn -Dtest=SessionManagerTest test`

---

### **3. AIOrchestrationServiceTest** (4 tests)
Tests the 3 LLM orchestration:

| Test | Purpose |
|------|---------|
| `testAnalyzeResume_Success` | LLM Call 1 extraction |
| `testAnalyzeResume_Failure` | Fallback on API error |
| `testProcessCandidate_HighScore_InterviewQuestions` | LLM Call 2A (score >= 70%) |
| `testProcessCandidate_LowScore_RejectionGuidance` | LLM Call 2B (score < 70%) |
| `testProcessCandidate_AlwaysGeneratesSummary` | LLM Call 3 always runs |

**Run:** `mvn -Dtest=AIOrchestrationServiceTest test`

---

### **4. AnalysisControllerTest** (4 tests)
Tests REST API endpoints:

| Test | Purpose |
|------|---------|
| `testAnalyzeResume_Success` | POST /analysis/screen success |
| `testAnalyzeResume_SessionNotFound` | Handle missing session |
| `testGetResults_Success` | GET /analysis/{id}/results success |
| `testGetResults_NotFound` | Handle missing session |

**Run:** `mvn -Dtest=AnalysisControllerTest test`

---

### **5. ResumeControllerTest** (5 tests)
Tests file upload endpoints:

| Test | Purpose |
|------|---------|
| `testUploadResume_Success` | POST /resume/upload success |
| `testUploadResume_EmptyFile` | Reject empty files |
| `testUploadResume_FileTooLarge` | Reject files > 10MB |
| `testGetPreview_Success` | GET /resume/{id}/preview success |
| `testGetPreview_NotFound` | Handle missing session |

**Run:** `mvn -Dtest=ResumeControllerTest test`

---

## 📊 Test Results Interpretation

### **Success Output**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 8.234 s
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
```

### **Failed Test Output**
```
[ERROR] FAILURE in configuration of test class
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
```

---

## 🔍 View Detailed Test Results

### **In Terminal**
```bash
# Show test output with details
mvn test -X

# Show only failed tests
mvn test | grep FAILURE
```

### **In IntelliJ**
1. Run tests (`Ctrl+Shift+F10`)
2. Click on failed test in **Run** panel
3. See failure details in console

### **HTML Report**
```bash
mvn clean test
# Open: backend/target/surefire-reports/index.html
```

---

## ✅ Complete Test Workflow

### **Step 1: Clean Build**
```bash
cd C:\Users\varshita.yerva\Desktop\FDE\project-3\backend
mvn clean
```

### **Step 2: Install Dependencies**
```bash
mvn install -DskipTests
```

### **Step 3: Run All Tests**
```bash
mvn test
```

### **Step 4: View Results**
```bash
# In terminal - shows summary
# Or open: backend/target/surefire-reports/index.html
```

### **Step 5: Generate Coverage**
```bash
mvn jacoco:report
# Open: backend/target/site/jacoco/index.html
```

---

## 🧩 Add More Tests

### **Create a New Test File**

```java
// File: backend/src/test/java/com/resumescreener/service/MyServiceTest.java

package com.resumescreener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MyService Tests")
class MyServiceTest {

    private MyService myService;

    @BeforeEach
    void setUp() {
        myService = new MyService();
    }

    @Test
    @DisplayName("Should do something")
    void testSomething() {
        // Arrange
        
        // Act
        
        // Assert
    }
}
```

---

## 📈 Coverage Target

| Component | Target | Current |
|-----------|--------|---------|
| Backend | 80%+ | [Run tests to see] |
| Frontend | 70%+ | [Jasmine tests] |
| Controllers | 100% | [25 tests] |
| Services | 95%+ | [20 tests] |

---

## 🚀 Quick Commands

```bash
# Run all tests
mvn clean test

# Run specific test class
mvn -Dtest=HuggingFaceClientTest test

# Run with coverage
mvn clean test jacoco:report

# Run without executing
mvn test --dry-run

# Skip tests (for CI/CD)
mvn clean install -DskipTests

# Fail on first failure
mvn test -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
```

---

## 🐛 Troubleshooting

### **Tests Won't Run**
```bash
# Clean Maven cache
mvn clean -DskipTests
mvn install -DskipTests

# Check Java version
java -version  # Must be 21+
```

### **Mockito Issues**
```bash
# Ensure mockito-junit-jupiter is in pom.xml
mvn dependency:tree | grep mockito
```

### **Test Timeout**
```bash
# Increase timeout
mvn test -DtimeoutSeconds=300
```

---

## 📝 Test Annotations Used

| Annotation | Purpose |
|-----------|---------|
| `@Test` | Marks method as test |
| `@BeforeEach` | Runs before each test |
| `@DisplayName` | Descriptive test name |
| `@Mock` | Creates mock object |
| `@MockitoAnnotations.openMocks()` | Initializes mocks |

---

## ✨ Next Steps

1. ✅ Run all tests: `mvn clean test`
2. ✅ View coverage: `mvn jacoco:report`
3. ✅ Add more tests for edge cases
4. ✅ Integrate into CI/CD pipeline
5. ✅ Monitor coverage over time

---

**Happy Testing!** 🎉
