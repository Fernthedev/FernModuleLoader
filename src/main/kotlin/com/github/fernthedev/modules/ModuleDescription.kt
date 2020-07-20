package com.github.fernthedev.modules

import lombok.AllArgsConstructor
import lombok.Getter

@Getter
@AllArgsConstructor
data class ModuleDescription(
    val authors: Array<String>,

    /**
     * By default, use the jar's manifest version.
     */
    val version: String,
    val name: String,
    val depend: Array<String>?,
    val softDepend: Array<String>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModuleDescription

        if (!authors.contentEquals(other.authors)) return false
        if (version != other.version) return false
        if (name != other.name) return false
        if (depend != null) {
            if (other.depend == null) return false
            if (!depend.contentEquals(other.depend)) return false
        } else if (other.depend != null) return false
        if (softDepend != null) {
            if (other.softDepend == null) return false
            if (!softDepend.contentEquals(other.softDepend)) return false
        } else if (other.softDepend != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = authors.contentHashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (depend?.contentHashCode() ?: 0)
        result = 31 * result + (softDepend?.contentHashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ModuleDescription(authors=${authors.contentToString()}, version='$version', name='$name', depend=${depend?.contentToString()}, softDepend=${softDepend?.contentToString()})"
    }


}