package kg.teksher.gs1scanner.utils

class DuplicateFilter(
    private val timeout: Long = 3000
) {

    private val cache = HashMap<String, Long>()

    fun isDuplicate(code: String): Boolean {
        val now = System.currentTimeMillis()

        cache.entries.removeIf { now - it.value > timeout }

        val last = cache[code]

        return if (last == null) {
            cache[code] = now
            false
        } else {
            true
        }
    }

    fun clear() {
        cache.clear()
    }
}