import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.rexvit.rexprefs.RexPrefs

class LifecycleAwareRexPrefs(
    private val rexPrefs: RexPrefs,
    lifecycle: Lifecycle
) : LifecycleEventObserver {

    private val pendingWrites = mutableMapOf<String, Any>()
    private var isStarted = false

    init {
        lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                isStarted = true
                flushPendingWrites()
            }
            Lifecycle.Event.ON_STOP -> {
                isStarted = false
            }
            else -> {}
        }
    }

    fun putString(key: String, value: String) {
        if (isStarted) {
            rexPrefs.putString(key, value)
        } else {
            pendingWrites[key] = value
        }
    }

    private fun flushPendingWrites() {
        if (pendingWrites.isNotEmpty()) {
            rexPrefs.putAll(pendingWrites)
            pendingWrites.clear()
        }
    }

    fun clearPendingWrites() {
        pendingWrites.clear()
    }

    companion object {
        fun from(rexPrefs: RexPrefs, lifecycle: Lifecycle): LifecycleAwareRexPrefs {
            return LifecycleAwareRexPrefs(rexPrefs, lifecycle)
        }
    }
}
