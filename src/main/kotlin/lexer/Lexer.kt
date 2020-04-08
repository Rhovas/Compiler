package dev.rhovas.compiler.lexer

fun lex(chars: CharSequence): Sequence<Token> {
    return Lexer(CharStream(chars)).tokens
}

private class Lexer(private val chars: CharStream) {

    val tokens = generateSequence { chars[1]?.let { lexToken() } }

    private fun lexToken(): Token {
        assert(chars[1] != null)
        val char = chars[1]!!
        val type = when {
            char.isWhitespace() -> lexWhitespace()
            char.isLetter() -> lexIdentifier()
            char.isDigit() -> lexNumber()
            char == '\'' -> lexCharacter()
            char == '\"' -> lexString()
            else -> lexOperator()
        }
        return Token(type, chars.emit())
    }

    private fun lexWhitespace(): TokenType {
        while (match(Character::isWhitespace)) {}
        return TokenType.WHITESPACE
    }

    private fun lexIdentifier(): TokenType {
        assert(match(Character::isLetter))
        while (match(Character::isLetterOrDigit)) {}
        return TokenType.IDENTIFIER
    }

    private fun lexNumber(): TokenType {
        assert(match(Character::isDigit))
        while (match(Character::isDigit)) {}
        if (chars[1] == '.' && chars[2]?.isDigit() == true) {
            assert(match { it == '.' })
            while (match(Character::isDigit)) {}
            return TokenType.DECIMAL
        }
        return TokenType.INTEGER
    }

    private fun lexCharacter(): TokenType {
        assert(match { it == '\'' })
        assert(match { it != '\'' })
        if (chars[0] == '\\') {
            assert(match { it in listOf('t', 'b', 'n', 'r', '\'', '\"', '\\', '\$') })
        }
        assert(match { it == '\'' })
        return TokenType.CHARACTER
    }

    private fun lexString(): TokenType {
        assert(match { it == '\"' })
        while (match { it != '\"' }) {
            if (chars[0] == '\\') {
                assert(match { it in listOf('t', 'b', 'n', 'r', '\'', '\"', '\\', '\$') })
            }
        }
        assert(match { it == '\"' })
        return TokenType.STRING
    }

    private fun lexOperator(): TokenType {
        assert(match { !it.isWhitespace() && !it.isLetterOrDigit() && it !in listOf('\'', '\"') })
        return TokenType.OPERATOR
    }

    private fun match(predicate: (Char) -> Boolean): Boolean {
        if (chars[1]?.let(predicate) == true) {
            chars.advance()
            return true
        }
        return false
    }

}

private class CharStream(private val chars: CharSequence) {

    private val builder = StringBuilder()
    private var index = -1

    operator fun get(offset: Int): Char? {
        return chars.elementAtOrNull(index + offset)
    }

    fun advance() {
        builder.append(chars[++index])
    }

    fun emit(): String {
        val literal = builder.toString()
        builder.clear()
        return literal
    }

}
