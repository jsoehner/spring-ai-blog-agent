import argparse
import concurrent.futures
import csv
import os
import sys
from pathlib import Path

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
        base_dir = Path.cwd().resolve()
        target_path = Path(args.csv).resolve()
        if not target_path.is_relative_to(base_dir):
            print(f"[-] Security Error: Path {args.csv} is outside allowed directory {base_dir}")
            sys.exit(1)

        with open(args.csv, "w", newline="") as f:
            writer = csv.DictWriter(f, fieldnames=["Start_URL", "Final_URL", "PQC", "Issuer", "Protocol", "Group", "Cipher"])
            writer.writeheader()
            for r in results:
                sanitized_r = {k: (v if not str(v).startswith(('=', '+', '-', '@', '\\t')) else f"'{v}'") for k, v in r.items()}
                writer.writerow(sanitized_r)
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

        header = [
            ("PQC", col_widths["PQC"]),
            ("START URL", col_widths["Start_URL"]),
            ("FINAL URL", col_widths["Final_URL"]),
            ("PROTOCOL", col_widths["Protocol"]),
            ("NEGOTIATED GROUP", col_widths["Group"]),
            ("CIPHER", col_widths["Cipher"]),
            ("ISSUER", None)
        ]
        
        header_str = " | ".join([f"{name:<{width}}" if width else name for name, width in header])
        print(header_str)
        print("-" * len(header_str))

        for r in results:
            row = [
                str(r.get("PQC", "")),
                str(r.get("Start_URL", "")),
                str(r.get("Final_URL", "")),
                str(r.get("Protocol", "")),
                str(r.get("Group", "")),
                str(r.get("Cipher", "")),
                str(r.get("Issuer", ""))
            ]
            row_str = " | ".join([f"{val:<{col_widths[key]}}" if key in col_widths else val for key, val in zip(
                ["PQC", "Start_URL", "Final_URL", "Protocol", "Group", "Cipher", "Issuer"], row
            )])
            print(row_str)
    else:
        print("No results to display.")

if __name__ == "__main__":
    main()
