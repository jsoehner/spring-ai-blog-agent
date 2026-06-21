package blog

default allow = true

# Words that are not allowed in topics
banned_words := {"bomb", "explosive", "weapon"}

# Deny if any banned word is a substring of the topic (case-insensitive)
allow = false {
    some word in banned_words
    contains(lower(input.topic), word)
}
