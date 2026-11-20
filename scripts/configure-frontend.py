#!/usr/bin/env python3
"""Write the deployed frontend API address from the resolved Compose configuration."""
import json
import os
from pathlib import Path
import sys
from urllib.parse import urlsplit

config = json.load(sys.stdin)
env = config['services']['web']['environment']
api = env.get('API_GATEWAY_URL', '').rstrip('/')
if api:
    parsed = urlsplit(api)
    if parsed.scheme != 'https' or not parsed.hostname:
        raise SystemExit('API_GATEWAY_URL must be an HTTPS URL.')
    api += '/api/v1'
else:
    if env.get('APP_DOMAIN', 'localhost') != 'localhost':
        raise SystemExit('Set API_GATEWAY_URL in .env before publishing the frontend.')
    api = '/api/v1'
directory = Path('public')
directory.mkdir(exist_ok=True)
temporary = directory / '.app-config.json.tmp'
temporary.write_text(json.dumps({'apiUrl': api}) + '\n')
os.replace(temporary, directory / 'app-config.json')
