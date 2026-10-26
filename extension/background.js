// SpeakSafe background service worker.
// Content scripts cannot call the Java backend directly (web pages block it),
// so they send messages here and this script talks to the backend.

const SERVER = 'http://localhost:8080';

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

async function callServer(path, body) {
  const response = await fetch(SERVER + path, {
    method: body ? 'POST' : 'GET',
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined
  });
  if (!response.ok) throw new Error('Server error ' + response.status);
  return response.json();
}

async function addToCounter(name, amount) {
  if (!amount) return;
  const counters = await chrome.storage.local.get({ [name]: 0 });
  await chrome.storage.local.set({ [name]: counters[name] + amount });
}

async function handle(message, sender) {
  switch (message.type) {
    case 'health':
      return callServer('/api/health');

    case 'analyze': {
      const result = await callServer('/api/analyze', { text: message.text, sourceUrl: message.url, autoFlag: false });
      await addToCounter('checked', 1);
      if (result.action !== 'NONE') await addToCounter('warned', 1);
      return result;
    }

    case 'analyzeBatch': {
      const results = await callServer('/api/analyze/batch',
        { texts: message.texts, sourceUrl: message.url, autoFlag: message.autoFlag });
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
      const result = await callServer('/api/report', { text: message.text, sourceUrl: message.url });
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
