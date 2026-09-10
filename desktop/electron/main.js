/**
 * Electron Main Process
 * RestaurantPOS Desktop Application
 *
 * Architecture:
 * - Electron loads Angular frontend from local files
 * - Angular connects to local Spring Boot API (127.0.0.1:8080)
 * - Spring Boot connects to local PostgreSQL
 */

const { app, BrowserWindow, ipcMain, shell, dialog, Menu } = require('electron');
const path = require('path');
const { autoUpdater } = require('electron-updater');
const { spawn } = require('child_process');
const fs = require('fs');

// ============================================================
// Configuration
// ============================================================
const isDev = !app.isPackaged;
const BACKEND_URL = 'http://127.0.0.1:8080';
const BACKEND_STARTUP_TIMEOUT = 60000; // 60 seconds
const APP_DATA = path.join(process.env.PROGRAMDATA || 'C:\\ProgramData', 'RestaurantPOS');

let mainWindow = null;
let backendProcess = null;
let backendReady = false;

// ============================================================
// Window Management
// ============================================================
function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 1024,
    minHeight: 600,
    title: 'RestaurantPOS',
    icon: path.join(__dirname, 'assets', 'icon.ico'),
    backgroundColor: '#0f1117',
    show: false, // Show after ready
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      webSecurity: !isDev,
    }
  });

  // Remove default menu in production
  if (!isDev) {
    Menu.setApplicationMenu(null);
  }

  // Load Angular app
  if (isDev) {
    mainWindow.loadURL('http://localhost:4200');
    mainWindow.webContents.openDevTools();
  } else {
    const indexPath = path.join(process.resourcesPath, 'app', 'index.html');
    mainWindow.loadFile(indexPath);
  }

  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    mainWindow.maximize();
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });

  // Open external links in browser
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });
}

// ============================================================
// Backend Management
// ============================================================
function startBackend() {
  const jarPath = findBackendJar();
  if (!jarPath) {
    console.log('Backend JAR not found, assuming it is running separately');
    backendReady = true;
    return;
  }

  console.log('Starting Spring Boot backend:', jarPath);

  const configFile = path.join(APP_DATA, 'config', 'application.properties');
  const javaArgs = [
    '-jar', jarPath,
    '-Dspring.config.additional-location=file:' + configFile,
    '-Dserver.port=8080'
  ];

  backendProcess = spawn('java', javaArgs, {
    cwd: path.dirname(jarPath),
    env: { ...process.env },
    stdio: 'pipe'
  });

  backendProcess.stdout.on('data', (data) => {
    const text = data.toString();
    if (text.includes('Started RestaurantPosApplication')) {
      backendReady = true;
      console.log('Backend started successfully');
      if (mainWindow) {
        mainWindow.webContents.send('backend-ready');
      }
    }
    console.log('[Backend]', text.trim());
  });

  backendProcess.stderr.on('data', (data) => {
    console.error('[Backend Error]', data.toString().trim());
  });

  backendProcess.on('exit', (code) => {
    console.log('Backend process exited with code:', code);
    backendReady = false;
  });
}

function findBackendJar() {
  const locations = [
    path.join(process.resourcesPath, 'backend'),
    path.join(__dirname, '..', '..', 'backend', 'restaurant-pos-api', 'target')
  ];

  for (const dir of locations) {
    if (fs.existsSync(dir)) {
      const files = fs.readdirSync(dir).filter(f => f.endsWith('.jar') && !f.includes('-sources'));
      if (files.length > 0) {
        return path.join(dir, files[0]);
      }
    }
  }
  return null;
}

function stopBackend() {
  if (backendProcess) {
    backendProcess.kill('SIGTERM');
    backendProcess = null;
  }
}

// ============================================================
// IPC Handlers
// ============================================================
ipcMain.handle('get-app-version', () => app.getVersion());
ipcMain.handle('get-app-data-path', () => APP_DATA);
ipcMain.handle('is-backend-ready', () => backendReady);
ipcMain.handle('get-backend-url', () => BACKEND_URL);

ipcMain.handle('check-for-updates', async () => {
  if (!isDev) {
    autoUpdater.checkForUpdatesAndNotify();
  }
});

ipcMain.handle('open-logs-folder', () => {
  shell.openPath(path.join(APP_DATA, 'logs'));
});

ipcMain.handle('open-backup-folder', () => {
  shell.openPath(path.join(APP_DATA, 'backups'));
});

ipcMain.handle('show-message-box', async (event, options) => {
  return dialog.showMessageBox(mainWindow, options);
});

// ============================================================
// App Lifecycle
// ============================================================
app.whenReady().then(() => {
  // Ensure app data directories exist
  ensureAppDirectories();

  // Start backend
  if (!isDev) {
    startBackend();
  } else {
    backendReady = true;
  }

  createWindow();

  // macOS: re-create window when dock icon clicked
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  stopBackend();
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('before-quit', () => {
  stopBackend();
});

// ============================================================
// Utilities
// ============================================================
function ensureAppDirectories() {
  const dirs = [
    APP_DATA,
    path.join(APP_DATA, 'logs'),
    path.join(APP_DATA, 'backups'),
    path.join(APP_DATA, 'config'),
    path.join(APP_DATA, 'PostgreSQL')
  ];

  dirs.forEach(dir => {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
  });
}

// ============================================================
// Auto-updater events
// ============================================================
autoUpdater.on('update-available', () => {
  if (mainWindow) {
    mainWindow.webContents.send('update-available');
  }
});

autoUpdater.on('update-downloaded', () => {
  if (mainWindow) {
    mainWindow.webContents.send('update-downloaded');
  }
});
