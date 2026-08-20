# Compose and AndroidX ship their own consumer rules, and the app has no
# reflection, no serialization and no JNI, so nothing extra needs keeping here.
# Kept as an explicit, empty file so the release build has somewhere obvious to
# add rules if that ever changes.
