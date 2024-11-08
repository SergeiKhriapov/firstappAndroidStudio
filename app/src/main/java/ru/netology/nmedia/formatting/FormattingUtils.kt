package ru.netology.nmedia.formatting

fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> {
            String.format("%.1fM", count / 1_000_000.0)
        }

        count >= 10_000 -> {
            String.format("%.1fK", count / 1_000.0)
        }

        count >= 1_100 -> {
            String.format("%.1fK", count / 1_000.0)
        }

        count >= 1_000 -> {
            "1K"
        }

        else -> count.toString()
    }
}
