package ru.netology.nmedia.dt

data class Attachment(
    val url: String,
    val description: String?,
    val type: AttachmentType,
)
enum class AttachmentType {
    IMAGE, VIDEO, AUDIO
}
