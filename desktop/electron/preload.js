/**
 * Electron Preload Script
 * Safely exposes backend/system APIs to the Angular renderer
 */
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  // App info
  getVersion: () => ipcRenderer.invoke('get-app-version'),
  getAppDataPath: () => ipcRenderer.invoke('get-app-data-path'),
  getBackendUrl: () => ipcRenderer.invoke('get-backend-url'),
  isBackendReady: () => ipcRenderer.invoke('is-backend-ready'),

  // System
  openLogsFolder: () => ipcRenderer.invoke('open-logs-folder'),
  openBackupFolder: () => ipcRenderer.invoke('open-backup-folder'),
  showMessageBox: (options) => ipcRenderer.invoke('show-message-box', options),

  // Updates
  checkForUpdates: () => ipcRenderer.invoke('check-for-updates'),

  // Events
  onBackendReady: (callback) => ipcRenderer.on('backend-ready', callback),
  onUpdateAvailable: (callback) => ipcRenderer.on('update-available', callback),
  onUpdateDownloaded: (callback) => ipcRenderer.on('update-downloaded', callback),

  // Remove listeners
  removeAllListeners: (channel) => ipcRenderer.removeAllListeners(channel)
});
