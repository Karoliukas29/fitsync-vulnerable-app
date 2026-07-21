# FitSync Mock Backend

A tiny, dependency-free stand-in for the FitSync API so you can run the app
end-to-end **locally** and intercept real traffic — no cloud, no accounts.

The responses are **deliberately insecure**: the `/auth/login` response over-shares
(password hash, Stripe customer id, internal ids, admin notes), mirroring the flaws
planted in the app. Everything here is fabricated. Only run it on a machine and
network you control.

Requires nothing but **Python 3.8+** (standard library only).

---

## What it serves

One `--tls` run impersonates **three** hosts at once — HTTPS on 443 for the two API
hosts, plain HTTP on 80 for the cleartext analytics host:

| Host | Method | Path | Returns |
|------|--------|------|---------|
| `api.fitsync.io` (HTTPS) | POST | `/auth/login` | Over-sharing auth response (the star of the show) |
| `api.fitsync.io` | GET | `/auth/refresh` | **401** — so the app's interceptor leaks the Bearer token to logcat |
| `api.fitsync.io` | GET | `/members` | List of members (PII) |
| `api.fitsync.io` | GET | `/members/{id}` | A single member |
| `api.fitsync.io` | GET/POST | `/subscriptions/*` | Subscription status |
| `api.fitcloud.io` (HTTPS) | POST | `/v1/sync` | Ack — the request carries the hardcoded FitCloud key + secret |
| `api.fitcloud.io` | POST | `/v1/webhooks/register` | Ack |
| `analytics.fitsync.io` (**HTTP**) | GET | `/track`, `/identify` | Ack — API key in the URL, PII in the query string |

---

## Quick start (just see the responses)

```bash
python3 mock_server.py            # http://127.0.0.1:8080
curl -s -X POST http://127.0.0.1:8080/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"email":"trainer@fitsync.io","password":"hunter2"}'
```

You'll see the full leaky JSON. That's exactly what the app receives.

---

## Full setup — intercept the app in Burp

The app talks to `https://api.fitsync.io/`, a host that doesn't exist publicly.
We make that name resolve to this mock, on your machine, and read the traffic in
Burp on the way through.

**1. Run the mock over TLS** (one command impersonates all three hosts — HTTPS on
443 for the API hosts, HTTP on 80 for cleartext analytics; the bundled cert's SAN
covers `api.fitsync.io` and `api.fitcloud.io`):

```bash
sudo python3 mock_server.py --tls          # https://127.0.0.1:443 + http://127.0.0.1:80
```

`sudo` is only needed because 443 and 80 are privileged. Prefer a high port? Run
`python3 mock_server.py --tls --port 8443` and set Burp's redirect port to match.
Don't need the cleartext HTTP listener? Add `--http-port 0`.

**2. Point the emulator through Burp.** Start an Android emulator, then set its
proxy to your Burp listener (Settings → proxy, or launch with
`-http-proxy http://10.0.2.2:8080`). Install the Burp CA cert on the emulator as a
**system** CA (the app has no certificate pinning, so a trusted CA is all you need).

**3. Make the fake hosts resolve to the mock.** In Burp:
`Settings → Network → Connections → Hostname resolution` → add all three:

```
api.fitsync.io        →  127.0.0.1
api.fitcloud.io       →  127.0.0.1
analytics.fitsync.io  →  127.0.0.1
```

Burp will now send requests for those hosts to your local mock. (If you used
`--port 8443`, add a Proxy listener redirect to `127.0.0.1:8443` instead.)

**4. Log in.** Open the app, enter any email/password, tap **Sign in**. In Burp's
**HTTP history** you'll see:

```
POST https://api.fitsync.io/auth/login
```

with the response body exposing `passwordHash`, `stripeCustomerId`, `adminNotes`,
and more — none of which the mobile client needs.

> If no proxy/mock is running, the app can't reach the backend and quietly falls
> back to a local demo session, so it still opens. You'll just see the network
> error in logcat instead of a real response.

---

## Also worth intercepting

- **Cleartext analytics** — on login and on the dashboard the app makes plain
  `http://analytics.fitsync.io/...` calls with the API key in the URL and PII in the
  query string. No CA cert needed; they're already unencrypted.

---

## Files

- `mock_server.py` — the server (stdlib only)
- `cert.pem` / `key.pem` — self-signed cert covering `api.fitsync.io` **and**
  `api.fitcloud.io` (for `--tls`). Regenerate anytime with:
  ```bash
  openssl req -x509 -newkey rsa:2048 -nodes -keyout key.pem -out cert.pem \
    -days 3650 -subj "/CN=api.fitsync.io" \
    -addext "subjectAltName=DNS:api.fitsync.io,DNS:api.fitcloud.io,DNS:analytics.fitsync.io,DNS:localhost,IP:127.0.0.1"
  ```
