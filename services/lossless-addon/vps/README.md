# Forge lossless add-on on a VPS

The same add-on as the Cloudflare Worker, run by Node.js on an Ubuntu
server with Caddy in front for HTTPS. Sign-ins are kept in a SQLite file
in `/var/lib/forge-addon`, and there is no daily request limit.

## Before installing

Point the domain at the server: an `A` record for
`forge-addons.invalid` with the VPS's IP address. If the domain is
on Cloudflare, set that record to **DNS only** (grey cloud), so Caddy can get
its certificate and the add-on sees each phone's own address.

In Hostinger's panel, make sure the VPS firewall lets in ports 80 and 443.

## Install

Copy the archive to the server and run the installer as root:

```sh
scp forge-addon-vps.tar.gz root@YOUR_VPS_IP:/root/
ssh root@YOUR_VPS_IP
tar -xzf forge-addon-vps.tar.gz
cd forge-addon
sudo bash install.sh
```

It installs Node.js 24 and Caddy if they're missing, puts the add-on in
`/opt/forge-addon`, starts it as the `forge-addon` service and asks
Caddy for the HTTPS certificate. At the end it prints the manifest URLs.

To update later, unpack a newer archive and run `sudo bash install.sh`
again. Settings and sign-ins stay.

## Settings

`/etc/forge-addon.env`. After a change:

```sh
sudo systemctl restart forge-addon
```

- `PAUSED=true` turns every search and stream away and tells apps to stay
  away for six hours.
- `APP_CERT_SHA256` is the official APK's signing certificate. Change it
  only if the app is ever signed with a different key.

## Day to day

```sh
journalctl -u forge-addon -f          # the add-on's log
systemctl status forge-addon          # running or not
tail -f /var/log/caddy/forge-addon.log  # every request, from Caddy
```

## Apps

Only app versions that trust `https://forge-addons.invalid` can
sign in to it; older ones were built for the workers.dev address. In the
owner dashboard, change the four add-on URLs to:

- `https://forge-addons.invalid/monochrome/manifest.json`
- `https://forge-addons.invalid/archive/manifest.json`
- `https://forge-addons.invalid/musopen/manifest.json`
- `https://forge-addons.invalid/netease/manifest.json`

## Owner dashboard and remote configuration

This package also includes `/control/`. Set `CONTROL_ADMIN_USERNAME` and `CONTROL_ADMIN_PASSWORD` (16+ characters) in `/etc/forge-addon.env`, restart the service, and open `https://forge-addons.invalid/control/`. Existing installations must add these two settings manually; the installer preserves the old environment file.

Back up `control.sqlite` alongside `auth.sqlite`; the configuration signing key must survive server migrations. See [the complete configuration guide](../../../docs/remote-configuration.md) for supported controls, publication, rollback and client compatibility.

## Complete Hostinger package and shared workers

`pack.sh` also creates `forge-hostinger.zip` and its SHA-256 checksum. It includes the add-on adapters, strict verification, owner controls, private usage dashboard/API, SQLite schema, installation script and migration tool. No credentials or live data are included.

Upload the ZIP to your KVM VPS, then run:

```sh
unzip forge-hostinger.zip
cd forge-addon
sudo bash install.sh
sudoedit /etc/forge-addon.env
sudo systemctl restart forge-addon
```

Keep your existing `PUBLIC_HOST` and certificate pin. Set `CONTROL_ADMIN_USERNAME` and a strong unique `CONTROL_ADMIN_PASSWORD`; these also unlock `/usage/` unless separate `USAGE_ADMIN_*` values are configured. `/control/` remains the content dashboard. The installer preserves an existing environment file and `/var/lib/forge-addon`, including its signing key and verified device sessions. Back up that directory and `/etc/forge-addon.env` before an upgrade. New domains require clients to enroll again; copy the stable signed remote-configuration keys when migrating an existing server.

`ADDON_WORKERS=1` suits KVM1. On a larger VPS, set 2–4 and restart. The parent routes work to the least busy healthy thread and retries a crashed worker once on another. A bounded shared metadata cache avoids repeated upstream calls. Authentication, nonce replay protection, rate limits, owner configuration and usage SQLite stay in one parent process, so they cannot disagree between workers. Worker threads are private: there are no unauthenticated worker ports. This distributes work on one VPS; it is not high availability across independent servers, and does not create extra upstream-service quotas. Never round-robin independently enrolled Cloudflare and VPS hosts: their verification sessions differ.

`USAGE_ENABLED=true` serves the usage API on this VPS. To move installed clients away from Cloudflare, change the website's `usage-config.json` `apiBase` to your existing HTTPS VPS origin **after** testing it and importing prior data. Clients cache that configuration for an hour; old apps can continue sending more often until updated. Keep the old endpoint available during migration. A DNS-only record avoids Worker invocations; don't route the new endpoint back through the old Worker. Audio files still stream directly from their providers.

### Preserve Cloudflare statistics

Export your D1 database before switching traffic (substitute the database name from `usage-api/wrangler.jsonc`):

```sh
npx wrangler d1 export DATABASE_NAME --remote --output usage-export.sql
sudo systemctl stop forge-addon
# If the installer already created a fresh empty database, back it up/move it aside first.
# Never discard or overwrite a database with reports you need to retain.
sudo node import-usage.mjs usage-export.sql /var/lib/forge-addon
sudo chown -R forge:forge /var/lib/forge-addon
sudo systemctl start forge-addon
```

The importer refuses to overwrite an existing database and checks SQLite integrity. Daily reports use cumulative maximums, so retries do not duplicate counts. Plan the final export/cutover together: reports accepted by the old endpoint after your export must also be migrated; this package does not silently merge divergent databases. Reporting counts are installations with consent, not unique people or proof of APK installation. Reinstalls/data clearing create a new identity.

### Verify after installation

```sh
sudo systemctl status forge-addon --no-pager
curl -f https://YOUR_DOMAIN/monochrome/manifest.json
curl -f https://YOUR_DOMAIN/v1/public
# An unauthenticated search must return HTTP 401:
curl -o /dev/null -w '%{http_code}\n' https://YOUR_DOMAIN/monochrome/search?q=test
sudo journalctl -u forge-addon -n 50 --no-pager
```

Then sign into `/control/` and `/usage/`, refresh each page, and test a song in the signed Forge app. Store backups off this VPS; a second thread does not protect against disk or server failure. Hostinger shared web hosting cannot run this systemd/Node service; use the KVM VPS.
