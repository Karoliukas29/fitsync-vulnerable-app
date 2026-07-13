# FitSync Pro — A Deliberately Vulnerable Android App

FitSync Pro is a fake fitness-tracking app I built to practice and demonstrate
Android security testing. It looks and behaves like a real app — login, biometric
unlock, workout sync, a premium paywall, Stripe payments — but it has **18
security vulnerabilities planted in it on purpose**.

Use it to practice static analysis, traffic interception, and runtime hooking on
something that behaves like production code, without touching a real app you don't
have permission to test.

> ⚠️ **This app is intentionally insecure. Never install it on a phone you use for
> real accounts, and never point it at real infrastructure.** Run it on an emulator
> or a dedicated test device only.

---

## Everything in here is fake

All the "secrets" you'll find — the Firebase config, the API keys, the Stripe
tokens, the backend hostnames — are **fabricated for this app**. They don't
authenticate against anything real. They're planted so you can practice *finding*
hardcoded secrets, not so you can use them. The package name (`com.fitsync.app`),
the company, and the endpoints are all made up.

---

## Try it

### Option A — install the prebuilt APK (fastest)

Grab `app-debug.apk` from the [**Releases**](../../releases) page, then:

```bash
adb install app-debug.apk
```

Boot an emulator or plug in a test device first. The debug build is unsigned for
Play but installs fine locally.

### Option B — build it yourself

```bash
git clone https://github.com/Karoliukas29/fitsync-vulnerable-app.git
cd fitsync-vulnerable-app
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Needs a JDK 17+ and the Android SDK (`compileSdk 35`). No signing config is
included — add your own if you want a release build.

---

## The challenge

There are **18 planted vulnerabilities**. No answer key ships with this repo on
purpose — the point is to find them yourself. They span the areas you'd audit on
any real engagement:

- **Cryptography** — how data is encrypted at rest, and where the keys come from
- **Authentication** — the biometric gate, and how the session token is trusted
- **IPC / components** — what's exported in the manifest and who can reach it
- **Deep links** — the app's custom URL scheme and how it's verified (or isn't)
- **Network** — TLS handling, what's sent in the clear, what leaks in responses
- **Storage & logs** — where sensitive values get written, and who can read them
- **Secrets** — what's baked into the APK that shouldn't be

Start with `AndroidManifest.xml` and work outward. Everything is discoverable with
free tooling — `apktool`, `jadx`, `adb`, Burp/mitmproxy, and Frida.

Map it to [OWASP MASVS](https://mas.owasp.org/) as you go — every finding lines up
with a MASVS control.

---

## Companion tools & write-ups

Two static-analysis tools I built that surface a lot of these findings
automatically:

- [**apk-audit**](https://github.com/Karoliukas29/quick_apk_analysis) — one-command
  APK audit with MASVS mapping
- [**apk-secret-scanner**](https://github.com/Karoliukas29/apk-secret-scanner) —
  hunts hardcoded secrets in compiled APKs

The full walkthrough series (static analysis → interception → Frida → report) uses
this exact app as its target.

---

## Responsible use

This project exists for education and authorized security testing only. Don't use
the techniques it teaches against apps or systems you don't own or have written
permission to test.
