#!/bin/bash

# Quick debug script to check deployment status

TOMCAT_HOME="/Users/nikolaiuteshev/Desktop/tools/apache-tomcat-10.1.43"

echo "======================================"
echo "Deployment Status Check"
echo "======================================"
echo ""

echo "1. Checking if WAR file exists..."
if [ -f "target/ROOT.war" ]; then
    echo "   ✅ WAR file exists: $(ls -lh target/ROOT.war | awk '{print $5}')"
else
    echo "   ❌ WAR file NOT found at target/ROOT.war"
fi
echo ""

echo "2. Checking if Tomcat is running..."
if ps aux | grep -v grep | grep tomcat > /dev/null; then
    echo "   ✅ Tomcat is running"
    ps aux | grep -v grep | grep tomcat | head -1
else
    echo "   ❌ Tomcat is NOT running"
fi
echo ""

echo "3. Checking deployed WAR in Tomcat..."
if [ -f "$TOMCAT_HOME/webapps/ROOT.war" ]; then
    echo "   ✅ ROOT.war deployed: $(ls -lh $TOMCAT_HOME/webapps/ROOT.war | awk '{print $5}')"
else
    echo "   ❌ ROOT.war NOT found in webapps"
fi
echo ""

echo "4. Checking if WAR was extracted..."
if [ -d "$TOMCAT_HOME/webapps/ROOT" ]; then
    echo "   ✅ ROOT directory exists (WAR was extracted)"
    if [ -d "$TOMCAT_HOME/webapps/ROOT/WEB-INF" ]; then
        echo "   ✅ WEB-INF directory found"
    else
        echo "   ⚠️  WEB-INF directory NOT found"
    fi
else
    echo "   ❌ ROOT directory NOT found (WAR not extracted yet)"
fi
echo ""

echo "5. Checking PostgreSQL..."
if ps aux | grep -v grep | grep postgres > /dev/null; then
    echo "   ✅ PostgreSQL is running"
else
    echo "   ⚠️  PostgreSQL might not be running"
fi
echo ""

echo "6. Testing endpoint..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/posts 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
    echo "   ✅ Endpoint responds with HTTP $HTTP_CODE"
elif [ "$HTTP_CODE" = "000" ]; then
    echo "   ❌ Cannot connect to Tomcat (connection refused)"
else
    echo "   ⚠️  Endpoint responds with HTTP $HTTP_CODE"
fi
echo ""

echo "7. Recent Tomcat logs (last 20 lines)..."
if [ -f "$TOMCAT_HOME/logs/catalina.out" ]; then
    echo "   Last errors/warnings:"
    grep -i "error\|exception\|warning" "$TOMCAT_HOME/logs/catalina.out" | tail -5
    echo ""
    echo "   Full log tail:"
    tail -10 "$TOMCAT_HOME/logs/catalina.out"
else
    echo "   ⚠️  catalina.out not found"
fi
echo ""

echo "======================================"
echo "Quick Actions:"
echo "======================================"
echo "View full logs:    tail -f $TOMCAT_HOME/logs/catalina.out"
echo "Test endpoint:     curl -v http://localhost:8080/api/posts"
echo "Stop Tomcat:       $TOMCAT_HOME/bin/shutdown.sh"
echo "Start Tomcat:      $TOMCAT_HOME/bin/startup.sh"
echo "Redeploy:          ./deploy.sh"

