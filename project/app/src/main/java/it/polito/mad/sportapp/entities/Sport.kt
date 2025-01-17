package it.polito.mad.sportapp.entities

class Sport (
    val id: String,
    val name: String,
    val emoji: String,
    val maxPlayers: Int
) {
    override fun toString(): String {
        return printWithEmoji(onTheLeft = false)
    }

    fun printWithEmoji(onTheLeft: Boolean = false): String {
        return if(onTheLeft) "$emoji  $name" else "$name  $emoji"
    }
}