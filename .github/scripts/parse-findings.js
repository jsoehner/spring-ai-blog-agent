const fs = require('fs');

let allFindings = [];

// Parse Semgrep
if (fs.existsSync('semgrep-results.json')) {
  try {
    const raw = fs.readFileSync('semgrep-results.json', 'utf8');
    const data = JSON.parse(raw);
    if (data.results && Array.isArray(data.results)) {
      for (const result of data.results) {
        allFindings.push({
          tool: 'Semgrep',
          file: `${result.path}:${result.start.line}`,
          description: result.extra.message,
          severity: result.extra.severity || 'HIGH'
        });
      }
    }
  } catch (e) {
    console.error('Error parsing Semgrep report:', e);
  }
}

// Write findings to markdown table if any exist
if (allFindings.length > 0) {
  let table = '| Tool | File | Severity | Description |\n| --- | --- | --- | --- |\n';
  allFindings.forEach(f => {
    table += `| ${f.tool} | ${f.file} | ${f.severity} | ${f.description.replace(/\n/g, ' ')} |\n`;
  });
  fs.writeFileSync('findings-table.md', table);
}
