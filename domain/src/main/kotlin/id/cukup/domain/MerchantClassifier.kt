package id.cukup.domain

/**
 * Menebak kategori dari nama merchant. Urutan:
 * 1. Aturan yang dipelajari dari koreksi pengguna (merchantKey → categoryId).
 * 2. Kata kunci merchant Indonesia → [Tag] → kategori dengan tag itu.
 * 3. Kategori "Lainnya" (tag OTHER), lalu kategori pertama.
 */
object MerchantClassifier {

    private val keywords: Map<Tag, List<String>> = mapOf(
        Tag.FOOD to listOf(
            "gofood", "grabfood", "shopeefood", "warung", "warteg", "resto", "rumah makan", "bakso", "mie", "nasi",
            "ayam", "sate", "padang", "indomaret", "alfamart", "alfamidi", "superindo", "hypermart", "hero", "lawson",
            "family mart", "familymart", "mcd", "mcdonald", "kfc", "burger king", "hokben", "solaria", "richeese",
            "pizza hut", "domino", "sayurbox", "astro", "bakery", "roti",
        ),
        Tag.FUN to listOf(
            "kopi", "coffee", "kenangan", "janji jiwa", "fore", "starbucks", "chatime", "mixue", "tomoro", "point coffee",
            "xxi", "cgv", "cinepolis", "netflix", "spotify", "youtube", "disney", "vidio", "steam", "playstation",
            "mobile legends", "moonton", "garena", "codashop", "unipin", "karaoke", "inul", "bar", "club",
        ),
        Tag.SHOPPING to listOf(
            "shopee", "tokopedia", "lazada", "blibli", "tiktok shop", "zalora", "uniqlo", "h&m", "zara", "erigo",
            "sociolla", "watsons", "guardian", "ace hardware", "ikea", "informa", "miniso", "gramedia",
        ),
        Tag.TRANSPORT to listOf(
            "gojek", "goride", "gocar", "grab", "maxim", "indrive", "bluebird", "krl", "commuter", "mrt", "lrt",
            "transjakarta", "kai", "tiket.com", "traveloka", "pertamina", "shell", "spbu", "parkir", "tol", "e-toll",
            "flazz", "brizzi",
        ),
        Tag.BILLS to listOf(
            "pln", "token listrik", "listrik", "pdam", "telkomsel", "indosat", "xl", "axis", "tri ", "smartfren",
            "indihome", "biznet", "first media", "myrepublic", "pulsa", "paket data", "kos", "kost", "sewa", "iuran",
        ),
        Tag.HEALTH to listOf("apotek", "kimia farma", "k24", "halodoc", "klinik", "rumah sakit", "bpjs", "alodokter"),
        Tag.DEBT to listOf(
            "kredivo", "akulaku", "paylater", "spaylater", "cicilan", "angsuran", "kartu kredit", "home credit",
            "adakami", "easycash", "kredit pintar",
        ),
        Tag.SAVINGS to listOf("bibit", "ajaib", "pluang", "tabungan", "deposito", "reksadana", "emas", "pegadaian"),
    )

    /** Normalisasi merchant untuk kunci aturan: huruf kecil, tanpa angka cabang dan tanda baca. */
    fun key(merchant: String): String =
        merchant.lowercase()
            .replace(Regex("""[^a-z& ]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    fun tagFor(merchant: String): Tag? {
        val m = " ${merchant.lowercase()} "
        if (m.isBlank()) return null
        return keywords.entries.firstOrNull { (_, words) -> words.any { m.contains(it) } }?.key
    }

    fun suggest(
        merchant: String,
        categories: List<Category>,
        learned: Map<String, Long>,
        kind: CategoryKind = CategoryKind.EXPENSE,
        isDebtPayment: Boolean = false,
    ): Category? {
        val pool = categories.filter { it.kind == kind }.sortedBy { it.sortOrder }
        if (pool.isEmpty()) return null
        learned[key(merchant)]?.let { id -> pool.firstOrNull { it.id == id }?.let { return it } }
        val tag = if (isDebtPayment) Tag.DEBT else tagFor(merchant)
        if (tag != null) pool.firstOrNull { it.tag == tag }?.let { return it }
        return pool.firstOrNull { it.tag == Tag.OTHER } ?: pool.first()
    }
}
