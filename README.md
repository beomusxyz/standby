![Standby showing an analog clock and monthly calendar](img/standby_hero.png)

# Standby

Standby turns an Android phone into a configurable clock, information display,
or dashboard while it charges. It runs as Android's screen saver and supports
HTML/CSS/JavaScript plugins alongside normal Android home-screen widgets.

The current build supports responsive portrait and landscape layouts, live
configuration in either orientation, Android widget hosting, explicit plugin
permissions and privacy notes, scheduled night mode, display dimming, and OLED
burn-in protection.

Version 0.1 is an early release for testing with friends. Expect rough edges and
please report anything that breaks.

## Install

Standby requires Android 13 or newer. It is distributed through GitHub and
Obtainium, not Google Play.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22org.phorophyte.standby%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fbeomusxyz%2Fstandby%22%2C%22author%22%3A%22beomusxyz%22%2C%22name%22%3A%22Standby%22%7D"><img src="img/badge_obtainium.png" alt="Get it on Obtainium" width="161"></a>

You can also download the APK from the [latest GitHub release](https://github.com/beomusxyz/standby/releases/latest).
Android may ask you to allow installs from the browser or file manager you used
to open it.

### Verify the app

GitHub releases for package `org.phorophyte.standby` are signed with this
certificate. Copy the fingerprint below into AppVerifier:

```text
80:D1:8F:84:95:13:21:11:76:B6:A4:2D:31:3F:7B:A5:FB:81:C7:FC:F0:6E:76:3D:BC:99:D2:BF:10:D4:1C:9D
```

[AppVerifier](https://github.com/soupslurpr/AppVerifier) will match that
fingerprint against the signing certificate of the installed app.

Each GitHub release also includes the SHA-256 checksum of its APK. The checksum
proves that the downloaded file matches that release; the certificate fingerprint
above proves who signed it.

## Get started

1. Open Standby and tap **+** to choose a full-screen layout, a split layout,
   an included plugin, or an Android widget.
2. Tap the pencil to change the active plugin's options.
3. Tap the gear to configure night mode, OLED protection, weather, and the local
   plugin uploader.
4. Rotate the phone to check both layouts. The app preview uses the same renderer
   as the screen saver.
5. Open Android **Settings → Display → Screen saver**, choose **Standby**, and
   select when it should start. The exact menu name varies between Android builds.
6. Put the phone on charge and let Android start the screen saver, or use the
   system's **Start now** button if it provides one.

Swipe sideways to move between configured pages. Android keeps its normal lock
screen gestures: swipe up to unlock and swipe down for system controls.

## Features

- Full-screen and split pages that reflow between landscape and portrait.
- Responsive plugins written with ordinary HTML, CSS, and JavaScript.
- Normal Android home-screen widgets, including their configuration activities.
- Live layout and plugin editing in portrait or landscape.
- Scheduled night mode with very low brightness and stronger OLED protection.
- Configurable OLED pixel masking during normal use.
- Per-plugin permissions, provider access, host allowlists, and privacy notes
  shown before import.
- Weather through Open-Meteo, with an optional city or coarse device location.
- A local, PIN-protected uploader for moving plugin ZIPs onto the phone.
- No accounts, analytics, advertising, Firebase, or Google Play Services.

## Plugins

Ready-to-import plugin ZIPs are in [`plugins/build/`](plugins/build/). Import one
from the layout screen, or enable the local uploader in Settings and send it from
another device on the same network.

The [plugin development guide](DOCS.md) documents manifests, responsive layouts,
customization controls, the native bridge, privacy notes, and network restrictions.
Plugin source is under [`plugins/src/`](plugins/src/).

Plugins are code. Read the import screen and install only ones you trust. Standby
blocks undeclared hosts, but a plugin can use the permissions and providers it
declares. The [privacy document](PRIVACY.md) describes the app, included plugins,
Android widgets, backups, weather, and the uploader in detail.

## Permissions

- **Internet:** weather, the local uploader, and network access explicitly
  declared by an imported plugin.
- **Vibrate:** a short response when paging between screens.
- **Approximate location:** optional local weather when you enable it.
- **Local network:** lets the uploader accept a connection on Android 17 and
  newer.
- **Query installed apps:** finds apps that provide Android widgets. This is
  currently declared broadly and is being tested for removal before 1.0.

## Screenshots

### Layouts

![Side-by-side widget layout](img/half_plugin.png)

![Full-screen widget layout](img/fullscreen_plugin.png)

### Plugins and Android widgets

![Battery statistics plugin](img/battery_stats.png)

![Weather and clock plugin](img/clock_weather.png)

![Android clock widget](img/pixel_clock.png)

![GitHub contributions Android widget](img/app_widget_example.png)

![Two clocks in a split layout](img/side_clocks.png)

### Controls

![Plugin customization controls](img/plugin_customization.png)

![Night mode settings](img/night_mode_setting.png)

![OLED pixel protection](img/oled_burn_example.png)

## Build

Clone the repository and build a debug APK with the included Gradle wrapper:

```bash
git clone https://github.com/beomusxyz/standby.git
cd standby
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The project
requires Android SDK 37 and uses a Gradle-managed JDK 21 toolchain.

Run the local checks with:

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

## Credits and licence

Standby was forked from [Haxintosh/standby](https://github.com/Haxintosh/standby).
The original project established the plugin system, layout editor, uploader,
and OLED-protection work this fork builds on.

The project is available under the [MIT licence](LICENSE).
