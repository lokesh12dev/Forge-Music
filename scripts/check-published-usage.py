"""Read-only check: Pages serves the login/dashboard interface and correct share image."""
from time import sleep, time
from pathlib import Path
from hashlib import sha256
from urllib.request import Request, urlopen
import os
repo = os.environ.get('GITHUB_REPOSITORY', 'lokesh12dev/Forge-Music')
repo_name = repo.split('/')[1] if '/' in repo else repo
base = f'https://lokesh12dev.github.io/{repo_name}/usage/'
home = f'https://lokesh12dev.github.io/{repo_name}/'
for attempt in range(6):
    try:
        with urlopen(Request(base + '?private=' + str(int(time())), headers={'Cache-Control':'no-cache'}), timeout=20) as response:
            html = response.read().decode()
        assert 'id="owner-login"' in html and 'data-host="pages"' in html
        assert 'id="owner-dashboard" hidden' in html and 'name="password"' in html
        manifest = __import__('json').loads((Path(__file__).resolve().parents[1] / 'site/usage/assets.json').read_text())
        for name, expected in manifest['assets'].items():
            with urlopen(base + name, timeout=20) as response:
                assert sha256(response.read()).hexdigest() == expected
        with urlopen(Request(home + '?preview=' + str(int(time())), headers={'Cache-Control':'no-cache'}), timeout=20) as response:
            metadata = response.read().decode()
        image = 'img/share-card-v3.jpg'
        assert image in metadata and 'summary_large_image' in metadata
        with urlopen(home + image, timeout=20) as response:
            published = response.read()
        local = Path(__file__).resolve().parents[1] / 'site' / image
        assert sha256(published).digest() == sha256(local.read_bytes()).digest()
        print('Verified Pages login/dashboard assets and exact social preview image published.')
        break
    except Exception:
        if attempt == 5: raise
        sleep(10)
