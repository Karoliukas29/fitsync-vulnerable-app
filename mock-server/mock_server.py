#!/usr/bin/env python3
"""
FitSync mock backend — a tiny, dependency-free stand-in for the FitSync API.

It exists so you can run the app end-to-end locally and intercept real traffic
(e.g. in Burp Suite) without any cloud infrastructure. The responses are
DELIBERATELY vulnerable — they mirror the flaws planted in the app, most notably
an /auth/login response that over-shares (password hash, Stripe customer id,
internal ids, admin notes). Everything here is fabricated; none of it is real.

Usage:
    python3 mock_server.py                 # HTTP  on 127.0.0.1:8080
    sudo python3 mock_server.py --tls      # HTTPS on 127.0.0.1:443  (impersonates api.fitsync.io)
    python3 mock_server.py --tls --port 8443
    python3 mock_server.py --host 0.0.0.0  # reachable from an emulator via the host IP

See README.md for the full Burp + emulator setup.

Only run this on a machine and network you control. Do not expose it publicly.
"""

import argparse
import json
import ssl
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

HERE = Path(__file__).resolve().parent

# A fake bcrypt-looking hash — NOT a real password hash.
FAKE_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

# A fabricated JWT (header.payload.signature) — the signature is not valid.
FAKE_JWT = (
    "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9"
    ".eyJzdWIiOiJ1c3JfODgyMTMiLCJlbWFpbCI6InVzZXJAZml0c3luYy5pbyIsInJvbGUiOiJtYW5hZ2VyIiwiZXhwIjoxNzA5MjUxMjAwfQ"
    ".c2lnbmF0dXJlX25vdF92YWxpZA"
)
FAKE_REFRESH = "rt_9f2b7c41e0a84d6fbb0c2e5a1d7f3092"


def login_response(email):
    """The intentionally over-sharing /auth/login response (mirrors AuthResponse.kt)."""
    return {
        "token": FAKE_JWT,
        "refreshToken": FAKE_REFRESH,
        "userId": "usr_88213",
        "email": email or "user@fitsync.io",
        "role": "manager",
        "isPremium": False,
        "expiresAt": 1709251200,
        # --- fields the mobile client never needs, but the server sends anyway ---
        "stripeCustomerId": "cus_Qh2Xk9RtVn3Lp0",         # enables Stripe customer enumeration
        "internalUserId": "int_442019",
        "gymChainId": "chain_eu_west_12",
        "passwordHash": FAKE_PASSWORD_HASH,               # must never leave the server
        "memberCount": 84213,                             # internal business metric
        "adminNotes": "VIP account — membership comped, do not bill. Ops escalations to ops@fitsync.io",
    }


MEMBERS = [
    {
        "id": "mem_1001", "displayName": "Ava Thompson", "email": "ava.t@example.com",
        "phone": "+1-202-555-0143", "plan": "premium", "status": "active",
        "expiryDate": "2027-01-31", "gymLocation": "Downtown - Floor 3",
    },
    {
        "id": "mem_1002", "displayName": "Liam Ortega", "email": "liam.o@example.com",
        "phone": "+1-202-555-0198", "plan": "standard", "status": "past_due",
        "expiryDate": "2026-08-15", "gymLocation": "Riverside",
    },
]

SUBSCRIPTION = {
    "isPremium": True,
    "planName": "FitSync Pro Annual",
    "expiryDate": "2027-01-31",
    "stripeSubscriptionId": "sub_1Ph2Xk9RtVn3Lp0",
}


