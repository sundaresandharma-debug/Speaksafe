// SpeakSafe popup: settings switches, counters, server status and a quick test box.

const SETTINGS = ['enabled', 'warnTyping', 'scanPage', 'autoFlag'];
const COUNTERS = ['checked', 'blurred', 'warned', 'reported'];

function ask(message) {
  return new Promise(resolve => chrome.runtime.sendMessage(message, response =>
    resolve(response && response.ok ? response.data : null)));
}

async function loadSettings() {
  const saved = await chrome.storage.sync.get({ enabled: true, warnTyping: true, scanPage: true, autoFlag: true });
  for (const key of SETTINGS) {
    const box = document.getElementById(key);
    box.checked = saved[key];
    box.addEventListener('change', () => chrome.storage.sync.set({ [key]: box.checked }));
  }
}

async function loadCounters() {
  const counts = await chrome.storage.local.get({ checked: 0, blurred: 0, warned: 0, reported: 0 });
  for (const key of COUNTERS) document.getElementById(key).textContent = counts[key];
}

async function checkServer() {
  const status = document.getElementById('status');
  const link = document.getElementById('dashboardLink');
  const health = await ask({ type: 'health' });
  if (health && health.mode === 'server') {
    status.textContent = 'Connected to Java server';
    status.className = 'status online';
    link.href = 'http://localhost:8080/dashboard.html';
    link.textContent = 'Open moderator dashboard →';
  } else {
    // No server needed: the built-in model runs inside the browser.
    status.textContent = 'Built-in model active (no server needed)';
    status.className = 'status builtin';
    link.href = chrome.runtime.getURL('reports.html');
    link.textContent = 'Open flagged messages →';
  }
}

async function testMessage() {
  const text = document.getElementById('testText').value.trim();
  const output = document.getElementById('testResult');
  if (!text) return;
  const result = await ask({ type: 'analyze', text: text, url: 'popup' });
  if (!result) {
    output.textContent = 'Server offline';
    output.className = 'result';
    return;
  }
  output.textContent = result.category.replace('_', ' ') + ' – toxicity ' + result.score + '/100';
  output.className = 'result ' + result.category;
  loadCounters();
}

document.getElementById('testBtn').addEventListener('click', testMessage);
loadSettings();
loadCounters();
checkServer();
