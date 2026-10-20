#!/usr/bin/env bash
# 仅用于空数据库首次初始化。使用前先把部署包解压到 /opt/vps/source。
set -euo pipefail
if mysql -NBe "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='vps_stock'" | grep -q vps_stock; then
  echo 'vps_stock already exists; refusing to overwrite existing data.' >&2
  exit 1
fi
install -d -m 750 /opt/vps/config /opt/vps/logs /opt/vps/uploads
id vps >/dev/null 2>&1 || useradd --system --home /opt/vps --shell /usr/sbin/nologin vps
python3 - <<'PY'
import secrets,json,os,bcrypt,base64
from pathlib import Path
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives import serialization
key=ec.generate_private_key(ec.SECP256R1())
b64=lambda b:base64.urlsafe_b64encode(b).decode().rstrip('=')
values={'VPS_DB_PASSWORD':secrets.token_hex(24),'VPS_REDIS_PASSWORD':secrets.token_hex(24),'VPS_ENCRYPT_PASSWORD':secrets.token_hex(16),'VPS_PUBLIC_URL':'http://68.68.249.248','VPS_PUSH_PUBLIC_KEY':b64(key.public_key().public_bytes(serialization.Encoding.X962,serialization.PublicFormat.UncompressedPoint)),'VPS_PUSH_PRIVATE_KEY':b64(key.private_numbers().private_value.to_bytes(32,'big'))}
Path('/etc/vps-stock.env').write_text('\n'.join(k+'='+v for k,v in values.items())+'\n')
os.chmod('/etc/vps-stock.env',0o600)
password=secrets.token_urlsafe(12)
Path('/etc/vps-stock-access.json').write_text(json.dumps({'username':'admin','password':password,'publicUrl':values['VPS_PUBLIC_URL'],'adminUrl':values['VPS_PUBLIC_URL']+'/admin/'},ensure_ascii=False))
os.chmod('/etc/vps-stock-access.json',0o600)
Path('/tmp/vps-init.sql').write_text("CREATE DATABASE vps_stock CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\nCREATE USER 'vps_app'@'127.0.0.1' IDENTIFIED BY '"+values['VPS_DB_PASSWORD']+"';\nGRANT SELECT, INSERT, UPDATE, DELETE ON vps_stock.* TO 'vps_app'@'127.0.0.1';\n")
os.chmod('/tmp/vps-init.sql',0o600)
Path('/tmp/vps-password.sql').write_text("UPDATE system_users SET password='"+bcrypt.hashpw(password.encode(),bcrypt.gensalt(12)).decode()+"' WHERE id=1;")
os.chmod('/tmp/vps-password.sql',0o600)
with open('/etc/redis/redis.conf','a') as f:f.write('\n# VPS stock service\nrequirepass '+values['VPS_REDIS_PASSWORD']+'\nmaxmemory 128mb\nmaxmemory-policy noeviction\nappendonly yes\n')
PY
mysql < /tmp/vps-init.sql
mysql vps_stock < /opt/vps/source/sql/mysql/bootstrap.sql
mysql vps_stock < /opt/vps/source/sql/mysql/vps.sql
mysql vps_stock < /opt/vps/source/sql/mysql/vps-region.sql
mysql vps_stock < /opt/vps/source/sql/mysql/vps-seed.sql
mysql vps_stock < /opt/vps/source/sql/mysql/vps-region-seed.sql
mysql vps_stock < /opt/vps/source/sql/mysql/vps-extended-seed.sql
mysql vps_stock < /tmp/vps-password.sql
rm -f /tmp/vps-init.sql /tmp/vps-password.sql
cp /opt/vps/source/deploy/application-prod.yaml /opt/vps/config/
chown root:vps /opt/vps/config
cp /opt/vps/source/deploy/vps-stock.service /etc/systemd/system/
chown -R vps:vps /opt/vps/logs /opt/vps/uploads
systemctl restart redis-server
systemctl daemon-reload
systemctl enable vps-stock
printf 'Database and service configuration initialized.\n'
