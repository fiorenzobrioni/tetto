# Keystores

One keystore lives in this folder, committed **on purpose**. It is not secret: its passwords are
written below.

| File | Alias | Store / key password | What it signs |
|---|---|---|---|
| `debug.keystore` | `tetto-debug` | `android` / `android` | Debug builds, and the debug-signed release APK CI builds for testing (`-PsignReleaseWithDebugKey`) |

Do not regenerate it: every debug install would have to be uninstalled. Committing it means
debug APKs from CI and from any machine share one signature and update each other in place, the
same arrangement as the family's apps. It is Tetto's own key (created on 5 Nov 2026, RSA 2048,
PKCS12, `CN=Tetto Debug, O=callbackdev, C=IT`), not a copy of a sister's: each app's debug
builds are signed apart.

SHA-256 certificate fingerprints:

- `debug.keystore`: `D4:DF:35:86:A9:89:96:FE:89:76:49:D5:AA:A0:33:91:BB:56:63:DB:84:16:05:CD:AF:81:E7:3A:12:69:78:17`
- release key: `28:A0:E8:60:45:9D:3C:24:06:BC:A7:6F:18:FA:1D:91:42:37:9F:A2:03:EA:85:1D:DE:4B:66:B2:55:19:9D:F1`
  (the one published in the root README and pinned in `release.yml`; `tetto-release.jks`, alias
  `tetto`, PKCS12, created by the owner and put in the four secrets on 5 Nov 2026)

## The release key

It must **never** enter the repo (`.gitignore` refuses `*.jks`, `*.keystore`, `*.p12`, `*.pfx`
outside `debug.keystore`). It signs every release for the life of the app, and a future Google
Play listing must reuse it (PLANNING.md §11 Phase 8), so losing it means users cannot update
without uninstalling. It is kept outside the repo, with the family's keys and an offline backup.

Creating it (once, by the owner, on a machine of theirs; done on 5 Nov 2026):

```bash
keytool -genkeypair -v -keystore tetto-release.jks -storetype PKCS12 \
  -alias tetto -keyalg RSA -keysize 4096 -validity 10950
```

Reading its fingerprint (the certificate is encrypted with the store password, which keytool
asks for):

```bash
keytool -list -v -keystore tetto-release.jks -alias tetto
```

### In GitHub Actions

`release.yml` reads it from four repository secrets (Settings, Secrets and variables, Actions),
the same names as the family's:

- `KEYSTORE_BASE64`: the keystore file, `base64 -w0 tetto-release.jks`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The workflow decodes the keystore into the runner's temp folder, passes the rest to Gradle as
`ORG_GRADLE_PROJECT_*` variables, and stops by name if a secret is missing. Nothing secret is
written to the logs or to the command line. It then checks the APK's signature against the
fingerprint above, and refuses any other key.

### Locally

Put the four properties in `~/.gradle/gradle.properties`:

```properties
TETTO_KEYSTORE=/absolute/path/to/tetto-release.jks
TETTO_KEYSTORE_PASSWORD=...
TETTO_KEY_ALIAS=tetto
TETTO_KEY_PASSWORD=...
```

With all four set, `./gradlew :app:assembleRelease` signs with the release key; without them the
release build is unsigned, and `-PsignReleaseWithDebugKey` signs it with the debug key for
testing only.
