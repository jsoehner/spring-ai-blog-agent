#!/usr/bin/env python3
import argparse
import concurrent.futures
import subprocess
import re
import sys
import csv
import requests

import socket
import ipaddress

def is_safe_host(hostname):
    try:
        # Resolve all IP addresses for the hostname
        ips = socket.getaddrinfo(hostname, None)
        for ip in ips:
            ip_str = ip[4][0]
            ip_obj = ipaddress.ip_address(ip_str)
            if (ip_obj.is_loopback or 
                ip_obj.is_private or 
                ip_obj.is_link_local or 
                ip_obj.is_multicast or 
                ip_obj.is_reserved or
                ip_str == "0.0.0.0"):
                return False
        return True
    except Exception:
        return False

def get_final_url(url):
    formatted_url = url if url.startswith(('http://', 'https://')) else 'https://' + url
    try:
        hostname = formatted_url.split('//')[-1].split('/')[0].split(':')[0]
        if not is_safe_host(hostname):
            return formatted_url
        response = requests.get(formatted_url, timeout=5, allow_redirects=True)
        return response.url
    except Exception:
        return formatted_url

def get_tls_audit(target_url):
    hostname = target_url.split('//')[-1].split('/')[0].split(':')[0]
    if not is_safe_host(hostname):
        return {"PQC": False, "Issuer": "Unsafe Host", "Protocol": "Unsafe Host", "Group": "Unsafe Host", "Cipher": "Unsafe Host"}
    cmd = ["openssl", "s_client", "-connect", f"{hostname}:443", "-servername", hostname, "-showcerts"]
    
    try:
        result = subprocess.run(cmd, input=b"Q", capture_output=True, timeout=12)
        if result.returncode != 0:
            raise RuntimeError(f"openssl failed with exit code {result.returncode}")
        output = result.stdout.decode('utf-8', errors='ignore') + result.stderr.decode('utf-8', errors='ignore')
        
        # Regex to capture all Issuer strings explicitly
        issuer_matches = re.findall(r"^\s*i\s*:\s*(.*?)$", output, re.MULTILINE)
        if not issuer_matches:
            issuer_matches = re.findall(r"issuer\s*=\s*(.*?)\n", output, re.IGNORECASE)
            
        seen = set()
        issuers = []
        for issuer in issuer_matches:
            issuer_clean = issuer.strip()
            if issuer_clean not in seen:
                seen.add(issuer_clean)
                issuers.append(issuer_clean)
                
        issuer_val = issuers[-1] if issuers else "N/A"

        protocol = re.search(r"Protocol\s*:\s*(TLSv[\d\.]+)", output)
        cipher = re.search(r"Cipher is\s*(.+)", output)
        group = re.search(r"(?:Negotiated TLS1\.3 group|Shared group|Peer Temp Key)\s*:\s*([\w\-]+)", output)
        
        group_val = group.group(1) if group else "Unknown"
        
        return {
            "PQC": group_val == "X25519MLKEM768",
            "Issuer": issuer_val,
            "Protocol": protocol.group(1) if protocol else "Unknown",
            "Group": group_val,
            "Cipher": cipher.group(1).strip() if cipher else "Unknown"
        }
    except Exception:
        return {"PQC": False, "Issuer": "Failed", "Protocol": "Failed", "Group": "Failed", "Cipher": "Failed"}

def process_target(target):
    final_url = get_final_url(target)
    audit = get_tls_audit(final_url)
    
    # Logic: Only show Final URL if it differs from Start
    display_final = "" if final_url == (target if target.startswith(('http')) else 'https://'+target) else final_url
    if len(display_final) > 50:
        display_final = display_final[:47] + "..."
    
    return {
        "Start_URL": target,
        "Final_URL": display_final,
        **audit
    }

def main():
    parser = argparse.ArgumentParser(description="Advanced TLS/PQC Auditor")
    parser.add_argument("-f", "--file", help="Path to text file with targets")
    parser.add_argument("-c", "--csv", help="Filename for CSV export")
    parser.add_argument("targets", nargs="*", help="List of domains or URLs")
    args = parser.parse_args()

    all_targets = list(args.targets)
    if args.file:
        try:
            with open(args.file, "r") as f:
                all_targets.extend([line.strip() for line in f if line.strip()])
        except Exception as e:
            print(f"[-] Could not read file: {e}")
            sys.exit(1)

    results = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
        future_to_target = {executor.submit(process_target, t): t for t in all_targets}
        for future in concurrent.futures.as_completed(future_to_target):
            results.append(future.result())

    if args.csv:
        with open(args.csv, "w", newline="") as f:
            writer = csv.DictWriter(f, fieldnames=["Start_URL", "Final_URL", "PQC", "Issuer", "Protocol", "Group", "Cipher"])
            writer.writeheader()
            writer.writerows(results)
        print(f"[+] Exported to {args.csv}")
    
    if results:
        col_widths = {
            "PQC": len("PQC"),
            "Start_URL": len("START URL"),
            "Final_URL": len("FINAL URL"),
            "Protocol": len("PROTOCOL"),
            "Group": len("NEGOTIATED GROUP"),
            "Cipher": len("CIPHER"),
            "Issuer": len("ISSUER")
        }

        for r in results:
            col_widths["PQC"] = max(col_widths["PQC"], len(str(r.get("PQC", ""))))
            col_widths["Start_URL"] = max(col_widths["Start_URL"], len(str(r.get("Start_URL", ""))))
            col_widths["Final_URL"] = max(col_widths["Final_URL"], len(str(r.get("Final_URL", ""))))
            col_widths["Protocol"] = max(col_widths["Protocol"], len(str(r.get("Protocol", ""))))
            col_widths["Group"] = max(col_widths["Group"], len(str(r.get("Group", ""))))
            col_widths["Cipher"] = max(col_widths["Cipher"], len(str(r.get("Cipher", ""))))
            col_widths["Issuer"] = max(col_widths["Issuer"], len(str(r.get("Issuer", ""))))

        header_str = f"{'PQC':<{col_widths['PQC']}} | {'START URL':<{col_widths['Start_URL']}} | {'FINAL URL':<{col_widths['Final_URL']}} | {'PROTOCOL':<{col_widths['Protocol']}} | {'NEGOTIATED GROUP':<{col_widths['Group']}} | {'CIPHER':<{col_widths['Cipher']}} | {'ISSUER'}"
        print(header_str)
        print("-" * len(header_str))
        
        for r in results:
            print(f"{str(r.get('PQC', '')):<{col_widths['PQC']}} | {str(r.get('Start_URL', '')):<{col_widths['Start_URL']}} | {str(r.get('Final_URL', '')):<{col_widths['Final_URL']}} | {str(r.get('Protocol', '')):<{col_widths['Protocol']}} | {str(r.get('Group', '')):<{col_widths['Group']}} | {str(r.get('Cipher', '')):<{col_widths['Cipher']}} | {str(r.get('Issuer', ''))}")
    else:
        print("No results to display.")

if __name__ == "__main__":
    main()
