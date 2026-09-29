package id.cukup.domain

/**
 * Menebak pos dari nama merchant. Urutan:
 * 1. Aturan yang dipelajari dari koreksi pengguna (merchantKey → pocketId).
 * 2. Kata kunci merchant Indonesia → [PocketTag] → pos dengan tag paling cocok.
 * 3. Pos belanja (SPEND) pertama.
 */
object MerchantClassifier {

    private val keywords: Map<PocketTag, List<String>> = mapOf(
        PocketTag.FOOD to listOf(
            "gofood", "grabfood", "shopeefood", "warung", "warteg", "resto", "rumah makan", "bakso", "mie", "nasi",
            "ayam", "sate", "padang", "indomaret", "alfamart", "alfamidi", "superindo", "hypermart", "hero", "lawson",
            "family mart", "familymart", "mcd", "mcdonald", "kfc", "burger king", "hokben", "solaria", "richeese",
            "pizza hut", "domino", "sayurbox", "astro", "bakery", "roti",
        ),
        PocketTag.FUN to listOf(
            "kopi", "coffee", "kenangan", "janji jiwa", "fore", "starbucks", "chatime", "mixue", "tomoro", "point coffee",
            "xxi", "cgv", "cinepolis", "netflix", "spotify", "youtube", "disney", "vidio", "steam", "playstation",
            "mobile legends", "moonton", "garena", "codashop", "unipin", "karaoke", "inul", "bar", "club",
        ),
        PocketTag.SHOPPING to listOf(
            "shopee", "tokopedia", "lazada", "blibli", "tiktok shop", "zalora", "uniqlo", "h&m", "zara", "erigo",
            "sociolla", "watsons", "guardian", "ace hardware", "ikea", "informa", "miniso", "gramedia",
        ),
        PocketTag.TRANSPORT to listOf(
            "gojek", "goride", "gocar", "grab", "maxim", "indrive", "bluebird", "krl", "commuter", "mrt", "lrt",
            "transjakarta", "kai", "tiket.com", "traveloka", "pertamina", "shell", "spbu", "parkir", "tol", "e-toll",
            "flazz", "brizzi",
        ),
        PocketTag.BILLS to listOf(
            "pln", "token listrik", "listrik", "pdam", "bpjs", "telkomsel", "indosat", "xl", "axis", "tri ", "smartfren",
            "indihome", "biznet", "first media", "myrepublic", "pulsa", "paket data", "kos", "kost", "sewa", "iuran",
        ),
        PocketTag.DEBT to listOf(
            "kredivo", "akulaku", "paylater", "spaylater", "cicilan", "angsuran", "kartu kredit", "home credit",
            "adakami", "easycash", "kredit pintar",
        ),
        PocketTag.SAVINGS to listOf("bibit", "ajaib", "pluang", "tabungan", "deposito", "reksadana", "emas", "pegadaian"),
    )

    /** Tag mana yang boleh dipakai sebagai cadangan bila pos dengan tag persis tidak ada. */
    private val fallbacks: Map<PocketTag, List<PocketTag>> = mapOf(
        PocketTag.FOOD to listOf(PocketTag.NEEDS),
        PocketTag.BILLS to listOf(PocketTag.NEEDS),
        PocketTag.TRANSPORT to listOf(PocketTag.NEEDS),
        PocketTag.FUN to listOf(PocketTag.WANTS, PocketTag.SHOPPING),
        PocketTag.SHOPPING to listOf(PocketTag.WANTS, PocketTag.FUN),
        PocketTag.DEBT to listOf(PocketTag.NEEDS),
        PocketTag.SAVINGS to listOf(),
    )

    /** Normalisasi merchant untuk kunci aturan: huruf kecil, tanpa angka cabang dan tanda baca. */
    fun key(merchant: String): String =
        merchant.lowercase()
            .replace(Regex("""[^a-z& ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    fun tagFor(merchant: String): PocketTag? {
        val m = " ${merchant.lowercase()} "
        if (m.isBlank()) return null
        return keywords.entries.firstOrNull { (_, words) -> words.any { m.contains(it) } }?.key
    }

    fun suggest(
        merchant: String,
        pockets: List<Pocket>,
        learned: Map<String, Long>,
        isPaylaterPayment: Boolean = false,
    ): Pocket? {
        if (pockets.isEmpty()) return null
        learned[key(merchant)]?.let { id -> pockets.firstOrNull { it.id == id }?.let { return it } }

        val tag = if (isPaylaterPayment) PocketTag.DEBT else tagFor(merchant)
        if (tag != null) {
            pockets.firstOrNull { it.tag == tag }?.let { return it }
            for (fb in fallbacks[tag].orEmpty()) pockets.firstOrNull { it.tag == fb }?.let { return it }
            if (tag == PocketTag.DEBT) pockets.firstOrNull { it.kind == PocketKind.DEBT }?.let { return it }
        }
        return pockets.sortedBy { it.sortOrder }.firstOrNull { it.kind == PocketKind.SPEND } ?: pockets.first()
    }
}
