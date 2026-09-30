# Room, Hilt, Compose dan Glance membawa aturan keep masing-masing.
# Enum domain disimpan sebagai nama di database.
-keepclassmembers enum id.cukup.domain.** { *; }

# Glance membuat ActionCallback widget lewat nama kelasnya.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
