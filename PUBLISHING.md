# Publishing Guide — Puzzle Quest

This guide covers how to prepare and publish Puzzle Quest to the Google Play Store.

---

## Pre-Publishing Checklist

- [ ] Replace all test AdMob ad unit IDs with real IDs
- [ ] Update AdMob App ID in `AndroidManifest.xml`
- [ ] Test on real device with real ads
- [ ] Update version code and version name
- [ ] Create release keystore and add signing config
- [ ] Add GitHub secrets for signing and Play Store
- [ ] Test release build locally
- [ ] Create git tag and push to trigger GitHub Actions

---

## Step 1: Replace Test Ad Unit IDs

### Current Test IDs (DO NOT USE IN PRODUCTION)

```
Banner:       ca-app-pub-3940256099942544/6300978111
Interstitial: ca-app-pub-3940256099942544/1033173712
Rewarded:     ca-app-pub-3940256099942544/5224354917
App ID:       ca-app-pub-3940256099942544~3347511713
```

### Get Your Real Ad Unit IDs

1. Go to [Google AdMob Console](https://admob.google.com)
2. Sign in with your Google account
3. Create an app if you haven't already
4. Create ad units for:
   - Banner (320x50)
   - Interstitial
   - Rewarded
5. Copy the ad unit IDs

### Replace in Code

**File: `app/src/main/java/com/pingsama/puzzlequest/ui/GameScreen.kt`**

Search for `ca-app-pub-3940256099942544/6300978111` and replace with your banner ad unit ID.

**File: `app/src/main/java/com/pingsama/puzzlequest/ads/InterstitialAdManager.kt`**

Search for `ca-app-pub-3940256099942544/1033173712` and replace with your interstitial ad unit ID.

**File: `app/src/main/java/com/pingsama/puzzlequest/ads/RewardedAdManager.kt`**

Search for `ca-app-pub-3940256099942544/5224354917` and replace with your rewarded ad unit ID.

**File: `app/src/main/AndroidManifest.xml`**

Update the AdMob App ID:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
```

---

## Step 2: Create Release Keystore

Generate a release signing key:

```sh
keytool -genkey -v -keystore puzzle-quest-release.jks \
        -keyalg RSA -keysize 2048 -validity 10000 -alias puzzle-quest
```

You'll be prompted for:
- Keystore password
- Key password
- Key holder information (name, organization, etc.)

**Keep this keystore safe!** You'll need it for every future update.

---

## Step 3: Configure Gradle Signing

Update `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../puzzle-quest-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "puzzle-quest"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

---

## Step 4: Set Environment Variables (Local Testing)

Before building locally, set the keystore passwords:

```sh
export KEYSTORE_PASSWORD="your-keystore-password"
export KEY_PASSWORD="your-key-password"
```

Then build:

```sh
./gradlew bundleRelease
```

This produces: `app/build/outputs/bundle/release/app-release.aab`

---

## Step 5: Add GitHub Secrets (For CI/CD)

1. Go to your GitHub repository
2. Settings → Secrets and variables → Actions
3. Add these secrets:
   - `KEYSTORE_PASSWORD` — Your keystore password
   - `KEY_PASSWORD` — Your key password
   - `PLAY_STORE_SERVICE_ACCOUNT_JSON` (optional) — For automatic Play Store uploads

---

## Step 6: Create a Release

### Option A: Using Git Tags (Recommended)

```sh
# Update version in app/build.gradle.kts first
# Then create and push a tag

git tag v1.0.0
git push origin v1.0.0
```

This triggers the GitHub Actions `release-build.yml` workflow.

### Option B: Manual Workflow Dispatch

1. Go to GitHub Actions
2. Click "Release Build"
3. Click "Run workflow"
4. Select your branch and click "Run workflow"

---

## Step 7: Download Release Artifacts

1. Go to **Actions** tab
2. Click the latest **Release Build** workflow
3. Scroll down to **Artifacts**
4. Download `puzzle-quest-release-aab` (for Play Store)

---

## Step 8: Upload to Google Play Console

1. Go to [Google Play Console](https://play.google.com/console)
2. Select your app
3. Go to **Release** → **Production** (or **Internal testing** first)
4. Click **Create new release**
5. Upload the `app-release.aab` file
6. Fill in release notes
7. Review and publish

---

## Step 9: Monitor & Update

After publishing:

1. Check Play Store Console for crash reports
2. Monitor AdMob for ad performance
3. Gather user feedback
4. Plan next version

For the next release:

1. Increment `versionCode` in `app/build.gradle.kts`
2. Update `versionName` (e.g., 1.0.1, 1.1.0)
3. Make code changes
4. Commit and push
5. Create new tag: `git tag v1.0.1 && git push origin v1.0.1`
6. Repeat steps 7-8

---

## Troubleshooting

### Build fails with "Keystore not found"

Make sure:
- `puzzle-quest-release.jks` exists in the project root
- `KEYSTORE_PASSWORD` and `KEY_PASSWORD` environment variables are set

### APK/AAB not generated

Check the build output:

```sh
./gradlew bundleRelease --info
```

Look for errors in the output.

### Play Store upload fails

- Verify the AAB is signed correctly
- Check that version code is higher than the previous release
- Ensure the app ID matches what's registered on Play Store

### Ads not showing in release build

- Verify you replaced all test ad unit IDs
- Check that AdMob App ID is correct in `AndroidManifest.xml`
- Test on a real device (emulator ads may not work)
- Check AdMob Console for any approval issues

---

## Security Notes

- **Never commit the keystore file** to version control
- **Never commit passwords** to version control
- Use GitHub Secrets for sensitive data
- Keep the keystore file backed up in a secure location
- Do not share the keystore password

---

## Support

For AdMob issues: [Google AdMob Help](https://support.google.com/admob)
For Play Store issues: [Google Play Console Help](https://support.google.com/googleplay/android-developer)
