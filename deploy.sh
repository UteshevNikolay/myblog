#!/bin/bash

# Configuration
TOMCAT_HOME="/Users/nikolaiuteshev/Desktop/tools/apache-tomcat-10.1.43"  # Adjust to your Tomcat installation path
WAR_FILE="target/ROOT.war"
WEBAPPS_DIR="$TOMCAT_HOME/webapps"

# Step 1: Build the WAR file
echo "Building WAR file..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Maven build failed. Exiting."
    exit 1
fi

# Verify WAR file exists
if [ ! -f "$WAR_FILE" ]; then
    echo "ERROR: WAR file not found at $WAR_FILE"
    exit 1
fi
echo "WAR file built successfully: $WAR_FILE"

# Step 2: Stop Tomcat if it's running
echo "Stopping Tomcat if running..."
if [ -f "$TOMCAT_HOME/bin/shutdown.sh" ]; then
    $TOMCAT_HOME/bin/shutdown.sh 2>/dev/null || true
    sleep 5
    echo "Tomcat stopped"
fi

# Step 3: Clean old deployment
echo "Cleaning old ROOT deployment..."
rm -rf $WEBAPPS_DIR/ROOT
rm -f $WEBAPPS_DIR/ROOT.war

# Step 4: Copy new WAR to Tomcat webapps
echo "Copying $WAR_FILE to $WEBAPPS_DIR..."
cp $WAR_FILE $WEBAPPS_DIR/ROOT.war
if [ $? -ne 0 ]; then
    echo "Failed to copy WAR file. Exiting."
    exit 1
fi

# Step 5: Start Tomcat
echo "Starting Tomcat..."
$TOMCAT_HOME/bin/startup.sh
if [ $? -ne 0 ]; then
    echo "Failed to start Tomcat. Exiting."
    exit 1
fi

echo ""
echo "=========================================="
echo "Deployment successful!"
echo "=========================================="
echo "Application URL: http://localhost:8080/api/posts"
echo ""
echo "Wait ~10 seconds for application to start, then check logs:"
echo "  tail -f $TOMCAT_HOME/logs/catalina.out"
echo ""
echo "Test with: curl http://localhost:8080/api/posts"
