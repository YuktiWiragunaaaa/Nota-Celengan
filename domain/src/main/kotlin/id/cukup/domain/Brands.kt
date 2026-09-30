package id.cukup.domain

/**
 * Bank & e-wallet yang dikenal: dipakai untuk pilihan dompet, membaca notifikasi,
 * dan menebak dompet mana yang dipakai sebuah notifikasi.
 */
data class Brand(
    val key: String,
    val name: String,
    val kind: AccountKind,
    /** Paket aplikasi Android yang notifikasinya dibaca. */
    val packages: List<String>,
    /** Nama lain yang mungkin dipakai pengguna untuk dompetnya (huruf kecil). */
    val aliases: List<String>,
    /** Warna khas (ARGB). */
    val color: Long,
    /** Singkatan untuk ikon. */
    val mark: String,
)

object Brands {
    val all = listOf(
        Brand("bca", "BCA", AccountKind.BANK, listOf("com.bca.mybca.omni.android", "com.bca"), listOf("bca", "mybca", "klikbca"), 0xFF005EB8, "BCA"),
        Brand("mandiri", "Mandiri", AccountKind.BANK, listOf("id.bmri.livin"), listOf("mandiri", "livin"), 0xFF003D79, "MDR"),
        Brand("bri", "BRI", AccountKind.BANK, listOf("id.co.bri.brimo"), listOf("bri", "brimo"), 0xFF00529C, "BRI"),
        Brand("bni", "BNI", AccountKind.BANK, listOf("src.com.bni", "id.bni.wondr"), listOf("bni", "wondr"), 0xFFF15A23, "BNI"),
        Brand("krom", "Krom", AccountKind.BANK, listOf("com.krom.android"), listOf("krom"), 0xFF7B3FE4, "KR"),
        Brand("seabank", "SeaBank", AccountKind.BANK, listOf("id.co.bankbkemobile.digitalbank"), listOf("seabank", "sea bank"), 0xFFFF6A13, "SB"),
        Brand("jago", "Jago", AccountKind.BANK, listOf("com.jago.digitalBanking"), listOf("jago"), 0xFFF2A900, "JG"),
        Brand("blu", "blu", AccountKind.BANK, listOf("com.bcadigital.blu"), listOf("blu"), 0xFF00A3E0, "blu"),
        Brand("jenius", "Jenius", AccountKind.BANK, listOf("com.btpn.dc"), listOf("jenius", "btpn"), 0xFF00A9E0, "JN"),
        Brand("gopay", "GoPay", AccountKind.EWALLET, listOf("com.gojek.gopay", "com.gojek.app"), listOf("gopay", "gojek"), 0xFF00AED6, "GP"),
        Brand("ovo", "OVO", AccountKind.EWALLET, listOf("ovo.id"), listOf("ovo"), 0xFF4C3494, "OVO"),
        Brand("dana", "DANA", AccountKind.EWALLET, listOf("id.dana"), listOf("dana"), 0xFF118EEA, "DN"),
        Brand("shopeepay", "ShopeePay", AccountKind.EWALLET, listOf("com.shopeepay.id", "com.shopee.id"), listOf("shopeepay", "shopee"), 0xFFEE4D2D, "SP"),
        Brand("linkaja", "LinkAja", AccountKind.EWALLET, listOf("com.telkom.mwallet"), listOf("linkaja"), 0xFFE82529, "LA"),
        Brand("kredivo", "Kredivo", AccountKind.PAYLATER, listOf("com.finaccel.android"), listOf("kredivo"), 0xFFFF7A00, "KV"),
        Brand("akulaku", "Akulaku", AccountKind.PAYLATER, listOf("io.silvrr.installment"), listOf("akulaku"), 0xFFE6212A, "AK"),
    )

    private val byPackage = all.flatMap { b -> b.packages.map { it to b } }.toMap()

    fun forPackage(packageName: String): Brand? = byPackage[packageName]

    fun forName(name: String?): Brand? = all.firstOrNull { it.name == name }

    /** Brand yang namanya cocok dengan nama dompet ("BCA Tahapan" → BCA). */
    fun forAccountName(name: String): Brand? {
        val words = name.lowercase().split(' ', '-', '_', '.').filter { it.isNotBlank() }
        return all.firstOrNull { b -> b.aliases.any { alias -> alias in words || name.lowercase() == alias } }
    }

    /**
     * Dompet untuk notifikasi dari [packageName]:
     * 1. dompet yang pernah kamu tautkan sendiri ([links]),
     * 2. dompet yang namanya brand ini ("BCA", "Krom"),
     * 3. kalau hanya ada satu dompet sejenis (mis. satu rekening bank), dompet itu.
     * null bila tidak yakin.
     */
    fun accountFor(packageName: String, accounts: List<Account>, links: Map<String, Long>): Long? {
        val brand = forPackage(packageName) ?: return null
        links[brand.key]?.let { id -> if (accounts.any { it.id == id }) return id }
        accounts.firstOrNull { forAccountName(it.name)?.key == brand.key }?.let { return it.id }
        val sameKind = accounts.filter { it.kind == brand.kind && forAccountName(it.name) == null }
        return sameKind.singleOrNull()?.id
    }

    /** Dompet lain milikmu yang disebut di teks notifikasi ("Top up OVO", "ke KROM"). */
    fun mentionedAccount(text: String, accounts: List<Account>, except: Long?): Long? {
        val lower = text.lowercase()
        return accounts.firstOrNull { a ->
            a.id != except && forAccountName(a.name)?.aliases?.any { Regex("""\b${Regex.escape(it)}\b""").containsMatchIn(lower) } == true
        }?.id
    }
}
