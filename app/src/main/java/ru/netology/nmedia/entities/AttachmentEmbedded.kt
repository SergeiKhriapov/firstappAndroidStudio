package ru.netology.nmedia.entities

import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType

data class AttachmentEmbedded(
    val url: String,
    val type: AttachmentType,
) {
    companion object {
        fun fromDto(dto: Attachment): AttachmentEmbedded =
            AttachmentEmbedded(
                url = dto.url,
                type = dto.type
            )
    }
    fun toDto(): Attachment = Attachment(url = url, type = type)
}
