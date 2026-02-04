# Professional Job Recruitment Workflow System

A comprehensive job recruitment and onboarding system built with **Spring Boot** and **Camunda BPM**, featuring dynamic workflow management, email-based secure onboarding, and intelligent referral processing.

## 🚀 Features

### Core Workflow Management
- **Dynamic BPMN Workflow**: Complete job application process from submission to onboarding
- **Multi-level Approval System**: HR → Team Lead → Project Manager → Head HR → Company Manager
- **Referral Bypass Logic**: Valid referrals skip intermediate approvals
- **Real-time Status Tracking**: Live application status updates

### Email-Based Secure Onboarding
- **Professional Email Notifications**: Automated congratulations emails with secure links
- **Token-based Authentication**: Secure access without passwords
- **Comprehensive Onboarding Form**: 9 detailed sections for complete candidate information
- **Process Completion**: Automatic Camunda workflow termination

### Advanced Features
- **Referral System**: Built-in referral ID validation and bypass logic
- **Professional Dashboards**: Role-based approval interfaces
- **Form Validation**: Real-time validation with user-friendly feedback
- **Responsive Design**: Mobile-friendly interface

## 🛠️ Technology Stack

- **Backend**: Java 11, Spring Boot 2.6.15
- **Workflow Engine**: Camunda BPM 7.18.0
- **Database**: H2 (In-memory for development)
- **Email Service**: Gmail SMTP integration
- **Frontend**: HTML5, CSS3, JavaScript (ES6+)
- **Build Tool**: Maven
- **Version Control**: Git

## 📋 Prerequisites

- Java 11 or higher
- Maven 3.6+
- Git
- Gmail account with app password (for email features)

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/Aakif9616/camunda-dynamic-workflow.git
cd camunda-dynamic-workflow
```

### 2. Configure Email Settings
Update `src/main/resources/application.yml`:
```yaml
app:
  email:
    username: your-email@gmail.com
    password: your-app-password
```

### 3. Build and Run
```bash
mvn clean compile
mvn spring-boot:run
```

### 4. Access the Application
- **Main Application**: http://localhost:8083
- **Camunda Cockpit**: http://localhost:8083/camunda (admin/admin)
- **H2 Console**: http://localhost:8083/h2-console

## 📊 Application Flow

```
Application Submission → HR Review → Team Lead Review → Project Manager Review 
→ Head HR Review → Company Manager Review → HR Hiring → Email Notification 
→ Secure Onboarding → Process Completion
```

### Referral Flow (Bypass)
```
Application Submission (with valid referral) → HR Review → Company Manager Review 
→ HR Hiring → Email Notification → Secure Onboarding → Process Completion
```

## 🎯 Key Components

### Controllers
- `JobApplicationController` - Main application workflow
- `AuthController` - Role-based authentication
- `SecureOnboardingController` - Onboarding form routing
- `SecureOnboardingApiController` - Onboarding API endpoints
- `OnboardingAccessController` - Email token validation

### Services
- `JobApplicationService` - Core business logic
- `EmailService` - Professional email notifications
- `ValidationService` - Form and data validation
- `ReferralService` - Referral ID management
- `WorkflowDefinitionService` - Dynamic workflow management

### Key Features Implementation
- **Email Authentication**: Secure token-based access
- **Real-time Validation**: JavaScript form validation
- **Professional UI/UX**: Modern, responsive design
- **Process Management**: Complete Camunda integration

## 📱 User Interfaces

### Applicant Portal
- Application form submission
- Status tracking
- Secure onboarding access

### HR Dashboard
- Application review and approval
- Candidate hiring functionality
- Status management

### Management Dashboards
- Team Lead approval interface
- Project Manager review
- Head HR final review
- Company Manager approval

## 🔧 Configuration

### Email Configuration
```yaml
app:
  email:
    smtp:
      host: smtp.gmail.com
      port: 587
    username: your-email@gmail.com
    password: your-16-digit-app-password
```

### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:camunda-db
    username: sa
    password: 
```

## 📈 Workflow States

- `STARTED` - Application initiated
- `IN_PROGRESS` - Form completion in progress
- `PENDING_HR_REVIEW` - Awaiting HR approval
- `PENDING_TL_REVIEW` - Awaiting Team Lead approval
- `PENDING_PM_REVIEW` - Awaiting Project Manager approval
- `PENDING_HEAD_HR_REVIEW` - Awaiting Head HR approval
- `PENDING_COMPANY_MANAGER_REVIEW` - Awaiting final approval
- `PENDING_HR_HIRING` - Ready for HR hiring
- `HIRED` - Candidate hired, email sent
- `ONBOARDING_COMPLETED` - Process completed

## 🧪 Testing

### Manual Testing
1. Submit a job application
2. Navigate through approval dashboards
3. Test referral bypass functionality
4. Verify email notifications
5. Complete onboarding process

### Test Credentials
- **HR**: hr/password
- **Team Lead**: tl/password
- **Project Manager**: pm/password
- **Head HR**: headhr/password
- **Company Manager**: cm/password

## 📚 Documentation

- [Complete Workflow Test Guide](COMPLETE_WORKFLOW_TEST_GUIDE.md)
- [Email Testing Guide](EMAIL_TESTING_GUIDE.md)
- [Professional Onboarding Implementation](PROFESSIONAL_ONBOARDING_IMPLEMENTATION.md)
- [Referral Bypass Test Guide](REFERRAL_BYPASS_TEST_GUIDE.md)

## 🤝 Contributing

This is a personal project. If you'd like to suggest improvements:
1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Mohamed Aakif**
- GitHub: [@Aakif9616](https://github.com/Aakif9616)
- Email: mohamedaakif10616@gmail.com

## 🙏 Acknowledgments

- Camunda BPM for the excellent workflow engine
- Spring Boot team for the robust framework
- Contributors to open-source libraries used in this project

---

⭐ **Star this repository if you find it helpful!**