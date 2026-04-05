#!/bin/bash
echo "=== Testing root with password ==="
mysql -u root -pPostgres1 -e "SELECT 'connected' AS status;" 2>&1 || echo "PASSWORD AUTH FAILED"

echo "=== Testing root without password (unix_socket) ==="
mysql -u root -e "SELECT user,plugin FROM mysql.user WHERE user='root';" 2>&1

echo "=== Applying fix ==="
mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED VIA mysql_native_password USING PASSWORD('Postgres1'); FLUSH PRIVILEGES;" 2>&1

echo "=== Testing root with password after fix ==="
mysql -u root -pPostgres1 -e "SELECT 'connected after fix' AS status;" 2>&1

echo "=== Restarting AssetMind ==="
systemctl restart assetmind
echo "Done"
