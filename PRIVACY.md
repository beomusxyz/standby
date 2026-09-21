# Privacy

Standby has no accounts, no analytics, no crash reporting and no advertising. Nothing here is a promise about intent; it is a description of what the code does, and you can check all of it.

<!-- Generated from PrivacyPolicy.kt. Edit that, not this file. -->

## What leaves this device

Four hosts, and only for weather. Nothing else in the app makes a network request of its own.

| Host | What it receives | When |
|---|---|---|
| api.open-meteo.com | A latitude and longitude | Hourly while the app is open, and when you press refresh |
| geocoding-api.open-meteo.com | The text you type into the city search | While you are typing, once you pause |
| ipapi.co | Your IP address, by the fact of connecting | Only when no location has been set yet |
| openweathermap.org | Which weather icon is being fetched | Once per weather condition, then cached on disk |

No API keys, no account, no device identifier and no custom headers are sent to any of them. Open-Meteo is the forecast source because it needs no key and no sign-up.


## Location

Off unless you turn it on. Weather uses a city you pick, and the app asks for location only when you enable "Use my approximate location".

- Coarse location only. A forecast is for a whole city, so a street address would be precision with no use.
- Your coordinates go to Open-Meteo to fetch a forecast. They go nowhere else.
- Turning location on does not overwrite the city you picked. It comes back when you turn it off.
- Turning the feature off is enough; you do not have to revoke the permission as well.

One caveat worth stating plainly: to turn coordinates into a city name the app asks Android's own geocoder. On most stock devices that is serviced by a Google component and may involve a network request this app cannot see or control. On a de-Googled device it usually returns nothing, and you get "Current Location" instead of a name.


## Permissions

| Permission | What it is for | If you deny it |
|---|---|---|
| Internet | Weather, the plugin upload server, and any network a plugin is allowed | Weather stops updating |
| Vibrate | A short buzz when you swipe between pages | No buzz |
| Approximate location | Weather for where you are, if you ask for it | Nothing; the feature is off by default |
| Nearby devices | Lets the plugin upload server accept a connection from your own network | The upload server runs but nothing can reach it |
| Query all packages | Lists which of your apps offer home screen widgets, so the picker can show them | The widget picker may be empty |

"Nearby devices" is Android's name for local network access from version 17 onward. It has nothing to do with Bluetooth here and the app does not scan for anything.

"Query all packages" is the broadest permission in the list and it is worth being honest about it: it is there so third-party widgets can be offered, it reads app names and icons to draw that list, and it may in fact be redundant now that the manifest also declares a narrower widget query. That has not been verified on a real device yet.


## Plugins

Plugins are not part of the app. They are zip files you choose to import, and each one declares which hosts it may reach. The app blocks everything outside that list, and the import dialog shows you the list first.

| Plugin | Reaches |
|---|---|
| Stocks | www.alphavantage.co, for share prices |
| Weather | Nothing. It reads the forecast the app already fetched |
| All five clocks | Nothing |
| Battery | Nothing |
| Calendar | Nothing. It draws a month from the date, and cannot read your calendar |
| Bad Apple | Nothing |

A plugin you install yourself is third-party code. If it declares the weather provider it can read the cached forecast, which contains the coordinates the forecast is for, and if it also declares a host it could send that somewhere. None of the bundled plugins do either. Read the permissions and the host list the import dialog shows you.


## What is stored on this device

- Your settings, the city you chose, and its coordinates.
- Imported plugins, their settings, and your page layouts.
- Coordinates and the city name the app resolved, if you use location.
- The most recent forecast, and the weather icons that go with it.
- Anything a plugin saves for itself in its own browser storage.

All of it is private to the app and none of it is transmitted. There are no credentials to store: the upload server's PIN exists only in memory and is new every time the server starts.


## Backup

If you use Android backup, or Seedvault on GrapheneOS, the app opts in selectively rather than all or nothing.

- Backed up: your settings, your plugins, your layouts, and the city you picked.
- Not backed up: resolved coordinates, the resolved city name, the cached forecast, and whether the upload server was running.

Those are in a separate preferences file specifically so they can be excluded, because Android's backup rules can exclude a whole file but not individual settings within one. A restored phone starts with the upload server off.


## The plugin upload server

A way to get plugin zips onto your phone from a browser on the same network. It is off by default and does nothing until you switch it on.

- It only ever receives. It makes no outbound request of any kind.
- A six-digit PIN, newly generated from a cryptographic random source each time it starts, and compared in a way that does not leak it by timing.
- Five wrong PINs locks it for a minute.
- Uploads are capped at 50 MB.
- It refuses requests that do not address it by IP, which stops a website you are browsing from reaching it through your DNS.
- It keeps listening with the screen off until you turn it off.

While it is running, the address and PIN are shown on screen, so anyone who can see your phone can see them.


## Home screen widgets

Widgets you add from other apps run their own code with their own permissions and their own network access. This app neither mediates nor sees any of it, and whatever that app's privacy policy says is what applies.

The reverse does not happen: a hosted widget cannot read this app's settings, your plugins, your location or anything else of yours. It is handed only its own size.


## Warning for GrapheneOS Users

GrapheneOS may tell you Standby tried to perform DCL (Dynamic Code Loading) via storage. That is expected and it is the plugin system: plugins are HTML and JavaScript read from the app's own storage and run in a web view.

It is worth understanding rather than dismissing. A plugin is code you chose to install, and it runs with whatever the app grants it. Install ones you trust, and read what the import dialog tells you.


## What there is none of

- No analytics or usage tracking of any kind.
- No crash reporting.
- No advertising and no advertising identifiers.
- No accounts, no sign-in, no sync.
- No Google Play Services, no Firebase.

The last one is enforced rather than promised: the build fails outright if any dependency drags in Play Services, Firebase or Google's data transport libraries, so it cannot creep back in through something else's update.
