package com.example.domain

data class RepoSlug(
    val owner: String,
    val repo: String,
) {
    val fullName: String get() = "$owner/$repo"
    val htmlUrl: String get() = "https://github.com/$owner/$repo"
}

object RepoInputParser {
    private val slugSegmentRegex = Regex("^[A-Za-z0-9_.-]+$")

    fun parse(rawInput: String): RepoSlug? {
        val trimmed = rawInput.trim()
            .removeSuffix("/")
            .removeSuffix(".git")
        if (trimmed.isEmpty()) return null

        // Handle SSH format: git@github.com:owner/repo
        if (trimmed.startsWith("git@github.com:", ignoreCase = true)) {
            val path = trimmed.substringAfter("git@github.com:")
            return parsePair(path)
        }

        // Handle full HTTPS/HTTP URLs
        if (trimmed.contains("github.com/", ignoreCase = true)) {
            val afterDomain = trimmed.substringAfter("github.com/", missingDelimiterValue = "")
            if (afterDomain.isBlank()) return null
            val parts = afterDomain.split("/").filter { it.isNotBlank() }
            if (parts.size < 2) return null
            return validateAndCreate(parts[0], parts[1])
        }

        // Handle owner/repo shorthand
        return parsePair(trimmed)
    }

    private fun parsePair(path: String): RepoSlug? {
        val parts = path.split("/").filter { it.isNotBlank() }
        if (parts.size != 2) return null
        return validateAndCreate(parts[0], parts[1])
    }

    private fun validateAndCreate(owner: String, repo: String): RepoSlug? {
        if (!slugSegmentRegex.matches(owner) || !slugSegmentRegex.matches(repo)) {
            return null
        }
        return RepoSlug(owner = owner, repo = repo)
    }
}
