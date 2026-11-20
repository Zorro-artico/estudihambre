package com.example.domain.usecase

object FoodEmojiMapper {

    private data class FoodRule(val keywords: List<String>, val emoji: String)

    private val rules = listOf(
        FoodRule(listOf("maruchan", "instantanea", "instantánea", "ramen", "sopita", "cup noodles", "nissin"), "🍜"),
        FoodRule(listOf("torta", "bolillo", "telera", "cemita", "pambazo"), "🥖"),
        FoodRule(listOf("hamburguesa", "burger", "whopper", "big mac"), "🍔"),
        FoodRule(listOf("taco", "tacos", "pastor", "suadero", "campechano", "birria"), "🌮"),
        FoodRule(listOf("burrito", "wrap"), "🌯"),
        FoodRule(listOf("quesadilla", "sincronizada", "gringa", "queso"), "🧀"),
        FoodRule(listOf("pizza", "pepperoni"), "🍕"),
        FoodRule(listOf("hot dog", "hotdog", "jocho"), "🌭"),
        FoodRule(listOf("sandwich", "sándwich", "club", "baguette"), "🥪"),
        FoodRule(listOf("chilaquil", "enchilada", "enfrijolada", "mollete", "sope", "gordita", "tlacoyo"), "🌶️"),
        FoodRule(listOf("tamal", "tamales"), "🫔"),
        FoodRule(listOf("huevo", "huevos", "omelette", "revuelto", "estrellado"), "🍳"),
        FoodRule(listOf("pollo", "pechuga", "milanesa", "alitas", "nuggets", "rostizado"), "🍗"),
        FoodRule(listOf("atun", "atún", "pescado", "salmon", "salmón", "sardina", "mojarra", "ceviche"), "🐟"),
        FoodRule(listOf("bistec", "carne", "arrachera", "res", "milanesa de res", "cecina", "carnitas"), "🥩"),
        FoodRule(listOf("arroz", "sushi", "onigiri"), "🍚"),
        FoodRule(listOf("pasta", "espagueti", "spaghetti", "macarrones", "lasagna"), "🍝"),
        FoodRule(listOf("frijol", "frijoles", "lentejas", "garbanzo"), "🫘"),
        FoodRule(listOf("avena", "cereal", "granola", "caldo", "sopa", "pozole", "consome", "consomé"), "🥣"),
        FoodRule(listOf("ensalada", "verdura", "lechuga", "nopales", "brocoli", "brócoli"), "🥗"),
        FoodRule(listOf("cafe", "café", "americano", "capuchino", "espresso", "latte", "te ", "té "), "☕"),
        FoodRule(listOf("licuado", "batido", "proteina", "proteína", "malteada", "smoothie", "jugo", "refresco", "coca"), "🥤"),
        FoodRule(listOf("yogurt", "yogur", "leche", "chocomilk"), "🥛"),
        FoodRule(listOf("pan dulce", "concha", "dona", "cuernito", "croissant", "galleta", "hotcake", "waffle"), "🥐"),
        FoodRule(listOf("manzana", "platano", "plátano", "fruta", "fresa", "mango", "papaya", "naranja"), "🍎"),
        FoodRule(listOf("papas", "fritas", "sabritas", "doritos", "botana"), "🍟")
    )

    private fun startsWithEmoji(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val firstCodePoint = trimmed.codePointAt(0)
        return firstCodePoint in 0x1F300..0x1FAFF || firstCodePoint in 0x2600..0x27BF
    }

    fun findEmoji(title: String, description: String = ""): String? {
        val combined = "${title.lowercase()} ${description.lowercase()}"
        return rules.firstOrNull { rule ->
            rule.keywords.any { kw -> combined.contains(kw) }
        }?.emoji
    }

    fun formatTitleWithEmoji(title: String, description: String = ""): String {
        val clean = title.trim()
        if (clean.isBlank()) return clean
        if (startsWithEmoji(clean)) return clean
        val emoji = findEmoji(clean, description) ?: return clean
        if (clean.contains(emoji)) return clean
        return "$emoji $clean"
    }
}
