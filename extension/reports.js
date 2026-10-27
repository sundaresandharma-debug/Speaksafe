// Lists the flagged / reported messages kept by the extension (used when the Java server is not running).

const CATEGORIES = ['INSULT', 'THREAT', 'HATE_SPEECH'];

function cell(text, className) {
  const td = document.createElement('td');
  td.textContent = text;   // plain text only, so stored messages cannot inject code
  if (className) td.className = className;
  return td;
}

async function render() {
  const { localReports = [] } = await chrome.storage.local.get('localReports');

  const stats = document.getElementById('stats');
  stats.innerHTML = '';
  for (const category of CATEGORIES) {
    const div = document.createElement('div');
    div.className = 'stat ' + category;
    const count = document.createElement('b');
    count.textContent = localReports.filter(r => r.category === category).length;
    div.append(count, category.replace('_', ' '));
    stats.appendChild(div);
  }

  const body = document.getElementById('reports');
  body.innerHTML = '';
  document.getElementById('empty').hidden = localReports.length > 0;
  for (const r of localReports) {
    const tr = document.createElement('tr');
    const badgeCell = document.createElement('td');
    const badge = document.createElement('span');
    badge.className = 'badge ' + r.category;
    badge.textContent = r.category.replace('_', ' ');
    badgeCell.appendChild(badge);

    const actionCell = document.createElement('td');
    const dismiss = document.createElement('button');
    dismiss.textContent = 'Dismiss';
    dismiss.onclick = async () => {
      const { localReports: all = [] } = await chrome.storage.local.get('localReports');
      await chrome.storage.local.set({ localReports: all.filter(x => x.id !== r.id) });
    };
    actionCell.appendChild(dismiss);

    tr.append(cell(r.createdAt), cell(r.text), cell(r.score), badgeCell, cell(r.sourceUrl, 'url'), cell(r.reportedBy), actionCell);
    body.appendChild(tr);
  }
}

document.getElementById('clearBtn').addEventListener('click', async () => {
  if (confirm('Delete all flagged messages stored in this browser?')) {
    await chrome.storage.local.set({ localReports: [] });
  }
});

chrome.storage.onChanged.addListener((changes, area) => {
  if (area === 'local' && changes.localReports) render();
});
render();
