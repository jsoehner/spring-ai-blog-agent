const fs = require('fs');

let allFindings = [];

// Parse Gitleaks
if (fs.existsSync('gitleaks-report.json')) {
  try {
    const raw = fs.readFileSync('gitleaks-report.json', 'utf8');
    const data = JSON.parse(raw);
    if (Array.isArray(data)) {
      for (const finding of data) {
        allFindings.push({
          tool: 'Gitleaks',
          file: `${finding.File}:${finding.StartLine}`,
          description: `${finding.Description} (${finding.RuleID})`,
          severity: 'CRITICAL'
        });
      }
    }
  } catch (e) {
    console.error('Error parsing Gitleaks report:', e);
  }
}

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

// Parse Trivy
if (fs.existsSync('trivy-results.json')) {
  try {
    const raw = fs.readFileSync('trivy-results.json', 'utf8');
    const data = JSON.parse(raw);
    if (data.Results && Array.isArray(data.Results)) {
      for (const result of data.Results) {
        if (result.Vulnerabilities && Array.isArray(result.Vulnerabilities)) {
          for (const vuln of result.Vulnerabilities) {
            allFindings.push({
              tool: 'Trivy',
              file: result.Target,
              description: `${vuln.PkgName} (${vuln.VulnerabilityID}): ${vuln.Title || vuln.Description || ''}`,
              severity: vuln.Severity || 'HIGH'
            });
          }
        }
      }
    }
  } catch (e) {
    console.error('Error parsing Trivy report:', e);
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
