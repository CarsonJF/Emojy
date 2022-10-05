package com.mineinabyss.emojy

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.mineinabyss.idofront.config.IdofrontConfig
import com.mineinabyss.idofront.messaging.miniMsg
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player

var emojyConfig = EmojyConfig.data
object EmojyConfig : IdofrontConfig<EmojyConfig.EmojyConfig>(emojy, EmojyConfig.serializer()) {
    const val PRIVATE_USE_FIRST = 57344

    @Serializable
    data class EmojyConfig(
        val defaultNamespace: String = "emotes",
        val defaultFolder: String = "emotes",
        val defaultFont: String = "emotes",
        val defaultHeight: Int = 7,
        val defaultAscent: Int = 7,
        val requirePermissions: Boolean = true,
        val generateResourcePack: Boolean = true,
        val emotes: Set<Emote> = emptySet()
        //val gifs: Set<Gif>
    )

    @Serializable
    data class Emote(
        val id: String,
        val font: String = emojyConfig.defaultFont,
        val texture: String = "${emojyConfig.defaultNamespace}:textures/${emojyConfig.defaultFolder}/${id}.png",
        val height: Int = emojyConfig.defaultHeight,
        val ascent: Int = emojyConfig.defaultAscent,
        val permission: String = "emojy.emote.$id"
    ) {
        // Beginning of Private Use Area \uE000 -> uF8FF
        // Option: (Character.toCodePoint('\uE000', '\uFF8F')/37 + getIndex())
        fun getUnicode(): Char =
            Character.toChars(PRIVATE_USE_FIRST + emojyConfig.emotes.filter { it.font == font }.indexOf(this)).first()

        fun getFont() = Key.key(getNamespace(), font)
        fun getNamespace() = texture.substringBefore(":")
        fun getImage() = texture.substringAfterLast("/")
        fun getImagePath() = texture.substringAfter(":")
        fun toJson(): JsonObject {
            val output = JsonObject()
            val chars = JsonArray()
            output.addProperty("type", "bitmap")
            output.addProperty("file", texture)
            output.addProperty("ascent", ascent)
            output.addProperty("height", height)
            chars.add(getUnicode())
            output.add("chars", chars)
            return output
        }
        fun checkPermission(player: Player?) =
            !emojyConfig.requirePermissions || player == null || player.hasPermission(permission) || player.hasPermission("emojy.font.${font}")

        // TODO Change this to miniMsg(TagResolver) when Idofront is updated
        fun getFormattedUnicode(splitter: String = ""): Component {
            val component = getUnicode().toString().miniMsg().mergeStyle(
                "".miniMsg().font(getFont()).color(NamedTextColor.WHITE).insertion(":$id:")
                    .hoverEvent(
                        HoverEvent.hoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            ("<red>Type <i>:$id:</i> or <i>Shift + Click</i> this to use this emote").miniMsg()
                        )
                    )
            )
            return if (emojyConfig.emotes.indexOf(this) == emojyConfig.emotes.size - 1) component
            else component.append("<font:default><white>$splitter</white></font>".miniMsg())
        }

        private val emojyTagResolver: TagResolver
            get() {
                val tagResolver = TagResolver.builder()
                emojyConfig.emotes.forEach { emote ->
                    Placeholder.component("emojy_${emote.id}", emote.getFormattedUnicode())
                }
                return tagResolver.build()
            }
    }


    /*@Serializable
    data class Gif(
        val id: String,
        val frameCount: Int,

    )*/
}