class Handler(BaseHTTPRequestHandler):
    server_version = "FitSyncMock/1.0"

    def _send(self, status, payload):
        body = json.dumps(payload, indent=2).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Server", "nginx/1.25.3")  # a little realism for the screenshot
        self.end_headers()
        self.wfile.write(body)

    def _read_json(self):
        length = int(self.headers.get("Content-Length", 0) or 0)
        if not length:
            return {}
        try:
            return json.loads(self.rfile.read(length) or b"{}")
        except json.JSONDecodeError:
            return {}

    def do_POST(self):
        path = urlparse(self.path).path.rstrip("/")
        if path == "/auth/login":
            email = self._read_json().get("email", "")
            self._send(200, login_response(email))
        elif path == "/subscriptions/purchase":
            self._send(200, SUBSCRIPTION)
        # --- third-party FitCloud sync (host: api.fitcloud.io) ---
        # The app posts here with hardcoded live FitCloud credentials in the headers.
        # We just acknowledge it; the point is the request the app sends.
        elif path == "/v1/sync":
            self._send(200, {"status": "synced", "workoutId": "wk_50291"})
        elif path == "/v1/webhooks/register":
            self._send(200, {"status": "registered"})
        else:
            self._send(404, {"error": "not_found", "path": path})

    def do_GET(self):
        path = urlparse(self.path).path.rstrip("/")
        if path == "/auth/refresh":
            # [demo] Reject the silent refresh so the app's ApiInterceptor hits its
            # error branch and leaks the Bearer token to logcat (mirrors V-12).
            self._send(401, {"error": "token_expired"})
        elif path == "/members":
            self._send(200, MEMBERS)
        elif path.startswith("/members/"):
            member_id = path.rsplit("/", 1)[-1]
            match = next((m for m in MEMBERS if m["id"] == member_id), MEMBERS[0])
            self._send(200, match)
        elif path == "/subscriptions/verify":
            self._send(200, SUBSCRIPTION)
        # --- cleartext analytics (host: analytics.fitsync.io, plain HTTP) ---
        # The API key rides in the query string; email/phone are sent unencrypted.
        elif path in ("/track", "/identify"):
            self._send(200, {"status": "ok"})
        else:
            self._send(404, {"error": "not_found", "path": path})

    def log_message(self, fmt, *args):
        print(f"  {self.command} {self.path} -> {args[1] if len(args) > 1 else ''}")


def make_server(host, port, tls):
    httpd = ThreadingHTTPServer((host, port), Handler)
    if tls:
        ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        ctx.load_cert_chain(certfile=HERE / "cert.pem", keyfile=HERE / "key.pem")
        httpd.socket = ctx.wrap_socket(httpd.socket, server_side=True)
    return httpd


def main():
    ap = argparse.ArgumentParser(description="FitSync mock backend (intentionally vulnerable, all data fake).")
    ap.add_argument("--host", default="127.0.0.1", help="bind address (default 127.0.0.1)")
    ap.add_argument("--port", type=int, help="port (default 8080 http, 443 with --tls)")
    ap.add_argument("--tls", action="store_true", help="serve HTTPS using the bundled self-signed cert")
    ap.add_argument("--http-port", type=int, default=80,
                    help="with --tls, also serve plain HTTP on this port for the cleartext "
                         "analytics.fitsync.io traffic (default 80; set 0 to disable)")
    args = ap.parse_args()

    servers = []
    if args.tls:
        # HTTPS on 443 answers api.fitsync.io + api.fitcloud.io (one cert covers both).
        https_port = args.port or 443
        servers.append(("https", make_server(args.host, https_port, tls=True)))
        # Plain HTTP on 80 answers the cleartext analytics.fitsync.io calls, so the
        # whole app works from one command. Disable with --http-port 0.
        if args.http_port:
            servers.append(("http", make_server(args.host, args.http_port, tls=False)))
    else:
        servers.append(("http", make_server(args.host, args.port or 8080, tls=False)))

    for scheme, httpd in servers:
        print(f"FitSync mock backend listening on {scheme}://{args.host}:{httpd.server_address[1]}")
    print("HTTPS 443 → api.fitsync.io, api.fitcloud.io   |   HTTP 80 → analytics.fitsync.io")
    print("Endpoints: POST /auth/login | GET /auth/refresh (401) | GET /members[/{id}] |")
    print("           /subscriptions/* | POST /v1/sync | GET /track | GET /identify")
    print("All responses are fabricated and intentionally insecure. Ctrl+C to stop.\n")

    # Run every server but the last in daemon threads; block on the last one.
    import threading
    for _, httpd in servers[:-1]:
        threading.Thread(target=httpd.serve_forever, daemon=True).start()
    try:
        servers[-1][1].serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()
