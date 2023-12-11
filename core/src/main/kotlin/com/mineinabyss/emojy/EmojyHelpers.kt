package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.SPACE_PERMISSION
import com.mineinabyss.idofront.font.Space
import com.mineinabyss.idofront.messaging.broadcast
import com.mineinabyss.idofront.messaging.broadcastVal
import com.mineinabyss.idofront.messaging.logInfo
import com.mineinabyss.idofront.messaging.logSuccess
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player

fun Component.transform(player: Player?, insert: Boolean) = player?.let { replaceEmoteIds(it, insert) } ?: transformEmoteIds(insert)

private val spaceRegex: Regex = "(?<!\\\\):space_(-?\\d+):".toRegex()
private val escapedSpaceRegex: Regex = "\\\\(:space_(-?\\d+):)".toRegex()
private val colorableRegex: Regex = "\\|(c|colorable)".toRegex()
private val bitmapIndexRegex: Regex = "\\|([0-9]+)".toRegex()
//TODO Tags like rainbow and gradient, which split the text into multiple children, will break replacement below
// Above is due to Adventure-issue, nothing on our end for once. https://github.com/KyoriPowered/adventure/issues/872
// Find out why this is called 3 times
private fun Component.replaceEmoteIds(player: Player, insert: Boolean = true): Component {
    var msg = GlobalTranslator.render(this, player.locale())
    val serialized = msg.serialize()

    // Replace all unicodes found in default font with a random one
    // This is to prevent use of unicodes from the font the chat is in
    /*for (emote in emojy.emotes.filter { it.font == Key.key("default") }) emote.unicodes().forEach {
        val replacement =
            if (emote.checkPermission(player)) emote.formattedUnicode(insert = insert, colorable = false, bitmapIndex = 0)
            else "\\:${emote.id}:".miniMsg()
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .matchLiteral(it)
                .replacement(replacement)
                .build()
        )
    }*/

    for (emote in emojy.emotes) emote.baseRegex.findAll(serialized).forEach { match ->
        val colorable = colorableRegex in match.value
        val bitmapIndex = bitmapIndexRegex.find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: -1
        val replacement =
            if (emote.checkPermission(player)) emote.formattedUnicode(insert = insert, colorable = colorable, bitmapIndex = bitmapIndex)
            else "\\${match.value}".miniMsg()
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .match(emote.baseRegex.pattern)
                .replacement(replacement)
                .build()
        )
    }

    for (gif in emojy.gifs) gif.baseRegex.findAll(serialized).forEach { match ->
        val replacement = if (gif.checkPermission(player)) gif.formattedUnicode(insert = insert)
        else "\\:${gif.id}:".miniMsg()
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .match(gif.baseRegex.pattern)
                .replacement(replacement)
                .build()
        )
    }

    spaceRegex.findAll(serialized).forEach { match ->
        val space = match.groupValues[1].toIntOrNull() ?: return@forEach
        val replacement = if (player.hasPermission(SPACE_PERMISSION)) buildSpaceComponents(space) else "\\:space_$space:".miniMsg()

        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .match(spaceRegex.pattern)
                .replacement(replacement)
                .build()
        )
    }

    return msg
}

/**
 * Formats emote-ids in a component to their unicode representation.
 * This does format without a player context, but ignores escaped emote-ids.
 * This is because we handle with a player-context first, and escape that in-which should not be formatted.
 */
private fun Component.transformEmoteIds(insert: Boolean = true): Component {
    var msg = this
    val serialized = this.serialize()

    for (emote in emojy.emotes) {
        if (emote.id == "logo") continue
        emote.baseRegex.findAll(serialized).forEach { match ->

            val colorable = colorableRegex in match.value
            val bitmapIndex = bitmapIndexRegex.find(match.value)?.groupValues?.get(1)?.toIntOrNull() ?: -1

            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(emote.baseRegex.pattern)
                    .replacement(emote.formattedUnicode(insert = insert, colorable = colorable, bitmapIndex = bitmapIndex))
                    .build()
            )
        }

        emote.escapedRegex.findAll(serialized).forEach { match ->
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(emote.escapedRegex.pattern)
                    .replacement(match.value.removePrefix("\\"))
                    .build()
            )
        }
    }

    for (gif in emojy.gifs) {
        gif.baseRegex.findAll(serialized).forEach { match ->
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(gif.baseRegex.pattern)
                    .replacement(gif.formattedUnicode(insert = insert))
                    .build()
            )
        }

        gif.escapedRegex.findAll(serialized).forEach { match ->
            msg = msg.replaceText(
                TextReplacementConfig.builder()
                    .match(gif.escapedRegex.pattern)
                    .replacement(match.value.removePrefix("\\"))
                    .build()
            )
        }
    }

    spaceRegex.findAll(serialized).forEach { match ->
        val space = match.groupValues[1].toIntOrNull() ?: return@forEach
        val spaceRegex = "(?<!\\\\):space_(-?$space+):".toRegex()
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .match(spaceRegex.pattern)
                .replacement(buildSpaceComponents(space))
                .build()
        )
    }

    escapedSpaceRegex.findAll(serialized).forEach { match ->
        msg = msg.replaceText(
            TextReplacementConfig.builder()
                .match(match.value)
                .replacement(match.value.removePrefix("\\"))
                .build()
        )
    }

    return msg
}

private fun buildSpaceComponents(space: Int) =
    Component.textOfChildren(Component.text(Space.of(space)).font(emojyConfig.spaceFont))

fun Component.space(advance: Int = 3) = this.append(buildSpaceComponents(advance))
fun Component.space() = append(Component.text().content(" ").font(Key.key("default")))
