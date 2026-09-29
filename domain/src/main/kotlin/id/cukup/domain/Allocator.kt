package id.cukup.domain

object Allocator {

    /**
     * Membagi [amount] ke pos-pos sesuai persentase dengan metode largest remainder,
     * sehingga jumlah hasil selalu tepat sama dengan [amount].
     */
    fun split(amount: Long, pockets: List<Pocket>): List<Pair<Pocket, Long>> {
        require(amount >= 0) { "amount must be >= 0" }
        val total = pockets.sumOf { it.percent }
        if (pockets.isEmpty() || total <= 0) return pockets.map { it to 0L }

        val parts = pockets.map { p ->
            val numerator = amount * p.percent
            Triple(p, numerator / total, numerator % total)
        }
        var leftover = amount - parts.sumOf { it.second }
        val extra = mutableMapOf<Long, Long>()
        val order = parts
            .filter { it.first.percent > 0 }
            .sortedWith(compareByDescending<Triple<Pocket, Long, Long>> { it.third }.thenBy { it.first.sortOrder })
            .map { it.first.id }
        var i = 0
        while (leftover > 0 && order.isNotEmpty()) {
            val id = order[i % order.size]
            extra[id] = (extra[id] ?: 0) + 1
            leftover--
            i++
        }
        return parts.map { (p, base, _) -> p to base + (extra[p.id] ?: 0) }
    }

    fun isValid(pockets: List<Pocket>): Boolean =
        pockets.isNotEmpty() && pockets.all { it.percent in 0..100 } && pockets.sumOf { it.percent } == 100
}
