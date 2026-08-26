# Privacy Policy — Kids Explore

**Application:** Kids Explore (`com.kidsexplore.app`)
**Last updated:** 26 August 2026

<!--
  This is the source of truth. `privacy-policy.html` next to it is the same
  document laid out for hosting, and the two must be edited together — Play
  checks the hosted copy, and a repository copy that says something different
  is worse than no repository copy at all.

  One placeholder has to be filled in before this is published:
    * the URL it ends up at, which also goes in PRIVACY_POLICY_URL in
      app/src/main/java/com/kidsexplore/app/ui/screens/SettingsScreen.kt and in
      the Play Console listing.

  Everything else is a statement of fact about the app as it is built today. If
  the app ever gains a network call, an analytics library, an advertisement or a
  new permission, this document stops being true and must change in the same
  commit.
-->

## The short version

Kids Explore collects nothing. It has no internet access, shows no
advertisements, contains no analytics, and asks for no personal information from
anyone — child or adult. There is no account to create and nothing to log in to.

## What the app collects

Nothing.

Kids Explore does not collect, transmit, sell or share any personal information.
It cannot: the app declares no `INTERNET` permission and contains no networking
code of any kind, so there is nowhere for information to be sent, and no server
of ours for it to be sent to. Every photograph the app shows is packaged inside
the app itself and is displayed from the device.

The only permission in the installed app is
`android.permission.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. It is a
signature-level Android permission contributed automatically by a standard
Android support library, it grants no access to anything on the device, and it
involves no data at all. The app requests no camera, microphone, location,
contacts, storage, or advertising identifier.

## What the app stores on your device

Two things, in the app's own private storage, readable only by the app:

* **Which picture categories are switched on.** Up to fourteen short category
  identifiers, chosen by an adult in Parent Settings.
* **The parental gate's state.** A count of recent wrong answers and the time
  the resulting short lockout ends. These exist only so that repeatedly guessing
  at the gate is slowed down.

The chosen language is stored the same way, by the standard Android component
that handles per-app language settings.

None of this is personal information, and none of it leaves the device except as
described in the next section. Uninstalling the app deletes all of it.

## Backup

Android's built-in Auto Backup may copy the app's settings file — the category
identifiers and the two parental-gate numbers listed above — to the backup
associated with your own Google account, if you have device backup enabled. This
is a feature of the Android operating system, not of this app. That backup
belongs to you, is governed by
[Google's privacy policy](https://policies.google.com/privacy), and the
developer of Kids Explore has no access to it and cannot read it. Turning device
backup off in your Android settings stops it.

## Advertising, analytics and third parties

* **No advertisements.** The app shows none, of any kind.
* **No analytics or crash reporting.** No usage data, no events, no
  identifiers, no crash reports are gathered or sent.
* **No third-party SDKs that collect data.** The app is built only on Google's
  standard Android and Jetpack libraries, none of which are used here to gather
  or transmit anything.
* **No data sharing or sale.** There is no data to share or sell.
* **No in-app purchases.**

## Children's privacy

Kids Explore is designed for children and is part of Google Play's Families
programme. Because the app collects no data whatsoever, it collects no data from
children either.

* **COPPA (United States).** No personal information is collected from anyone,
  including children under 13, so there is nothing for which verifiable parental
  consent would be required and nothing held that a parent could ask to review
  or delete. There is no way for a child to communicate with anyone through this
  app, and nothing in it is transmitted anywhere.
* **GDPR and GDPR-K (EU/EEA and UK).** No personal data as defined by the GDPR
  is collected or processed, so there is no processing to hold a lawful basis
  for, no profiling, and no data subject rights request that could be answered
  with anything other than "we hold nothing about you". The app does not track
  users across apps or websites.
* **Outbound links.** The single link out of the app — the one that opened this
  document — is placed inside Parent Settings, behind a parental gate that a
  child is not expected to pass.

## Data deletion

There is no account to delete and no server-side data to erase, because none is
ever created. Uninstalling Kids Explore removes everything it stored on the
device. To remove the operating system's backup copy described above, turn off
or clear device backup in your Android settings.

## Changes to this policy

If the app ever changes in a way that affects this policy, this document will be
updated and the "Last updated" date at the top changed. The current version is
always available at the address linked from Parent Settings inside the app.

## Contact

<!-- Deliberately an issue tracker rather than an email address: this repository
     is public, the issues are readable by anyone, and a question answered there
     stays answered for the next person who asks it. Note that Play separately
     requires a developer contact email on the Console account itself; that is an
     account setting, not part of this document. -->
Questions about this policy: open an issue at
<https://github.com/ivancesar/kid-android-image-app/issues>.
