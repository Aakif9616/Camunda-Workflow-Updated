// Minimal Workflow Manager - Fresh Version
class SimpleWorkflowManager {
    constructor() {
        this.workflowDefinition = null;
        this.currentStepIndex = 0;
        this.applicationId = null;
        this.stepData = {};
        this.baseUrl = '/api/job-applications';
    }

    async initialize() {
        try {
            console.log('Initializing simple workflow...');
            await this.loadWorkflowDefinition();
            await this.startApplication();
            this.renderCurrentStep();
            console.log('Simple workflow initialized successfully');
        } catch (error) {
            console.error('Failed to initialize workflow:', error);
            this.showMessage('Failed to load workflow. Please refresh the page.', 'error');
        }
    }

    async loadWorkflowDefinition() {
        const response = await fetch(`${this.baseUrl}/workflow-definition`);
        if (!response.ok) {
            throw new Error('Failed to load workflow definition');
        }
        this.workflowDefinition = await response.json();
        console.log('Workflow definition loaded:', this.workflowDefinition);
    }

    async startApplication() {
        const response = await fetch(`${this.baseUrl}/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });
        
        if (!response.ok) {
            throw new Error('Failed to start application');
        }
        
        const result = await response.json();
        this.applicationId = result.applicationId;
        console.log('Application started:', this.applicationId);
        this.showMessage(`Application started! ID: ${this.applicationId}`, 'success');
    }

    renderCurrentStep() {
        const container = document.getElementById('current-step');
        const step = this.workflowDefinition.steps[this.currentStepIndex];

        container.innerHTML = `
            <div class="step-header">
                <h2 class="step-title">${step.stepName}</h2>
                <p class="step-description">${step.description}</p>
            </div>
            <form id="step-form" class="step-form">
                ${this.renderFields(step.fields)}
            </form>
        `;

        this.attachEventListeners();
        this.updateNavigationButtons();
        this.renderProgressIndicators();
    }

    renderProgressIndicators() {
        const container = document.getElementById('step-indicators');
        if (!container) return;
        
        container.innerHTML = '';

        this.workflowDefinition.steps.forEach((step, index) => {
            const indicator = document.createElement('div');
            indicator.className = 'step-indicator';
            if (index === this.currentStepIndex) {
                indicator.classList.add('active');
            } else if (index < this.currentStepIndex) {
                indicator.classList.add('completed');
            }

            indicator.innerHTML = `
                <div class="step-number">${index + 1}</div>
                <div class="step-name">${step.stepName}</div>
            `;

            container.appendChild(indicator);
        });

        // Update progress bar
        const progressFill = document.getElementById('progress-fill');
        if (progressFill) {
            const progressPercentage = ((this.currentStepIndex + 1) / this.workflowDefinition.steps.length) * 100;
            progressFill.style.width = `${progressPercentage}%`;
        }
    }

    renderFields(fields) {
        return fields.map(field => {
            const isRequired = field.required ? 'required' : '';
            const requiredClass = field.required ? 'required' : '';
            
            switch (field.fieldType) {
                case 'text':
                case 'email':
                    return `
                        <div class="form-group">
                            <label class="form-label ${requiredClass}" for="${field.fieldId}">
                                ${field.fieldName}
                            </label>
                            <input 
                                type="text"
                                id="${field.fieldId}"
                                name="${field.fieldId}"
                                class="form-input"
                                placeholder="${field.placeholder || ''}"
                                ${isRequired}
                                value="${this.stepData[field.fieldId] || ''}"
                            />
                            <span class="error-message" id="${field.fieldId}-error"></span>
                        </div>
                    `;

                case 'number':
                    return `
                        <div class="form-group">
                            <label class="form-label ${requiredClass}" for="${field.fieldId}">
                                ${field.fieldName}
                            </label>
                            <input 
                                type="number"
                                id="${field.fieldId}"
                                name="${field.fieldId}"
                                class="form-input"
                                placeholder="${field.placeholder || ''}"
                                ${isRequired}
                                value="${this.stepData[field.fieldId] || ''}"
                            />
                            <span class="error-message" id="${field.fieldId}-error"></span>
                        </div>
                    `;

                case 'date':
                    return `
                        <div class="form-group">
                            <label class="form-label ${requiredClass}" for="${field.fieldId}">
                                ${field.fieldName}
                            </label>
                            <input 
                                type="date"
                                id="${field.fieldId}"
                                name="${field.fieldId}"
                                class="form-input"
                                ${isRequired}
                                value="${this.stepData[field.fieldId] || ''}"
                            />
                            <span class="error-message" id="${field.fieldId}-error"></span>
                        </div>
                    `;

                case 'dropdown':
                    const options = field.options.map(option => 
                        `<option value="${option.value}" ${this.stepData[field.fieldId] === option.value ? 'selected' : ''}>
                            ${option.label}
                        </option>`
                    ).join('');
                    
                    return `
                        <div class="form-group">
                            <label class="form-label ${requiredClass}" for="${field.fieldId}">
                                ${field.fieldName}
                            </label>
                            <select 
                                id="${field.fieldId}"
                                name="${field.fieldId}"
                                class="form-select"
                                ${isRequired}
                            >
                                <option value="">Select ${field.fieldName}</option>
                                ${options}
                            </select>
                            <span class="error-message" id="${field.fieldId}-error"></span>
                        </div>
                    `;

                case 'education-autocomplete':
                    const selectedEducation = this.stepData[field.fieldId] || '';
                    
                    return `
                        <div class="form-group">
                            <label class="form-label ${requiredClass}">
                                ${field.fieldName}
                            </label>
                            <div class="education-input-container">
                                <div class="education-search-wrapper">
                                    <input 
                                        type="text"
                                        id="${field.fieldId}-search"
                                        class="education-search-input"
                                        placeholder="Type to search education levels (e.g., Bachelor, Master, PhD)..."
                                        autocomplete="off"
                                        value="${selectedEducation}"
                                    />
                                    <div class="education-dropdown" id="${field.fieldId}-dropdown" style="display: none;">
                                        <!-- Education suggestions will appear here -->
                                    </div>
                                </div>
                                <input type="hidden" id="${field.fieldId}" name="${field.fieldId}" value="${selectedEducation}" />
                            </div>
                            <span class="error-message" id="${field.fieldId}-error"></span>
                        </div>
                    `;

                case 'radio':
                    const radioOptions = field.options.map(option => 
                        `<div class="radio-option ${this.stepData[field.fieldId] === option.value ? 'selected' : ''}">
                            <input 
                                type="radio" 
                                id="${field.fieldId}-${option.value}"
                                name="${field.fieldId}"
                                value="${option.value}"
                                ${this.stepData[field.fieldId] === option.value ? 'checked' : ''}
                            />
                            <label for="${field.fieldId}-${option.value}">${option.label}</label>
                        </div>`
                    ).join('');
                    
                    return `
                        <div class="form-group">
                            <label class="form-label ${requiredClass}">
                                ${field.fieldName}
                            </label>
                            <div class="radio-group" data-field-id="${field.fieldId}">
                                ${radioOptions}
                            </div>
                            <span class="error-message" id="${field.fieldId}-error"></span>
                        </div>
                    `;

                case 'checkbox':
                    const savedValues = this.stepData[field.fieldId] || [];
                    const checkboxOptions = field.options.map(option => 
                        `<div class="checkbox-option ${savedValues.includes(option.value) ? 'selected' : ''}">
                            <input 
                                type="checkbox" 
                                id="${field.fieldId}-${option.value}"
                                name="${field.fieldId}"
                                value="${option.value}"
                                ${savedValues.includes(option.value) ? 'checked' : ''}
                            />
                            <label for="${field.fieldId}-${option.value}">${option.label}</label>
                        </div>`
                    ).join('');
                    
                    return `
                        <div class="form-group">
                            <label class="form-label ${requiredClass}">
                                ${field.fieldName}
                            </label>
                            <div class="checkbox-group" data-field-id="${field.fieldId}">
                                ${checkboxOptions}
                            </div>
                            <span class="error-message" id="${field.fieldId}-error"></span>
                        </div>
                    `;

                case 'skills-autocomplete':
                    const selectedSkills = this.stepData[field.fieldId] || [];
                    const selectedSkillsHtml = selectedSkills.map(skill => 
                        `<span class="skill-tag">
                            ${skill}
                            <button type="button" class="skill-remove" data-skill="${skill}">×</button>
                        </span>`
                    ).join('');
                    
                    return `
                        <div class="form-group">
                            <label class="form-label ${requiredClass}">
                                ${field.fieldName}
                            </label>
                            <div class="skills-input-container">
                                <div class="selected-skills" id="${field.fieldId}-selected">
                                    ${selectedSkillsHtml}
                                </div>
                                <div class="skills-search-wrapper">
                                    <input 
                                        type="text"
                                        id="${field.fieldId}-search"
                                        class="skills-search-input"
                                        placeholder="Type to search skills (e.g., HTML, JavaScript, Python)..."
                                        autocomplete="off"
                                    />
                                    <div class="skills-dropdown" id="${field.fieldId}-dropdown" style="display: none;">
                                        <!-- Skills suggestions will appear here -->
                                    </div>
                                </div>
                                <input type="hidden" id="${field.fieldId}" name="${field.fieldId}" value="${JSON.stringify(selectedSkills).replace(/"/g, '&quot;')}" />
                            </div>
                            <span class="error-message" id="${field.fieldId}-error"></span>
                        </div>
                    `;

                default:
                    return `<div class="form-group">Unsupported field type: ${field.fieldType}</div>`;
            }
        }).join('');
    }

    // Comprehensive skills database
    getSkillsDatabase() {
        return [
            // Programming Languages
            'JavaScript', 'TypeScript', 'Java', 'Python', 'C++', 'C#', 'C', 'Go', 'Rust', 'Swift', 'Kotlin', 'Scala', 'Ruby', 'PHP', 'Perl', 'R', 'MATLAB', 'Objective-C', 'Dart', 'Elixir',
            
            // Web Technologies - Frontend
            'HTML', 'HTML5', 'CSS', 'CSS3', 'SCSS', 'SASS', 'Less', 'Bootstrap', 'Tailwind CSS', 'Material-UI', 'Ant Design', 'Chakra UI', 'Bulma', 'Foundation',
            
            // JavaScript Frameworks & Libraries
            'React', 'React.js', 'React Native', 'Vue.js', 'Vue', 'Angular', 'AngularJS', 'Svelte', 'Next.js', 'Nuxt.js', 'Gatsby', 'Ember.js', 'Backbone.js', 'jQuery', 'Alpine.js',
            
            // Backend Frameworks
            'Node.js', 'Express.js', 'Koa.js', 'Fastify', 'NestJS', 'Spring Boot', 'Spring Framework', 'Spring MVC', 'Spring Security', 'Django', 'Flask', 'FastAPI', 'Ruby on Rails', 'Laravel', 'Symfony', 'CodeIgniter', 'ASP.NET', '.NET Core', 'Gin', 'Echo', 'Fiber',
            
            // Databases
            'MySQL', 'PostgreSQL', 'MongoDB', 'Redis', 'SQLite', 'Oracle', 'SQL Server', 'MariaDB', 'Cassandra', 'DynamoDB', 'CouchDB', 'Neo4j', 'InfluxDB', 'Elasticsearch', 'Firebase Firestore', 'Supabase',
            
            // Cloud Platforms
            'AWS', 'Amazon Web Services', 'Azure', 'Google Cloud Platform', 'GCP', 'Heroku', 'DigitalOcean', 'Linode', 'Vultr', 'Netlify', 'Vercel', 'Railway', 'Render',
            
            // AWS Services
            'EC2', 'S3', 'RDS', 'Lambda', 'API Gateway', 'CloudFormation', 'CloudWatch', 'IAM', 'VPC', 'Route 53', 'CloudFront', 'ELB', 'Auto Scaling', 'ECS', 'EKS', 'Fargate',
            
            // DevOps & Tools
            'Docker', 'Kubernetes', 'Jenkins', 'GitLab CI', 'GitHub Actions', 'CircleCI', 'Travis CI', 'Ansible', 'Terraform', 'Vagrant', 'Chef', 'Puppet', 'Helm', 'ArgoCD', 'Prometheus', 'Grafana', 'ELK Stack', 'Nagios',
            
            // Version Control
            'Git', 'GitHub', 'GitLab', 'Bitbucket', 'SVN', 'Mercurial',
            
            // Testing
            'Jest', 'Mocha', 'Chai', 'Cypress', 'Selenium', 'Playwright', 'Puppeteer', 'JUnit', 'TestNG', 'Mockito', 'PyTest', 'unittest', 'RSpec', 'PHPUnit', 'Postman', 'Insomnia',
            
            // Mobile Development
            'React Native', 'Flutter', 'Ionic', 'Xamarin', 'Cordova', 'PhoneGap', 'Swift', 'Objective-C', 'Kotlin', 'Java Android', 'Android Studio', 'Xcode',
            
            // Data Science & ML
            'TensorFlow', 'PyTorch', 'Keras', 'Scikit-learn', 'Pandas', 'NumPy', 'Matplotlib', 'Seaborn', 'Plotly', 'Jupyter', 'Apache Spark', 'Hadoop', 'Kafka', 'Airflow', 'MLflow', 'Kubeflow',
            
            // Python Libraries
            'Django', 'Flask', 'FastAPI', 'Pandas', 'NumPy', 'Matplotlib', 'Seaborn', 'Requests', 'BeautifulSoup', 'Scrapy', 'Celery', 'SQLAlchemy', 'Alembic', 'Pydantic', 'PyTorch', 'TensorFlow', 'Keras', 'OpenCV',
            
            // Java Technologies
            'Spring Boot', 'Spring Framework', 'Spring MVC', 'Spring Security', 'Spring Data', 'Hibernate', 'JPA', 'Maven', 'Gradle', 'JUnit', 'TestNG', 'Mockito', 'Apache Kafka', 'Apache Camel', 'Quarkus', 'Micronaut',
            
            // Build Tools
            'Webpack', 'Vite', 'Rollup', 'Parcel', 'Gulp', 'Grunt', 'Maven', 'Gradle', 'npm', 'Yarn', 'pnpm', 'Lerna', 'Rush', 'Nx',
            
            // API Technologies
            'REST API', 'GraphQL', 'gRPC', 'WebSocket', 'Socket.io', 'Apollo GraphQL', 'Relay', 'OpenAPI', 'Swagger', 'Postman', 'Insomnia',
            
            // Microservices
            'Microservices', 'Service Mesh', 'Istio', 'Consul', 'Eureka', 'Zuul', 'API Gateway', 'Circuit Breaker', 'Event Sourcing', 'CQRS',
            
            // Message Queues
            'Apache Kafka', 'RabbitMQ', 'Apache ActiveMQ', 'Redis Pub/Sub', 'Amazon SQS', 'Google Pub/Sub', 'Apache Pulsar', 'NATS',
            
            // Monitoring & Logging
            'Prometheus', 'Grafana', 'ELK Stack', 'Elasticsearch', 'Logstash', 'Kibana', 'Fluentd', 'Jaeger', 'Zipkin', 'New Relic', 'Datadog', 'Splunk',
            
            // Security
            'OAuth', 'JWT', 'SAML', 'LDAP', 'SSL/TLS', 'HTTPS', 'CORS', 'XSS', 'CSRF', 'SQL Injection', 'Penetration Testing', 'Vulnerability Assessment',
            
            // Operating Systems
            'Linux', 'Ubuntu', 'CentOS', 'Red Hat', 'Debian', 'Alpine', 'Windows Server', 'macOS', 'Unix', 'Shell Scripting', 'Bash', 'PowerShell',
            
            // Methodologies
            'Agile', 'Scrum', 'Kanban', 'DevOps', 'CI/CD', 'TDD', 'BDD', 'DDD', 'Clean Architecture', 'SOLID Principles', 'Design Patterns', 'Microservices Architecture',
            
            // Other Tools
            'Postman', 'Insomnia', 'Figma', 'Adobe XD', 'Sketch', 'InVision', 'Zeplin', 'Slack', 'Discord', 'Jira', 'Confluence', 'Trello', 'Asana', 'Notion'
        ].sort();
    }

    searchSkills(query) {
        if (!query || query.length < 1) return [];
        
        const skills = this.getSkillsDatabase();
        const lowerQuery = query.toLowerCase();
        
        // Exact matches first, then starts with, then contains
        const exactMatches = skills.filter(skill => skill.toLowerCase() === lowerQuery);
        const startsWithMatches = skills.filter(skill => 
            skill.toLowerCase().startsWith(lowerQuery) && !exactMatches.includes(skill)
        );
        const containsMatches = skills.filter(skill => 
            skill.toLowerCase().includes(lowerQuery) && 
            !exactMatches.includes(skill) && 
            !startsWithMatches.includes(skill)
        );
        
        return [...exactMatches, ...startsWithMatches, ...containsMatches].slice(0, 10);
    }

    // Comprehensive education database
    getEducationDatabase() {
        return [
            // High School / Secondary Education
            'High School Diploma',
            'Secondary School Certificate',
            'Higher Secondary Certificate',
            'Class 12th',
            'Class 10th',
            'GED (General Educational Development)',
            'International Baccalaureate (IB)',
            'A-Levels',
            'O-Levels',
            
            // Undergraduate Degrees
            'Bachelor of Technology (B.Tech)',
            'Bachelor of Engineering (B.E.)',
            'Bachelor of Computer Science (B.CS)',
            'Bachelor of Science (B.Sc)',
            'Bachelor of Computer Applications (BCA)',
            'Bachelor of Information Technology (B.IT)',
            'Bachelor of Arts (B.A.)',
            'Bachelor of Commerce (B.Com)',
            'Bachelor of Business Administration (BBA)',
            'Bachelor of Fine Arts (BFA)',
            'Bachelor of Architecture (B.Arch)',
            'Bachelor of Design (B.Des)',
            'Bachelor of Pharmacy (B.Pharm)',
            'Bachelor of Medicine (MBBS)',
            'Bachelor of Dental Surgery (BDS)',
            'Bachelor of Veterinary Science (B.V.Sc)',
            'Bachelor of Law (LLB)',
            'Bachelor of Education (B.Ed)',
            'Bachelor of Social Work (BSW)',
            'Bachelor of Journalism (B.J.)',
            'Bachelor of Mass Communication (BMC)',
            
            // Master's Degrees
            'Master of Technology (M.Tech)',
            'Master of Engineering (M.E.)',
            'Master of Computer Science (M.CS)',
            'Master of Science (M.Sc)',
            'Master of Computer Applications (MCA)',
            'Master of Information Technology (M.IT)',
            'Master of Arts (M.A.)',
            'Master of Commerce (M.Com)',
            'Master of Business Administration (MBA)',
            'Master of Fine Arts (MFA)',
            'Master of Architecture (M.Arch)',
            'Master of Design (M.Des)',
            'Master of Pharmacy (M.Pharm)',
            'Master of Medicine (MD)',
            'Master of Surgery (MS)',
            'Master of Dental Surgery (MDS)',
            'Master of Law (LLM)',
            'Master of Education (M.Ed)',
            'Master of Social Work (MSW)',
            'Master of Journalism (M.J.)',
            'Master of Public Administration (MPA)',
            'Master of Public Health (MPH)',
            'Master of Library Science (MLS)',
            
            // Doctoral Degrees
            'Doctor of Philosophy (Ph.D)',
            'Doctor of Technology (D.Tech)',
            'Doctor of Science (D.Sc)',
            'Doctor of Medicine (MD)',
            'Doctor of Dental Medicine (DMD)',
            'Doctor of Veterinary Medicine (DVM)',
            'Doctor of Law (JD)',
            'Doctor of Education (Ed.D)',
            'Doctor of Business Administration (DBA)',
            'Doctor of Engineering (D.Eng)',
            'Doctor of Pharmacy (Pharm.D)',
            'Doctor of Psychology (Psy.D)',
            'Doctor of Public Health (DrPH)',
            'Doctor of Nursing Practice (DNP)',
            
            // Diplomas and Certificates
            'Diploma in Engineering',
            'Diploma in Computer Science',
            'Diploma in Information Technology',
            'Diploma in Electronics',
            'Diploma in Mechanical Engineering',
            'Diploma in Civil Engineering',
            'Diploma in Electrical Engineering',
            'Diploma in Architecture',
            'Diploma in Pharmacy',
            'Diploma in Nursing',
            'Diploma in Hotel Management',
            'Diploma in Fashion Design',
            'Diploma in Interior Design',
            'Diploma in Graphic Design',
            'Diploma in Digital Marketing',
            'Diploma in Data Science',
            'Diploma in Artificial Intelligence',
            'Diploma in Cyber Security',
            'Diploma in Cloud Computing',
            'Diploma in Web Development',
            'Diploma in Mobile App Development',
            'Diploma in Game Development',
            'Diploma in Animation',
            'Diploma in Film Making',
            'Diploma in Photography',
            'Diploma in Journalism',
            'Diploma in Mass Communication',
            'Diploma in Public Relations',
            'Diploma in Event Management',
            'Diploma in Human Resources',
            'Diploma in Finance',
            'Diploma in Marketing',
            'Diploma in International Business',
            'Diploma in Supply Chain Management',
            'Diploma in Project Management',
            
            // Professional Certifications
            'Certified Public Accountant (CPA)',
            'Chartered Accountant (CA)',
            'Certified Management Accountant (CMA)',
            'Project Management Professional (PMP)',
            'Certified Information Systems Security Professional (CISSP)',
            'Certified Ethical Hacker (CEH)',
            'AWS Certified Solutions Architect',
            'Microsoft Certified Azure Solutions Architect',
            'Google Cloud Professional Cloud Architect',
            'Certified Kubernetes Administrator (CKA)',
            'Certified ScrumMaster (CSM)',
            'Six Sigma Black Belt',
            'ITIL Foundation',
            'Cisco Certified Network Associate (CCNA)',
            'CompTIA Security+',
            'Oracle Certified Professional',
            'Red Hat Certified Engineer (RHCE)',
            'VMware Certified Professional (VCP)',
            
            // International Degrees
            'Associate Degree',
            'Bachelor\'s Degree (US)',
            'Master\'s Degree (US)',
            'Doctoral Degree (US)',
            'Honours Degree',
            'Graduate Diploma',
            'Postgraduate Diploma',
            'Graduate Certificate',
            'Postgraduate Certificate',
            
            // Specialized Programs
            'Integrated M.Tech',
            'Dual Degree (B.Tech + M.Tech)',
            'Integrated MBA',
            'Executive MBA',
            'Part-time MBA',
            'Distance Learning MBA',
            'Online MBA',
            'Executive Master\'s',
            'Professional Master\'s',
            'Research Master\'s',
            'Coursework Master\'s',
            'Thesis Master\'s',
            
            // Other Qualifications
            'Trade Certificate',
            'Vocational Training',
            'Apprenticeship',
            'Professional Course',
            'Short-term Course',
            'Online Certification',
            'Bootcamp Certificate',
            'Industry Certification',
            'Government Certification',
            'International Certification'
        ].sort();
    }

    searchEducation(query) {
        if (!query || query.length < 1) return [];
        
        const educationLevels = this.getEducationDatabase();
        const lowerQuery = query.toLowerCase();
        
        // Exact matches first, then starts with, then contains
        const exactMatches = educationLevels.filter(edu => edu.toLowerCase() === lowerQuery);
        const startsWithMatches = educationLevels.filter(edu => 
            edu.toLowerCase().startsWith(lowerQuery) && !exactMatches.includes(edu)
        );
        const containsMatches = educationLevels.filter(edu => 
            edu.toLowerCase().includes(lowerQuery) && 
            !exactMatches.includes(edu) && 
            !startsWithMatches.includes(edu)
        );
        
        return [...exactMatches, ...startsWithMatches, ...containsMatches].slice(0, 10);
    }

    attachEventListeners() {
        const form = document.getElementById('step-form');
        
        // Initialize form data from existing values
        this.captureCurrentFormData();
        
        // Add change listeners
        form.addEventListener('change', (e) => {
            this.handleFieldChange(e);
        });

        form.addEventListener('input', (e) => {
            this.handleFieldChange(e);
        });

        // Add click listeners for radio and checkbox styling
        document.querySelectorAll('.radio-option, .checkbox-option').forEach(option => {
            option.addEventListener('click', (e) => {
                if (e.target.tagName !== 'INPUT') {
                    const input = option.querySelector('input');
                    if (input.type === 'radio') {
                        input.checked = true;
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                    } else if (input.type === 'checkbox') {
                        input.checked = !input.checked;
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                    }
                }
            });
        });

        // Initialize skills autocomplete
        this.initializeSkillsAutocomplete();
        
        // Initialize education autocomplete
        this.initializeEducationAutocomplete();
    }

    initializeEducationAutocomplete() {
        const educationSearchInputs = document.querySelectorAll('.education-search-input');
        
        educationSearchInputs.forEach(input => {
            const fieldId = input.id.replace('-search', '');
            const dropdown = document.getElementById(`${fieldId}-dropdown`);
            const hiddenInput = document.getElementById(fieldId);
            
            // Handle input for search
            input.addEventListener('input', (e) => {
                const query = e.target.value.trim();
                if (query.length > 0) {
                    const suggestions = this.searchEducation(query);
                    this.showEducationSuggestions(dropdown, suggestions, fieldId);
                } else {
                    dropdown.style.display = 'none';
                }
                
                // Update hidden input and stepData
                hiddenInput.value = query;
                this.stepData[fieldId] = query;
            });

            // Handle focus to show recent suggestions
            input.addEventListener('focus', (e) => {
                const query = e.target.value.trim();
                if (query.length > 0) {
                    const suggestions = this.searchEducation(query);
                    this.showEducationSuggestions(dropdown, suggestions, fieldId);
                }
            });

            // Handle blur to hide dropdown (with delay for click events)
            input.addEventListener('blur', (e) => {
                setTimeout(() => {
                    dropdown.style.display = 'none';
                }, 200);
            });

            // Handle Enter key
            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    dropdown.style.display = 'none';
                }
            });
        });
    }

    showEducationSuggestions(dropdown, suggestions, fieldId) {
        if (suggestions.length === 0) {
            dropdown.style.display = 'none';
            return;
        }

        dropdown.innerHTML = suggestions.map(education => 
            `<div class="education-suggestion" data-education="${education}">${education}</div>`
        ).join('');

        dropdown.style.display = 'block';

        // Add click listeners to suggestions
        dropdown.querySelectorAll('.education-suggestion').forEach(suggestion => {
            suggestion.addEventListener('click', (e) => {
                const education = e.target.getAttribute('data-education');
                this.selectEducation(fieldId, education);
                dropdown.style.display = 'none';
            });
        });
    }

    selectEducation(fieldId, education) {
        const searchInput = document.getElementById(`${fieldId}-search`);
        const hiddenInput = document.getElementById(fieldId);
        
        searchInput.value = education;
        hiddenInput.value = education;
        this.stepData[fieldId] = education;
    }

    initializeSkillsAutocomplete() {
        const skillsSearchInputs = document.querySelectorAll('.skills-search-input');
        
        skillsSearchInputs.forEach(input => {
            const fieldId = input.id.replace('-search', '');
            const dropdown = document.getElementById(`${fieldId}-dropdown`);
            const selectedContainer = document.getElementById(`${fieldId}-selected`);
            const hiddenInput = document.getElementById(fieldId);
            
            // Handle input for search
            input.addEventListener('input', (e) => {
                const query = e.target.value.trim();
                if (query.length > 0) {
                    const suggestions = this.searchSkills(query);
                    this.showSkillsSuggestions(dropdown, suggestions, fieldId);
                } else {
                    dropdown.style.display = 'none';
                }
            });

            // Handle focus to show recent suggestions
            input.addEventListener('focus', (e) => {
                const query = e.target.value.trim();
                if (query.length > 0) {
                    const suggestions = this.searchSkills(query);
                    this.showSkillsSuggestions(dropdown, suggestions, fieldId);
                }
            });

            // Handle blur to hide dropdown (with delay for click events)
            input.addEventListener('blur', (e) => {
                setTimeout(() => {
                    dropdown.style.display = 'none';
                }, 200);
            });

            // Handle skill removal
            selectedContainer.addEventListener('click', (e) => {
                if (e.target.classList.contains('skill-remove')) {
                    const skillToRemove = e.target.getAttribute('data-skill');
                    this.removeSkill(fieldId, skillToRemove);
                }
            });

            // Handle Enter key
            input.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    const query = e.target.value.trim();
                    if (query) {
                        // Add the typed skill directly if it's not empty
                        this.addSkill(fieldId, query);
                        e.target.value = '';
                        dropdown.style.display = 'none';
                    }
                }
            });
        });
    }

    showSkillsSuggestions(dropdown, suggestions, fieldId) {
        if (suggestions.length === 0) {
            dropdown.style.display = 'none';
            return;
        }

        const currentSkills = this.stepData[fieldId] || [];
        const filteredSuggestions = suggestions.filter(skill => !currentSkills.includes(skill));

        if (filteredSuggestions.length === 0) {
            dropdown.style.display = 'none';
            return;
        }

        dropdown.innerHTML = filteredSuggestions.map(skill => 
            `<div class="skill-suggestion" data-skill="${skill}">${skill}</div>`
        ).join('');

        dropdown.style.display = 'block';

        // Add click listeners to suggestions
        dropdown.querySelectorAll('.skill-suggestion').forEach(suggestion => {
            suggestion.addEventListener('click', (e) => {
                const skill = e.target.getAttribute('data-skill');
                this.addSkill(fieldId, skill);
                
                // Clear search input
                const searchInput = document.getElementById(`${fieldId}-search`);
                searchInput.value = '';
                dropdown.style.display = 'none';
            });
        });
    }

    addSkill(fieldId, skill) {
        const currentSkills = this.stepData[fieldId] || [];
        
        // Avoid duplicates
        if (!currentSkills.includes(skill)) {
            currentSkills.push(skill);
            this.stepData[fieldId] = currentSkills;
            this.updateSkillsDisplay(fieldId);
            this.updateHiddenInput(fieldId);
        }
    }

    removeSkill(fieldId, skill) {
        const currentSkills = this.stepData[fieldId] || [];
        const index = currentSkills.indexOf(skill);
        
        if (index > -1) {
            currentSkills.splice(index, 1);
            this.stepData[fieldId] = currentSkills;
            this.updateSkillsDisplay(fieldId);
            this.updateHiddenInput(fieldId);
        }
    }

    updateSkillsDisplay(fieldId) {
        const selectedContainer = document.getElementById(`${fieldId}-selected`);
        const currentSkills = this.stepData[fieldId] || [];
        
        selectedContainer.innerHTML = currentSkills.map(skill => 
            `<span class="skill-tag">
                ${skill}
                <button type="button" class="skill-remove" data-skill="${skill}">×</button>
            </span>`
        ).join('');
    }

    updateHiddenInput(fieldId) {
        const hiddenInput = document.getElementById(fieldId);
        const currentSkills = this.stepData[fieldId] || [];
        hiddenInput.value = JSON.stringify(currentSkills);
    }

    captureCurrentFormData() {
        // Capture all current form values
        const form = document.getElementById('step-form');
        if (!form) return;

        // Text inputs, selects, textareas
        form.querySelectorAll('input[type="text"], input[type="email"], input[type="number"], input[type="date"], select, textarea').forEach(input => {
            if (input.value && input.name) {
                this.stepData[input.name] = input.value;
                console.log('Captured field:', input.name, '=', input.value);
            }
        });

        // Hidden inputs (for skills and education autocomplete)
        form.querySelectorAll('input[type="hidden"]').forEach(input => {
            if (input.name && input.value) {
                try {
                    // Try to parse as JSON for skills data
                    if (input.name === 'skills' || input.id.includes('skills')) {
                        this.stepData[input.name] = JSON.parse(input.value);
                    } else {
                        // For education and other hidden inputs, store as string
                        this.stepData[input.name] = input.value;
                    }
                    console.log('Captured hidden field:', input.name, '=', this.stepData[input.name]);
                } catch (error) {
                    this.stepData[input.name] = input.value;
                    console.log('Captured hidden field (as string):', input.name, '=', input.value);
                }
            }
        });

        // Radio buttons
        form.querySelectorAll('input[type="radio"]:checked').forEach(radio => {
            if (radio.name) {
                this.stepData[radio.name] = radio.value;
                console.log('Captured radio:', radio.name, '=', radio.value);
            }
        });

        // Checkboxes
        const checkboxGroups = {};
        form.querySelectorAll('input[type="checkbox"]:checked').forEach(checkbox => {
            if (checkbox.name) {
                if (!checkboxGroups[checkbox.name]) {
                    checkboxGroups[checkbox.name] = [];
                }
                checkboxGroups[checkbox.name].push(checkbox.value);
            }
        });
        
        Object.keys(checkboxGroups).forEach(name => {
            this.stepData[name] = checkboxGroups[name];
            console.log('Captured checkbox group:', name, '=', checkboxGroups[name]);
        });

        console.log('Current stepData after capture:', this.stepData);
    }

    handleFieldChange(e) {
        const fieldId = e.target.name;
        const fieldType = e.target.type;
        
        console.log('Field changed:', fieldId, 'Type:', fieldType, 'Value:', e.target.value);
        
        if (fieldType === 'checkbox') {
            const checkboxGroup = document.querySelector(`[data-field-id="${fieldId}"]`);
            const checkedBoxes = checkboxGroup.querySelectorAll('input:checked');
            this.stepData[fieldId] = Array.from(checkedBoxes).map(cb => cb.value);
            
            checkboxGroup.querySelectorAll('.checkbox-option').forEach(option => {
                const input = option.querySelector('input');
                option.classList.toggle('selected', input.checked);
            });
        } else if (fieldType === 'radio') {
            this.stepData[fieldId] = e.target.value;
            console.log('Radio selected:', fieldId, '=', e.target.value);
            console.log('Current stepData:', this.stepData);
            
            const radioGroup = document.querySelector(`[data-field-id="${fieldId}"]`);
            if (radioGroup) {
                radioGroup.querySelectorAll('.radio-option').forEach(option => {
                    const input = option.querySelector('input');
                    option.classList.toggle('selected', input.checked);
                });
            }
        } else if (fieldType === 'hidden' && e.target.id.includes('skills')) {
            // Handle skills autocomplete hidden input
            try {
                this.stepData[fieldId] = JSON.parse(e.target.value);
            } catch (error) {
                console.error('Error parsing skills data:', error);
                this.stepData[fieldId] = [];
            }
        } else if (fieldType === 'hidden' && (e.target.id.includes('education') || e.target.id === 'highestEducation')) {
            // Handle education autocomplete hidden input
            this.stepData[fieldId] = e.target.value;
        } else {
            this.stepData[fieldId] = e.target.value;
        }

        this.clearFieldError(fieldId);
    }

    validateCurrentStep() {
        const step = this.workflowDefinition.steps[this.currentStepIndex];
        let isValid = true;

        console.log('Validating step:', step.stepId);
        console.log('Current stepData:', this.stepData);

        // Clear all existing errors
        document.querySelectorAll('.error-message').forEach(el => el.textContent = '');

        // Validate regular fields
        step.fields.forEach(field => {
            console.log('Validating field:', field.fieldId, 'Value:', this.stepData[field.fieldId]);
            if (!this.validateField(field)) {
                isValid = false;
            }
        });

        return isValid;
    }

    validateField(field) {
        const value = this.stepData[field.fieldId];
        
        // Required field validation
        if (field.required) {
            if (field.fieldType === 'skills-autocomplete') {
                // For skills, check if array exists and has at least one skill
                if (!value || !Array.isArray(value) || value.length === 0) {
                    this.showFieldError(field.fieldId, `${field.fieldName} is required - please select at least one skill`);
                    return false;
                }
            } else if (!value || (typeof value === 'string' && value.trim() === '')) {
                this.showFieldError(field.fieldId, `${field.fieldName} is required`);
                return false;
            }
        }

        return true;
    }

    showFieldError(fieldId, message) {
        const errorElement = document.getElementById(`${fieldId}-error`);
        if (errorElement) {
            errorElement.textContent = message;
        }
    }

    clearFieldError(fieldId) {
        const errorElement = document.getElementById(`${fieldId}-error`);
        if (errorElement) {
            errorElement.textContent = '';
        }
    }

    updateNavigationButtons() {
        const prevBtn = document.getElementById('prev-btn');
        const nextBtn = document.getElementById('next-btn');
        const submitBtn = document.getElementById('submit-btn');

        prevBtn.disabled = this.currentStepIndex === 0;
        
        const isLastStep = this.currentStepIndex === this.workflowDefinition.steps.length - 1;
        
        if (isLastStep) {
            nextBtn.style.display = 'none';
            submitBtn.style.display = 'block';
        } else {
            nextBtn.style.display = 'block';
            submitBtn.style.display = 'none';
        }
    }

    async nextStep() {
        // First capture all current form data
        this.captureCurrentFormData();
        
        if (!this.validateCurrentStep()) {
            console.log('Validation failed, not proceeding');
            return;
        }

        try {
            const currentStep = this.workflowDefinition.steps[this.currentStepIndex];
            const stepSubmissionData = {
                currentStep: currentStep.stepId,
                ...this.stepData
            };

            console.log('Submitting step data:', stepSubmissionData);

            const response = await fetch(`${this.baseUrl}/${this.applicationId}/step`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(stepSubmissionData)
            });

            const result = await response.json();
            console.log('Step submission response:', result);
            
            if (!response.ok) {
                throw new Error(result.message || 'Failed to submit step');
            }

            this.currentStepIndex++;
            this.renderCurrentStep();
            this.showMessage('Step completed successfully!', 'success');

        } catch (error) {
            console.error('Failed to submit step:', error);
            this.showMessage(error.message, 'error');
        }
    }

    previousStep() {
        if (this.currentStepIndex > 0) {
            this.currentStepIndex--;
            this.renderCurrentStep();
        }
    }

    async submitApplication() {
        // First capture all current form data
        this.captureCurrentFormData();
        
        if (!this.validateCurrentStep()) {
            console.log('Validation failed, not submitting');
            return;
        }

        try {
            const currentStep = this.workflowDefinition.steps[this.currentStepIndex];
            const stepSubmissionData = {
                currentStep: currentStep.stepId,
                ...this.stepData
            };

            console.log('Submitting final application data:', stepSubmissionData);

            const response = await fetch(`${this.baseUrl}/${this.applicationId}/step`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(stepSubmissionData)
            });

            const result = await response.json();
            console.log('Final submission response:', result);
            
            if (!response.ok) {
                throw new Error(result.message || 'Failed to submit application');
            }

            console.log('Application submitted successfully:', result);
            this.showMessage(`Application submitted successfully! Your application ID is: ${this.applicationId}`, 'success');
            
            // Disable all form elements
            const stepForm = document.getElementById('step-form');
            if (stepForm) {
                stepForm.style.pointerEvents = 'none';
            }
            
            const submitBtn = document.getElementById('submit-btn');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Application Submitted';
            }
            
            // Show final application ID prominently
            const container = document.getElementById('current-step');
            container.innerHTML = `
                <div class="step-header">
                    <h2 class="step-title">Application Submitted Successfully!</h2>
                    <p class="step-description">Thank you for your application.</p>
                </div>
                <div style="background: #e8f5e8; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0;">
                    <h3>Your Application ID:</h3>
                    <div style="font-size: 1.5em; font-weight: bold; color: #4CAF50; margin: 10px 0;">
                        ${this.applicationId}
                    </div>
                    <p>Please save this ID for future reference.</p>
                </div>
                <div style="background: #f0f0f0; padding: 15px; border-radius: 8px;">
                    <h4>Next Steps:</h4>
                    <ul style="text-align: left; margin: 10px 0;">
                        <li>You will receive a confirmation email shortly</li>
                        <li>Our HR team will review your application</li>
                        <li>We will contact you within 5-7 business days</li>
                    </ul>
                </div>
            `;

        } catch (error) {
            console.error('Failed to submit application:', error);
            this.showMessage(error.message, 'error');
        }
    }

    showMessage(message, type = 'info') {
        const container = document.getElementById('message-container');
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.textContent = message;
        
        container.appendChild(messageDiv);
        
        if (type === 'success') {
            setTimeout(() => {
                if (messageDiv.parentNode) {
                    messageDiv.parentNode.removeChild(messageDiv);
                }
            }, 5000);
        }
    }

    // Debug function to check current state
    debugCurrentState() {
        console.log('=== DEBUG INFO ===');
        console.log('Application ID:', this.applicationId);
        console.log('Current Step Index:', this.currentStepIndex);
        console.log('Current Step:', this.workflowDefinition?.steps[this.currentStepIndex]?.stepId);
        console.log('Step Data:', this.stepData);
        console.log('Form Values:');
        
        const form = document.getElementById('step-form');
        if (form) {
            const formData = new FormData(form);
            for (let [key, value] of formData.entries()) {
                console.log(`  ${key}: ${value}`);
            }
        }
        
        console.log('==================');
        return this.stepData;
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    console.log('Simple Workflow - Starting...');
    
    const workflowManager = new SimpleWorkflowManager();
    window.workflowManager = workflowManager;
    
    workflowManager.initialize().catch(error => {
        console.error('Failed to initialize workflow:', error);
    });
    
    // Setup navigation buttons
    const prevBtn = document.getElementById('prev-btn');
    const nextBtn = document.getElementById('next-btn');
    const submitBtn = document.getElementById('submit-btn');
    
    if (prevBtn) prevBtn.addEventListener('click', () => workflowManager.previousStep());
    if (nextBtn) nextBtn.addEventListener('click', () => workflowManager.nextStep());
    if (submitBtn) submitBtn.addEventListener('click', () => {
        if (confirm('Are you sure you want to submit your application? You will not be able to make changes after submission.')) {
            workflowManager.submitApplication();
        }
    });
    
    console.log('Simple Workflow - Initialized');
});