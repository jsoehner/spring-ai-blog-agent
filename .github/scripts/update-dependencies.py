#!/usr/bin/env python3
import re
import urllib.request
import xml.etree.ElementTree as ET
import os

def parse_version(v_str):
    # Splits version into integer parts and pre-release suffix list
    # e.g., "4.1.0-RC1" -> ([4, 1, 0], ["rc", 1])
    # e.g., "4.1.0" -> ([4, 1, 0], ["z_stable"])
    match = re.match(r'^([\d.]+)(.*)$', v_str)
    if not match:
        return ([], ["unknown"])
    
    digits = [int(x) for x in match.group(1).strip('.').split('.')]
    suffix = match.group(2).strip('-').lower()
    
    if not suffix:
        suffix_parts = ["z_stable"]
    else:
        # Convert numeric parts inside suffix to integers for proper comparison
        suffix_parts = []
        for x in re.split(r'[-.]', suffix):
            if not x:
                continue
            if x.isdigit():
                suffix_parts.append((0, int(x)))
            else:
                suffix_parts.append((1, x))
        # Add a prefix to ensure "z_stable" string comparison comparison works if needed
        # We can just represent stable as (2, "stable") and pre-release as (1, suffix)
        suffix_parts = [(1, suffix_parts)]
        
    # Standardize comparison tuple: (digits, is_stable, suffix_parts)
    is_stable = 1 if not suffix else 0
    return (digits, is_stable, suffix_parts)

def get_latest_version(group, artifact, current_version):
    # Validate group and artifact to prevent SSRF or directory traversal
    if not re.match(r'^[a-zA-Z0-9.\-_]+$', group) or not re.match(r'^[a-zA-Z0-9.\-_]+$', artifact):
        print(f"[-] Invalid characters in group/artifact: {group}:{artifact}")
        return None

    group_path = group.replace('.', '/')
    url = f"https://repo1.maven.org/maven2/{group_path}/{artifact}/maven-metadata.xml"
    
    # Ensure the URL is strictly targeting Maven Central
    if not url.startswith("https://repo1.maven.org/maven2/"):
        print(f"[-] Blocked unsafe URL: {url}")
        return None

    try:
        # nosemgrep: urllib-ssrf-or-lfi
        req = urllib.request.Request(
            url, 
            headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
        )
        with urllib.request.urlopen(req, timeout=10) as response:  # nosemgrep: urllib-ssrf-or-lfi
            xml_data = response.read()
        root = ET.fromstring(xml_data)
        
        # Collect all versions
        versions = []
        for v in root.findall('.//versioning/versions/version'):
            if v.text:
                versions.append(v.text)
                
        if not versions:
            return None
            
        # If current version is stable, we only want to upgrade to a stable version
        current_parsed = parse_version(current_version)
        current_is_stable = current_parsed[1] == 1
        
        valid_versions = []
        for v in versions:
            parsed = parse_version(v)
            if current_is_stable and parsed[1] == 0:
                # Skip pre-releases if we are on a stable release
                continue
            valid_versions.append((parsed, v))
            
        if not valid_versions:
            return None
            
        valid_versions.sort()
        return valid_versions[-1][1]
    except Exception as e:
        print(f"[-] Error fetching version for {group}:{artifact}: {e}")
    return None

def main():
    build_gradle_path = "build.gradle"
    if not os.path.exists(build_gradle_path):
        print("[-] build.gradle not found in the current directory.")
        return

    with open(build_gradle_path, 'r') as f:
        content = f.read()

    original_content = content

    # 1. Update Spring Boot Plugin Version
    boot_match = re.search(r"id\s+'org\.springframework\.boot'\s+version\s+'([^']+)'", content)
    if boot_match:
        current_version = boot_match.group(1)
        latest_version = get_latest_version("org.springframework.boot", "spring-boot-starter", current_version)
        if latest_version and parse_version(latest_version) > parse_version(current_version):
            print(f"[+] Spring Boot: {current_version} -> {latest_version}")
            content = re.sub(
                r"(id\s+'org\.springframework\.boot'\s+version\s+')[^']+'",
                rf"\g<1>{latest_version}'",
                content
            )

    # 2. Update Spring AI Version
    ai_match = re.search(r"set\('springAiVersion',\s*\"([^\"]+)\"\)", content)
    if ai_match:
        current_version = ai_match.group(1)
        latest_version = get_latest_version("org.springframework.ai", "spring-ai-bom", current_version)
        if latest_version and parse_version(latest_version) > parse_version(current_version):
            print(f"[+] Spring AI: {current_version} -> {latest_version}")
            content = re.sub(
                r"(set\('springAiVersion',\s*\")[^\"]+(\"\))",
                rf"\g<1>{latest_version}\g<2>",
                content
            )

    # 3. Update standard dependencies
    dep_pattern = re.compile(r"'([^':]+):([^':]+):([^']+)'")
    
    def replace_dep(match):
        group, artifact, current_version = match.group(1), match.group(2), match.group(3)
        latest_version = get_latest_version(group, artifact, current_version)
        if latest_version and parse_version(latest_version) > parse_version(current_version):
            print(f"[+] {group}:{artifact}: {current_version} -> {latest_version}")
            return f"'{group}:{artifact}:{latest_version}'"
        return match.group(0)

    content = dep_pattern.sub(replace_dep, content)

    if content != original_content:
        with open(build_gradle_path, 'w') as f:
            f.write(content)
        print("[+] build.gradle updated successfully.")
    else:
        print("[~] No updates found or needed.")

if __name__ == "__main__":
    main()
