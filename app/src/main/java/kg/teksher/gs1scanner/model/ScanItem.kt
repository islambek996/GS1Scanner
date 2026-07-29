package kg.teksher.gs1scanner.model

data class ScanItem(
    val raw: String,
    val display: String,
    val timestamp: Long = System.currentTimeMillis()
)