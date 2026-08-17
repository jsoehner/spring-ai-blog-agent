#!/usr/bin/env python3
import sys
import texthumanize

def main():
    try:
        content = sys.stdin.read()
        if not content.strip():
            sys.stdout.write(content)
            return

        result = texthumanize.humanize(content, intensity=75)
        output_text = result.text if hasattr(result, 'text') else str(result)
        sys.stdout.write(output_text)
    except Exception as e:
        sys.stderr.write(f"Error humanizing text: {e}\n")
        sys.exit(1)

if __name__ == "__main__":
    main()
