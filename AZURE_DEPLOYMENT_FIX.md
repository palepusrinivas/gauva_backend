# Azure App Service Deployment Fix

## Problem
The container is timing out because:
1. The application is not using the `PORT` environment variable (Azure provides this)
2. Database connection timeout is too long (60 seconds)
3. No explicit startup command configured

## Changes Made

### 1. Port Configuration
Updated `application.yml` to use Azure's `PORT` environment variable:
```yaml
server:
  port: ${PORT:8080}  # Uses PORT env var for Azure (defaults to 8080 for local)
```

### 2. Reduced Database Connection Timeout
- Reduced connection timeout from 60s to 30s
- Added `initialization-fail-timeout: 10000` to fail fast if database is unreachable
- Updated JDBC URL timeouts to 30 seconds

### 3. Startup Script
Created `.azure/startup.sh` for explicit startup command (optional, Azure auto-detects JARs)

## Required Azure App Service Configuration

### Step 1: Configure Application Settings
In Azure Portal → Your App Service → Configuration → Application settings, ensure:

1. **PORT** (Azure sets this automatically, but verify it exists)
2. **Database Connection Settings:**
   - `DB_HOST` - Your PostgreSQL host
   - `DB_PORT` - PostgreSQL port (usually 5432)
   - `DB_NAME` - Database name
   - `DB_USERNAME` - Database username
   - `DB_PASSWORD` - Database password

3. **Firebase Settings:**
   - `FIREBASE_PROJECT_ID` - Your Firebase project ID
   - `FIREBASE_STORAGE_BUCKET` - Your Firebase storage bucket
   - `FIREBASE_CREDENTIALS_B64` - Base64 encoded Firebase service account JSON

4. **Other Required Settings:**
   - `REDIS_HOST` - Redis host
   - `REDIS_PORT` - Redis port
   - `REDIS_PASSWORD` - Redis password (if required)
   - `APP_FRONTEND_URL` - Frontend URL for CORS

### Step 2: Configure Startup Command (Optional)
If Azure doesn't auto-detect the JAR, set in Configuration → General settings:

**Startup Command:**
```bash
java -jar /home/site/wwwroot/*.jar
```

Or use the startup script:
```bash
bash /home/site/wwwroot/.azure/startup.sh
```

### Step 3: Increase Startup Timeout (If Needed)
In Configuration → General settings:
- **Always On**: Enable this to prevent cold starts
- **Startup Timeout**: Increase to 300 seconds if needed (default is 230s)

### Step 4: Health Check Configuration
The application has health check endpoints configured:
- Health endpoint: `/actuator/health`
- Configure Azure to use this for health checks

## Verification Steps

1. **Check Logs:**
   - Go to Azure Portal → Your App Service → Log stream
   - Look for "Started RideFastBackendApplication" message (this confirms successful startup)
   - **Normal messages you'll see (not errors):**
     - `Picked up JAVA_TOOL_OPTIONS: -javaagent:/agents/java/applicationinsights-agent-codeless.jar` ✅ Normal
     - `OpenJDK 64-Bit Server VM warning: Sharing is only supported...` ✅ Harmless warning
   - **What to look for:**
     - `Started RideFastBackendApplication` ✅ Success!
     - `Tomcat started on port(s):` ✅ App is listening
     - Database connection errors ❌ Check connection settings
     - `Exception` or `Error` messages ❌ Investigate these

2. **Test Health Endpoint:**
   ```bash
   curl https://your-app-name.azurewebsites.net/actuator/health
   ```
   Should return: `{"status":"UP"}` or similar

3. **Check Application Status:**
   - Go to Azure Portal → Your App Service → Overview
   - Verify the app is running (not stopped/error)
   - Check the status badge (should be green "Running")

## Troubleshooting

### If Container Still Times Out:

1. **Check Database Connectivity:**
   - Ensure PostgreSQL is accessible from Azure App Service
   - Check firewall rules allow Azure IPs
   - Verify connection string is correct

2. **Check Application Logs:**
   - Go to Log stream in Azure Portal
   - Look for specific error messages
   - Check if database connection is the issue

3. **Reduce Startup Dependencies:**
   - Consider lazy initialization if database is slow
   - Disable non-critical startup tasks

4. **Increase Container Resources:**
   - Upgrade to a higher App Service Plan
   - More CPU/RAM can speed up startup

### Common Issues:

1. **"Could not find executable jar"**
   - Ensure JAR is deployed to `/home/site/wwwroot/`
   - Check GitHub Actions deployment is successful

2. **"Connection timeout"**
   - Verify database is accessible
   - Check network security groups
   - Ensure database allows Azure IPs

3. **"Port already in use"**
   - Azure sets PORT automatically, don't override it
   - Ensure application uses `${PORT}` variable

## Next Steps

1. Commit and push these changes
2. Wait for GitHub Actions to deploy
3. Monitor Azure Log stream for startup
4. Verify health endpoint responds
5. Test API endpoints
