package ru.netology.nmedia.util

fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> {
            String.format("%.1fM", count / 1_000_000.0).replace(",", ".")
        }

        count >= 100_000 -> {
            "${count / 1_000}K"
        }

        count >= 1_000 -> {
            String.format("%.1fK", count / 1_000.0).replace(",", ".")
        }

        else -> count.toString()
    }
}
