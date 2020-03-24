package dev.rhovas.compiler.lexer

data class Token(val type: TokenType, val literal: String)

enum class TokenType {
    WHITESPACE,
    IDENTIFIER,
    INTEGER,
    DECIMAL,
    CHARACTER,
    STRING,
    OPERATOR
}
