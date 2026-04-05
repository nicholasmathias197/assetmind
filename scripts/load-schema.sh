#!/bin/bash
# AssetMind uses Flyway for DB migrations — the app auto-migrates on startup.
# This script just ensures the database exists and restarts the app.

mysql -u root -pPostgres1 -e "CREATE DATABASE IF NOT EXISTS assetmind;"
echo "Database ensured"

# Re-download latest JAR from S3
aws s3 cp s3://assetmind-frontend-911784620581/app/assetmind-application.jar /opt/assetmind/assetmind-application.jar

systemctl restart assetmind
echo "Done"
