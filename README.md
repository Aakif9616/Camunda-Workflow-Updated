# Job Recruitment Workflow - Dynamic Workflow Module

A Spring Boot application with Camunda BPM integration for dynamic job recruitment workflows.

## 🚀 Quick Start

### Prerequisites
- Java 11+
- Maven 3.6+

### Running the Application

1. **Clone and build**:
```bash
mvn clean install
mvn spring-boot:run
```

2. **Access the applications**:
- **Frontend**: http://localhost:8082/
- **HR Dashboard**: http://localhost:8082/hr-dashboard.html
- **Team Lead Dashboard**: http://localhost:8082/teamlead-dashboard.html
- **Project Manager Dashboard**: http://localhost:8082/projectmanager-dashboard.html
- **Head HR Dashboard**: http://localhost:8082/headhr-dashboard.html
- **Camunda Cockpit**: http://localhost:8082/camunda/app/cockpit/default/ (admin/admin)
- **Camunda Tasklist**: http://localhost:8082/camunda/app/tasklist/default/ (admin/admin)
- **API Health**: http://localhost:8082/api/job-applications/health

## 📋 API Testing

### Quick Test with curl:
```bash
# Health check
curl http://localhost:8082/api/job-applications/health

# Start new application
curl -X POST http://localhost:8082/api/job-applications/start

# Get workflow definition
curl http://localhost:8082/api/job-applications/workflow-definition
```

### Postman Testing
See `API_ENDPOINTS.md` for complete API documentation and testing examples.

## 🎯 Features

- **Dynamic Workflow**: JSON-driven multi-step forms
- **Camunda Integration**: Full BPMN workflow lifecycle with multi-level approval process
- **Multi-Level Approval System**:
  - **HR Review**: Initial screening and approval
  - **Parallel Review**: Team Lead and Project Manager review simultaneously
  - **Head HR Final Decision**: Final authority for hiring decisions
- **Role-Based Dashboards**: 
  - HR Dashboard for initial screening
  - Team Lead Dashboard for technical review
  - Project Manager Dashboard for project fit assessment
  - Head HR Dashboard for final hiring decisions
- **Indian Job Market**: Tailored for Indian recruitment process
- **Responsive UI**: Works on desktop and mobile
- **Real-time Validation**: Client and server-side validation
- **Process Monitoring**: Camunda Cockpit integration
- **Parallel Gateway Logic**: Both TL and PM must approve; rejection by either stops the process

## 🏗️ Architecture

- **Backend**: Spring Boot 2.6.15 + Camunda BPM 7.18
- **Frontend**: HTML5 + CSS3 + Vanilla JavaScript
- **Database**: H2 (development) / PostgreSQL (production ready)
- **Process Engine**: Camunda BPM embedded

## 🔄 Workflow Process

The application follows a comprehensive multi-level approval workflow:

1. **Applicant Submission** (3 Steps)
   - Personal Information
   - Job Preferences
   - Experience & Education

2. **Data Collection**
   - System collects and formats all applicant data

3. **HR Review** (First Gate)
   - HR reviews application in dashboard
   - Makes Accept/Reject decision in Camunda Tasklist
   - If **Rejected**: Process ends with rejection notification
   - If **Accepted**: Moves to parallel review

4. **Parallel Review** (Second Gate)
   - **Team Lead Review**: Technical assessment
   - **Project Manager Review**: Project fit assessment
   - Both reviews happen simultaneously
   - If **Either Rejects**: Process ends with rejection notification
   - If **Both Accept**: Moves to Head HR

5. **Head HR Final Review** (Final Gate)
   - Reviews all previous approvals and comments
   - Makes final hiring decision
   - Can specify offer CTC
   - If **Accepted**: Application stored, candidate hired
   - If **Rejected**: Process ends with rejection notification

### Approval Logic
- **HR Rejection**: Immediate rejection, no further reviews
- **TL/PM Rejection**: If either Team Lead OR Project Manager rejects, application is rejected
- **Head HR**: Final authority - can accept or reject even after all approvals

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/dynamicworkflow/
│   │   ├── controller/          # REST API controllers
│   │   ├── service/             # Business logic
│   │   ├── model/               # Data models
│   │   ├── dto/                 # Data transfer objects
│   │   ├── delegate/            # Camunda delegates
│   │   └── config/              # Configuration classes
│   └── resources/
│       ├── static/              # Frontend files
│       ├── processes/           # BPMN files
│       └── workflow-definition.json
```

## 🔧 Configuration

The application runs on **port 8082** by default. You can change this in `application.yml`:

```yaml
server:
  port: 8082
```

## 📖 Documentation

- `API_ENDPOINTS.md` - Complete API documentation
- `DEPENDENCIES_CHECK.md` - Dependencies and requirements
- `task_requirements.md` - Original project requirements

## 🧪 Testing

The application includes comprehensive testing capabilities:
- Unit tests for services and controllers
- Integration tests for API endpoints
- Camunda process testing with camunda-bpm-assert

Run tests with:
```bash
mvn test
```

## 🌐 Deployment

For production deployment:
1. Update `application.yml` with production database settings
2. Build the application: `mvn clean package`
3. Run the JAR: `java -jar target/job-recruitment-workflow-0.0.1-SNAPSHOT.jar`

## 📝 License

This project is for educational and demonstration purposes.