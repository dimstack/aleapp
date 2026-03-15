# aleapp

CallApp backend and Android client.

## Install backend

Run the installer directly from GitHub:

```bash
curl -fsSL https://raw.githubusercontent.com/dimstack/aleapp/main/server/install.sh | bash
```

The script installs Docker if needed, deploys the backend, and on repeated runs lets you either:

- update containers to the latest version with data preserved
- fully reinstall the server after explicit confirmation
