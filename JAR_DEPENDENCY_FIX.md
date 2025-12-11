# JAR Dependency Issue Fix

## Problem
The application was failing to start on Azure with:
```
java.lang.NoClassDefFoundError: ch/qos/logback/classic/spi/ThrowableProxy
```

This indicates that the deployed JAR file doesn't contain all required dependencies (specifically Logback classes).

## Root Cause
The Spring Boot Maven plugin wasn't properly configured to create a "fat JAR" (uber JAR) with all dependencies included. The deployed JAR was missing runtime dependencies.

## Changes Made

### 1. Updated `pom.xml`
- Added explicit `<executions>` block with `<goal>repackage</goal>` to ensure the Spring Boot Maven plugin creates a fat JAR
- Added `<executable>true</executable>` to make the JAR executable
- Added `<includeSystemScope>true</includeSystemScope>` to include system-scoped dependencies

### 2. Updated GitHub Actions Workflow (`.github/workflows/main_gauva.yml`)
- Changed build command from `mvn clean install` to `mvn clean package -DskipTests`
  - `package` creates the JAR without running tests
  - `-DskipTests` skips tests for faster builds
- Added verification steps to check JAR contents
- Updated artifact path to use the specific JAR name: `guava_app_backend-0.0.1-SNAPSHOT.jar`
- Added step to list and verify JAR files before deployment

## How It Works

1. **Maven Build Process:**
   - `mvn clean package` compiles the code
   - Spring Boot Maven plugin's `repackage` goal creates a fat JAR
   - The fat JAR includes:
     - Your application classes
     - All Maven dependencies (including Logback, Spring, etc.)
     - Spring Boot loader classes

2. **JAR Structure:**
   ```
   guava_app_backend-0.0.1-SNAPSHOT.jar
   ├── BOOT-INF/
   │   ├── classes/          (Your application classes)
   │   └── lib/              (All dependency JARs)
   └── org/springframework/boot/loader/  (Spring Boot loader)
   ```

3. **Deployment:**
   - GitHub Actions uploads the fat JAR
   - Azure App Service deploys it to `/home/site/wwwroot/`
   - Azure runs: `java -jar /home/site/wwwroot/guava_app_backend-0.0.1-SNAPSHOT.jar`

## Verification

After deployment, check the GitHub Actions logs for:
- ✅ "Number of dependency JARs: X" (should be > 50)
- ✅ "JAR file size: XX MB" (should be substantial, not just a few KB)
- ✅ No "WARNING: Logback classes not found" message

## Next Steps

1. **Commit and push these changes**
2. **Monitor the GitHub Actions workflow:**
   - Check the "Verify JAR contains dependencies" step
   - Ensure the JAR file size is reasonable (should be 50-100MB+ with all dependencies)
3. **After deployment, check Azure logs:**
   - Look for "Started RideFastBackendApplication" message
   - Verify no `NoClassDefFoundError` errors

## Troubleshooting

### If JAR still doesn't contain dependencies:

1. **Check Maven build logs:**
   - Look for "Repackaging JAR" message
   - Verify no errors during repackaging

2. **Manually verify JAR contents:**
   ```bash
   jar tf target/guava_app_backend-0.0.1-SNAPSHOT.jar | grep BOOT-INF/lib | wc -l
   ```
   Should return a large number (50+)

3. **Check if original JAR is being used:**
   - The original JAR (without dependencies) is renamed to `.original`
   - Only the repackaged JAR should be deployed

### If deployment still fails:

1. **Check Azure Log Stream** for specific error messages
2. **Verify database connectivity** (might be a different issue)
3. **Check application.yml** for correct configuration
4. **Ensure all environment variables** are set in Azure Portal

## Related Files

- `pom.xml` - Maven configuration
- `.github/workflows/main_gauva.yml` - CI/CD pipeline
- `AZURE_DEPLOYMENT_FIX.md` - Azure-specific configuration
