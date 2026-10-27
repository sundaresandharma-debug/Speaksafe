// SpeakSafe background service worker.
// Content scripts cannot call the Java backend directly (web pages block it),
// so they send messages here.
//
// Two modes:
//   1. Java server running on localhost:8080 -> it scores messages and stores reports in SQLite
//   2. No server -> the built-in model (same trained SVM, exported to model.json) scores messages
//      inside the browser and reports are kept in the extension's own storage

importScripts('speaksafe-model.js');

const SERVER = 'http://localhost:8080';
const RETRY_SERVER_AFTER_MS = 30000;   // when the server is down, check again after 30 s
const MAX_LOCAL_REPORTS = 500;

const DEFAULT_SETTINGS = {
  enabled: true,      // master on/off switch
  warnTyping: true,   // warn the user while typing
  scanPage: true,     // blur toxic messages on the page
  autoFlag: true      // save blurred messages for moderators
};

chrome.runtime.onInstalled.addListener(async () => {
  const saved = await chrome.storage.sync.get(DEFAULT_SETTINGS);
  await chrome.storage.sync.set(saved);
  await chrome.storage.local.set({ checked: 0, blurred: 0, warned: 0, reported: 0 });
});

// ---------------- built-in model ----------------
let modelPromise = null;
function builtInModel() {
  if (!modelPromise) {
    modelPromise = fetch(chrome.runtime.getURL('model.json'))
      .then(response => response.json())
      .then(json => new SpeakSafeModel(json));
  }
  return modelPromise;
}

// ---------------- Java server ----------------
let serverDownSince = 0;

async function callServer(path, body) {
  if (serverDownSince && Date.now() - serverDownSince < RETRY_SERVER_AFTER_MS) {
    throw new Error('Server offline');
  }
  try {
    const response = await fetch(SERVER + path, {
      method: body ? 'POST' : 'GET',
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(3000)
    });
    if (!response.ok) throw new Error('Server error ' + response.status);
    serverDownSince = 0;
    return await response.json();
  } catch (error) {
    serverDownSince = Date.now();
    throw error;
  }
}

// ---------------- reports kept in the extension (when there is no server) ----------------
async function saveLocalReport(result, url, reportedBy) {
  const { localReports = [] } = await chrome.storage.local.get('localReports');
  const duplicate = localReports.some(r => r.text === result.text && r.sourceUrl === url && r.reportedBy === reportedBy);
  if (duplicate) return;
  localReports.unshift({
    id: Date.now() + Math.random(),
    text: result.text,
    score: result.score,
    category: result.category,
    sourceUrl: url || '',
    reportedBy: reportedBy,
    createdAt: new Date().toLocaleString()
  });
  await chrome.storage.local.set({ localReports: localReports.slice(0, MAX_LOCAL_REPORTS) });
}

async function addToCounter(name, amount) {
  if (!amount) return;
  const counters = await chrome.storage.local.get({ [name]: 0 });
  await chrome.storage.local.set({ [name]: counters[name] + amount });
}

// ---------------- message handling ----------------
async function analyzeMany(texts, url, autoFlag) {
  try {
    return await callServer('/api/analyze/batch', { texts, sourceUrl: url, autoFlag });
  } catch (e) {
    const model = await builtInModel();
    const results = texts.map(t => model.analyze(t));
    if (autoFlag) {
      for (const r of results) if (r.action === 'BLUR') await saveLocalReport(r, url, 'AUTO');
    }
    return results;
  }
}

async function handle(message, sender) {
  switch (message.type) {
    case 'health':
      try {
        await callServer('/api/health');
        return { mode: 'server' };
      } catch (e) {
        await builtInModel();
        return { mode: 'built-in' };
      }

    case 'analyze': {
      let result;
      try {
        result = await callServer('/api/analyze', { text: message.text, sourceUrl: message.url, autoFlag: false });
      } catch (e) {
        result = (await builtInModel()).analyze(message.text);
      }
      await addToCounter('checked', 1);
      if (result.action !== 'NONE') await addToCounter('warned', 1);
      return result;
    }

    case 'analyzeBatch': {
      const results = await analyzeMany(message.texts, message.url, message.autoFlag);
      const blurred = results.filter(r => r.action === 'BLUR').length;
      await addToCounter('checked', results.length);
      await addToCounter('blurred', blurred);
      // Show how many messages were hidden on this page as a red number on the icon.
      if (sender.tab && blurred > 0) {
        chrome.action.setBadgeBackgroundColor({ tabId: sender.tab.id, color: '#dc2626' });
        chrome.action.setBadgeText({ tabId: sender.tab.id, text: String(message.pageBlurredTotal + blurred) });
      }
      return results;
    }

    case 'report': {
      let result;
      try {
        result = await callServer('/api/report', { text: message.text, sourceUrl: message.url });
      } catch (e) {
        result = (await builtInModel()).analyze(message.text);
        await saveLocalReport(result, message.url, 'USER');
      }
      await addToCounter('reported', 1);
      return result;
    }
  }
  throw new Error('Unknown message type: ' + message.type);
}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  handle(message, sender)
    .then(data => sendResponse({ ok: true, data }))
    .catch(error => sendResponse({ ok: false, error: error.message }));
  return true;   // tells Chrome we will answer asynchronously
});
