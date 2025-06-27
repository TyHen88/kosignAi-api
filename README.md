# 🤖 PPC Bank AI ChatBot API

An intelligent chatbot system that scrapes PPC Bank website data and uses AI to answer user questions based on the collected information.

## 🚀 Features

- **Web Scraping**: Automatically scrapes PPC Bank website content
- **AI Integration**: Uses OpenAI GPT models to answer user questions
- **Database Storage**: Stores scraped content in PostgreSQL
- **Smart Search**: Finds relevant information based on user queries
- **REST API**: Provides endpoints for chat interactions
- **Web Interface**: Beautiful chat interface for testing
- **Scheduled Updates**: Automatically updates scraped content

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Scraper   │    │   PostgreSQL    │    │   OpenAI API    │
│   (Python)      │───▶│   Database      │◀───│   (GPT Models)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                Spring Boot Application                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   Batch     │  │   AI        │  │   Chat      │            │
│  │ Scheduler   │  │  Service    │  │ Controller  │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
                   ┌─────────────────┐
                   │  Chat Web UI    │
                   │   (HTML/JS)     │
                   └─────────────────┘
```

## 📋 Prerequisites

- **Java 17** or higher
- **Python 3.8** or higher
- **PostgreSQL 12** or higher
- **OpenAI API Key** (from https://platform.openai.com/api-keys)

## 🛠️ Installation & Setup

### 1. Clone and Setup Environment

```bash
git clone <repository-url>
cd chatbotai-api
```

### 2. Setup PostgreSQL Database

Create a PostgreSQL database named `web_scraper` running on port `5433`:

```sql
CREATE DATABASE web_scraper;
CREATE USER postgres WITH PASSWORD '12345678';
GRANT ALL PRIVILEGES ON DATABASE web_scraper TO postgres;
```

### 3. Setup OpenAI API Key

Run the environment setup script:

```bash
# Windows
setup-env.bat

# Or set manually
set OPENAI_API_KEY=your-actual-openai-api-key-here
```

### 4. Run the Application

```bash
# Windows - Complete setup and run
run-app.bat

# Or manual steps:
setup-python.bat
.\gradlew.bat clean build
.\gradlew.bat bootRun
```

## 🎯 Usage

### Web Interface

1. Start the application
2. Open your browser to: `http://localhost:8080/chat.html`
3. Start chatting with the AI assistant!

### API Endpoints

#### Chat with AI
```http
POST /api/chat/query
Content-Type: application/json

{
  "query": "What services does PPC Bank offer?"
}
```

#### Get Database Statistics
```http
GET /api/chat/stats
```

#### Health Check
```http
GET /api/chat/health
```

#### Manual Scraper Trigger
```http
POST /api/scraper/run
```

### Example Questions

- "What banking services does PPC Bank offer?"
- "How can I open an account?"
- "What are the loan options available?"
- "Tell me about PPC Bank's digital banking"
- "What are the interest rates?"

## ⚙️ Configuration

### Application Configuration (`application.yml`)

```yaml
# Database Configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/web_scraper
    username: postgres
    password: 12345678

# AI Configuration
ai:
  openai:
    api-key: ${OPENAI_API_KEY}
    model: gpt-3.5-turbo
    max-tokens: 1000
    temperature: 0.7

# Scraper Schedule (daily at 2:30 AM)
scraper:
  schedule:
    cron: "0 30 2 * * ?"
```

### Python Scraper Configuration

Environment variables passed to Python scraper:
- `BASE_URL`: Website to scrape
- `MAX_DEPTH`: Maximum crawl depth
- `MAX_CONTENT_LENGTH`: Content truncation limit
- `CRAWL_DELAY`: Delay between requests

## 🗃️ Database Schema

### Tables

- **`pages`**: Stores scraped web page content
- **`page_versions`**: Keeps version history of pages
- **`broken_links`**: Tracks failed URLs
- **Spring Batch tables**: For job scheduling and monitoring

## 🔄 Scheduling

The scraper runs automatically based on the cron schedule:
- **Development**: Every 30 seconds (for testing)
- **Production**: Daily at 2:30 AM

To change the schedule, modify the `scraper.schedule.cron` property in `application.yml`.

## 🧪 Testing

### Test the Chat API

```bash
# Test health endpoint
curl http://localhost:8080/api/chat/health

# Test chat query
curl -X POST http://localhost:8080/api/chat/query \
  -H "Content-Type: application/json" \
  -d '{"query": "What services does PPC Bank offer?"}'

# Get database stats
curl http://localhost:8080/api/chat/stats
```

### Test the Scraper

```bash
# Trigger manual scraping
curl -X POST http://localhost:8080/api/scraper/run
```

## 🚨 Troubleshooting

### Common Issues

#### 1. OpenAI API Key Not Working
```bash
# Check if environment variable is set
echo %OPENAI_API_KEY%

# Re-run setup if needed
setup-env.bat
```

#### 2. Database Connection Issues
- Verify PostgreSQL is running on port 5433
- Check username/password in `application.yml`
- Ensure database `web_scraper` exists

#### 3. Python Dependencies Missing
```bash
# Reinstall Python dependencies
pip install -r src/main/resources/scraper-script/requirements.txt
```

#### 4. Port Already in Use
- Change the port in `application.yml`:
```yaml
server:
  port: 8081  # or any available port
```

## 📊 Monitoring

### Logs

- **Application logs**: Check console output for Spring Boot logs
- **AI Service logs**: Debug level logs for AI processing
- **Scraper logs**: Python script output in application logs

### Database Monitoring

```sql
-- Check scraped pages count
SELECT COUNT(*) FROM pages;

-- Check recent scraping activity
SELECT url, title, updated_at FROM pages 
ORDER BY updated_at DESC LIMIT 10;

-- Check broken links
SELECT url, error, created_at FROM broken_links 
ORDER BY created_at DESC LIMIT 10;
```

## 🔧 Development

### Adding New Features

1. **New Chat Commands**: Extend `AIService.java`
2. **Additional Scrapers**: Modify `web_scraper.py`
3. **Custom Endpoints**: Add controllers in `controller/` package
4. **Database Changes**: Update entities in `domains/` package

### Project Structure

```
src/
├── main/
│   ├── java/org/kosign/chatbotapi/
│   │   ├── controller/          # REST controllers
│   │   ├── service/             # Business logic
│   │   ├── repository/          # Data access
│   │   ├── domains/             # JPA entities
│   │   ├── batch/               # Spring Batch components
│   │   └── config/              # Configuration classes
│   └── resources/
│       ├── scraper-script/      # Python scraper
│       ├── static/              # Web interface
│       ├── application.yml      # Configuration
│       └── schema.sql           # Database schema
```

## 📄 License

This project is licensed under the MIT License.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📞 Support

For issues and questions:
1. Check the troubleshooting section
2. Review application logs
3. Create an issue in the repository

---

**Happy Chatting! 🤖💬** 