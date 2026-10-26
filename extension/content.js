// SpeakSafe content script: runs inside every web page.
// 1. Warns the user while they type a hurtful message.
// 2. Scans the messages on the page and blurs toxic ones.

(() => {
  const BATCH_SIZE = 25;
  const MIN_LENGTH = 3;
  const MAX_LENGTH = 600;
  const LABELS = { INSULT: 'Insult', THREAT: 'Threat', HATE_SPEECH: 'Hate speech', SAFE: 'Safe' };

  let settings = { enabled: true, warnTyping: true, scanPage: true, autoFlag: true };
  let blurredOnPage = 0;
  const checkedText = new WeakMap();   // element -> text we already analyzed

  // Don't scan SpeakSafe's own dashboard, otherwise it would blur the reports.
  if (location.host === 'localhost:8080') return;

  // Sends a request to background.js and returns the backend's answer (or null if the backend is offline).
  function ask(message) {
    return new Promise(resolve => {
      try {
        chrome.runtime.sendMessage(message, response => {
          resolve(response && response.ok ? response.data : null);
        });
      } catch (e) {
        resolve(null);   // extension was reloaded; ignore
      }
    });
  }

  // ---------------------------------------------------------------
  // PART 1: warn while typing
  // ---------------------------------------------------------------
  let bubble = null;
  let typingTimer = null;

  function isTextBox(el) {
    if (!el || !el.tagName) return false;
    if (el.tagName === 'TEXTAREA') return true;
    if (el.tagName === 'INPUT') return ['text', 'search', ''].includes((el.getAttribute('type') || '').toLowerCase());
    return el.isContentEditable;
  }

  function textOf(el) {
    return (el.value !== undefined ? el.value : el.innerText || '').trim();
  }

  function hideBubble() {
    if (bubble) bubble.remove();
    bubble = null;
  }

  function showBubble(target, result) {
    hideBubble();
    bubble = document.createElement('div');
    bubble.className = 'speaksafe-bubble ' + (result.action === 'BLUR' ? 'block' : 'warn');
    bubble.textContent = (result.action === 'BLUR' ? '⛔ ' : '⚠️ ')
      + 'SpeakSafe: this message looks like ' + LABELS[result.category].toLowerCase()
      + ' (toxicity ' + result.score + '/100). Please think before you send it.';
    document.body.appendChild(bubble);
    const box = target.getBoundingClientRect();
    bubble.style.left = (window.scrollX + box.left) + 'px';
    // Show the bubble below the text box, or above it if there is no room below (e.g. chat apps).
    const fitsBelow = box.bottom + 6 + bubble.offsetHeight <= window.innerHeight;
    const top = fitsBelow ? box.bottom + 6 : box.top - 6 - bubble.offsetHeight;
    bubble.style.top = (window.scrollY + top) + 'px';
  }

  document.addEventListener('input', event => {
    const target = event.target.closest ? event.target.closest('textarea, input, [contenteditable]') : null;
    if (!settings.enabled || !settings.warnTyping || !isTextBox(target)) return;
    clearTimeout(typingTimer);
    typingTimer = setTimeout(async () => {   // wait until the user pauses typing
      const text = textOf(target);
      if (text.length < MIN_LENGTH) { hideBubble(); return; }
      const result = await ask({ type: 'analyze', text: text.slice(0, 2000), url: location.href });
      if (result && result.action !== 'NONE' && textOf(target) === text) showBubble(target, result);
      else hideBubble();
    }, 700);
  }, true);

  document.addEventListener('focusout', () => setTimeout(hideBubble, 200), true);

  // ---------------------------------------------------------------
  // PART 2: scan the page and blur toxic messages
  // ---------------------------------------------------------------
  const SKIP = 'script, style, noscript, textarea, input, select, code, pre, [contenteditable], .speaksafe-label, .speaksafe-bubble';

  // Finds the smallest "block" element (paragraph, list item, comment div, ...) around a piece of text.
  function blockOf(node) {
    let el = node.parentElement;
    while (el && el !== document.body && getComputedStyle(el).display.startsWith('inline')) {
      el = el.parentElement;
    }
    return el;
  }

  function findMessages() {
    const blocks = new Set();
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    let node;
    while ((node = walker.nextNode()) && blocks.size < 300) {
      if (node.nodeValue.trim().length < MIN_LENGTH || node.parentElement.closest(SKIP)) continue;
      const block = blockOf(node);
      if (!block || block === document.body || block.closest(SKIP + ', .speaksafe-blurred')) continue;
      blocks.add(block);
    }
    const messages = [];
    for (const block of blocks) {
      // Use only the innermost blocks, so a comment and its container are not both blurred.
      if ([...blocks].some(other => other !== block && block.contains(other))) continue;
      if (block.querySelector('.speaksafe-blurred, .speaksafe-label')) continue;
      const text = (block.innerText || '').trim().replace(/\s+/g, ' ');
      if (text.length < MIN_LENGTH || text.length > MAX_LENGTH) continue;
      if (checkedText.get(block) === text) continue;   // already checked, nothing changed
      checkedText.set(block, text);
      messages.push({ block, text });
    }
    return messages;
  }

  function blur(block, result) {
    if (block.classList.contains('speaksafe-blurred')) return;
    block.classList.add('speaksafe-blurred');

    const label = document.createElement('span');
    label.className = 'speaksafe-label';
    label.textContent = '🛡️ Hidden by SpeakSafe: ' + LABELS[result.category] + ' (' + result.score + '/100)';

    const show = document.createElement('button');
    show.textContent = 'Show';
    show.onclick = () => {
      const hidden = block.classList.toggle('speaksafe-blurred');
      show.textContent = hidden ? 'Show' : 'Hide';
    };

    const report = document.createElement('button');
    report.textContent = 'Report';
    report.onclick = async () => {
      report.textContent = 'Reporting…';
      const ok = await ask({ type: 'report', text: result.text, url: location.href });
      report.textContent = ok ? 'Reported ✓' : 'Server offline';
      report.disabled = true;
    };

    label.append(show, report);
    block.parentNode.insertBefore(label, block);
  }

  function warn(block, result) {
    block.classList.add('speaksafe-warned');
    block.title = 'SpeakSafe: possible ' + LABELS[result.category].toLowerCase() + ' (' + result.score + '/100)';
  }

  let scanning = false;
  async function scanPage() {
    if (!settings.enabled || !settings.scanPage || scanning) return;
    scanning = true;
    try {
      const messages = findMessages();
      for (let i = 0; i < messages.length; i += BATCH_SIZE) {
        const batch = messages.slice(i, i + BATCH_SIZE);
        const results = await ask({
          type: 'analyzeBatch',
          texts: batch.map(m => m.text),
          url: location.href,
          autoFlag: settings.autoFlag,
          pageBlurredTotal: blurredOnPage
        });
        if (!results) return;   // backend offline
        results.forEach((result, j) => {
          if (result.action === 'BLUR') { blur(batch[j].block, result); blurredOnPage++; }
          else if (result.action === 'WARN') warn(batch[j].block, result);
        });
      }
    } finally {
      scanning = false;
    }
  }

  // Re-scan when the page changes (new chat messages, new comments, infinite scroll...).
  let scanTimer = null;
  const observer = new MutationObserver(mutations => {
    const ours = mutations.every(m => [...m.addedNodes].every(n =>
      n.classList && (n.classList.contains('speaksafe-label') || n.classList.contains('speaksafe-bubble'))));
    if (ours) return;
    clearTimeout(scanTimer);
    scanTimer = setTimeout(scanPage, 1000);
  });

  // ---------------------------------------------------------------
  // Settings (changed from the popup)
  // ---------------------------------------------------------------
  chrome.storage.sync.get(settings, saved => {
    settings = saved;
    scanPage();
    observer.observe(document.body, { childList: true, subtree: true, characterData: true });
  });

  chrome.storage.onChanged.addListener((changes, area) => {
    if (area !== 'sync') return;
    for (const key in changes) settings[key] = changes[key].newValue;
    if (!settings.enabled || !settings.warnTyping) hideBubble();
    if (settings.enabled && settings.scanPage) scanPage();
  });
})();
