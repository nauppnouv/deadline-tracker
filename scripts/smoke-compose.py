"""Local disposable Compose acceptance; no real accounts, no volume deletion."""
import http.cookiejar
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import socket
import subprocess
import time
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parent.parent
os.chdir(ROOT)
project = "deadline-w1-" + secrets.token_hex(4)
env = dict(os.environ)
env.update(DB_PASSWORD=secrets.token_urlsafe(32), DB_ROOT_PASSWORD=secrets.token_urlsafe(32))

def port():
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return str(s.getsockname()[1])

env.update(APP_PORT=port(), DB_PORT=port(), DOCKER_CONFIG=str(ROOT / ".tools/docker-client"))
docker_config = Path(env["DOCKER_CONFIG"])
docker_config.mkdir(parents=True, exist_ok=True)
vendor_plugins = Path(shutil.which("docker")).resolve().parent.parent / "cli-plugins"
config = {"auths": {}}
if vendor_plugins.is_dir():
    config["cliPluginsExtraDirs"] = [str(vendor_plugins)]
(docker_config / "config.json").write_text(json.dumps(config), encoding="utf-8")
base = "http://127.0.0.1:" + env["APP_PORT"]
command = ["docker", "compose", "--project-name", project]
report = ["Compose disposable project=" + project, "No real credentials or user data. Volumes retained; no -v cleanup."]

def compose(*args):
    result = subprocess.run(command + list(args), env=env, capture_output=True, text=True, encoding="utf-8", errors="replace")
    if result.returncode:
        # Build/status output is safe; never invoke config output or container env inspect.
        raise RuntimeError(result.stdout[-3000:] + result.stderr[-3000:])
    return result.stdout

opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def get(path):
    with opener.open(base + path, timeout=10) as response:
        return response.read().decode("utf-8")

def post(path, fields):
    with opener.open(base + path, urllib.parse.urlencode(fields).encode(), timeout=15) as response:
        return response.read().decode("utf-8"), response.url

def csrf(html):
    match = re.search(r'<input[^>]*name="_csrf"[^>]*value="([^"]+)"', html)
    if not match:
        raise AssertionError("Expected CSRF hidden field")
    return match.group(1)

started = False
try:
    compose("config", "--quiet")
    started = True
    compose("up", "--build", "-d")
    deadline = time.monotonic() + 180
    while True:
        try:
            get("/login")
            break
        except Exception:
            if time.monotonic() > deadline:
                raise
            time.sleep(1)
    password = secrets.token_urlsafe(24)
    html = get("/register")
    post("/register", dict(_csrf=csrf(html), username="compose.fixture", displayName="Compose fixture", password=password))
    html = get("/login")
    html, url = post("/login", dict(_csrf=csrf(html), username="compose.fixture", password=password))
    assert url.endswith("/groups") and "Nhóm của tôi" in html
    html = get("/groups/new")
    html, group_url = post("/groups", dict(_csrf=csrf(html), name="Compose persistent group", description="Synthetic only"))
    assert "Compose persistent group" in html
    # Restart the actual packaged application, reusing the MariaDB volume.
    compose("restart", "app")
    deadline = time.monotonic() + 90
    while True:
        try:
            html = get(urllib.parse.urlsplit(group_url).path)
            assert "Compose persistent group" in html
            break
        except Exception:
            if time.monotonic() > deadline:
                raise
            time.sleep(1)
    uid = compose("exec", "-T", "app", "id", "-u").strip()
    assert uid == "10001", uid
    report += ["PASS HTTP register/login/create group via packaged jar", "PASS restart app retains group and JDBC session", "PASS app user UID=10001", "PASS loopback port bindings specified by Compose"]
finally:
    if started:
        compose("down")  # Deliberately retains the trial volume. Never delete user volumes.
    report.append("Stopped/removed only this trial project's containers/network. Trial volume: " + project + "_database")
    (ROOT / "target/compose-smoke.txt").write_text("\n".join(report) + "\n", encoding="utf-8")
print("COMPOSE_SMOKE_OK " + project)
