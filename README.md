# 🧠 RexPrefs

**RexPrefs** is a modern, lightweight, and powerful SharedPreferences wrapper built for Android. It provides a clean API, lifecycle awareness, object serialization, async writes, and migration support — all while being fast and type-safe.

---

## 🚀 Features

- **Secure Storage**: Uses Android's `EncryptedSharedPreferences` when possible
- **Auto Migration**: Easy migration from existing `SharedPreferences`
- **In-Memory Caching**: Optional caching for faster reads
- **Fallback Mechanism**: Falls back to regular `SharedPreferences` if encryption fails
- **Clean API**: Simple methods for all basic types
- **Property Delegates**: Kotlin-friendly property delegation
- **Change Observers**: Standard `SharedPreferences` change listeners
- **Thread Safety**: Singleton instances managed per preference name

### 🧠 Performance:
- Bulk operations with `putAll()`
- Async operations with `putStringAsync()` etc.
- Optimized memory cache with size limits

### 🧬 Custom Serialization:
- GSON-based and Kotlinx serialization supported
- Type-safe object storage and retrieval

### 🔁 Lifecycle Awareness:
- Buffered writes for stopped components
- Auto flush on lifecycle resume
- Prevents unnecessary disk I/O

---

## 📦 Installation

### Gradle (Groovy)
In `settings.gradle` or `build.gradle` (project level):
```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

In `build.gradle` (app level):
```groovy
implementation 'com.github.ilokeshmeena:RexPrefs:v1.0.0'
```

### Gradle (Kotlin DSL)
In `settings.gradle.kts` or `build.gradle.kts` (project level):
```kotlin
repositories {
    maven("https://jitpack.io")
}
```

In `build.gradle.kts` (app level):
```kotlin
implementation("com.github.ilokeshmeena:RexPrefs:v1.0.0")
```

---

## 🧑‍💻 Usage Examples

### 1. Basic Usage
```kotlin
val rexPrefs = RexPrefs.getInstance(context)

rexPrefs.putString("user_email", "user@example.com")
rexPrefs.putInt("login_count", 5)
rexPrefs.putBoolean("dark_mode", true)

val email = rexPrefs.getString("user_email")
val count = rexPrefs.getInt("login_count")
val isDarkMode = rexPrefs.getBoolean("dark_mode")
```

### 2. Migration from SharedPreferences
```kotlin
val oldPrefs = context.getSharedPreferences("old_prefs", Context.MODE_PRIVATE)
val rexPrefs = RexPrefs.getInstance(context, migrationFromPrefs = oldPrefs)
```

### 3. Using Property Delegates
```kotlin
class UserSettings(private val rexPrefs: RexPrefs) {
    var username by rexPrefs.stringPref("username")
    var loginCount by rexPrefs.intPref("login_count")
    var isPremium by rexPrefs.booleanPref("is_premium", false)
}

val settings = UserSettings(rexPrefs)
settings.username = "new_user"
val currentUsername = settings.username
```

### 4. Observing Changes
```kotlin
val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
    if (key == "dark_mode") updateTheme()
}

rexPrefs.registerListener(listener)
rexPrefs.unregisterListener(listener) // Clean up
```

### 5. Non-blocking Writes
```kotlin
rexPrefs.putStringAsync("async_key", "value_written_in_background")
```

### 6. Bulk Operations
```kotlin
val userData = mapOf(
    "username" to "johndoe",
    "user_id" to 12345,
    "is_premium" to true,
    "last_login" to System.currentTimeMillis()
)

rexPrefs.putAll(userData)
```

### 7. Custom Object Serialization
```kotlin
data class UserProfile(val name: String, val email: String, val age: Int)

val profile = UserProfile("John", "john@example.com", 30)
rexPrefs.putObject("user_profile", profile)

val savedProfile = rexPrefs.getObject<UserProfile>("user_profile")
```

### 8. Lifecycle-Aware Usage
```kotlin
class MyActivity : AppCompatActivity() {
    private lateinit var lifecycleAwarePrefs: LifecycleAwareRexPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rexPrefs = RexPrefs.getInstance(this)
        lifecycleAwarePrefs = rexPrefs.withLifecycle(lifecycle)

        lifecycleAwarePrefs.putString("activity_state", "created")
        lifecycleAwarePrefs.putInt("create_count", 1)
    }
}
```

---

## ❤️ Made with Love

This library was crafted with care and passion by [**@ilokeshmeena**](https://github.com/ilokeshmeena).  
Feel free to contribute, report issues, or star the repo if you find it useful!

---
