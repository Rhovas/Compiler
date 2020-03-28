package dev.rhovas.compiler.lexer

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LexerTest {

    private fun testWhitespace() = listOf(
        Arguments.of("Single Space", " "),
        Arguments.of("Multiple Spaces", "   "),
        Arguments.of("Escape Characters", " \t\n\r")
    )

    @ParameterizedTest(name = "{0}")
    @MethodSource
    fun testWhitespace(name: String, input: String) {
        test(input, listOf(Token(TokenType.WHITESPACE, input)))
    }

    private fun testIdentifier() = listOf(
        Arguments.of("Single Lowercase", "a"),
        Arguments.of("Multiple Lowercase", "abc"),
        Arguments.of("Single Uppercase", "A"),
        Arguments.of("Multiple Uppercase", "ABC"),
        Arguments.of("Mixed Case", "AbC"),
        Arguments.of("Terminating Digits", "abc123"),
        Arguments.of("Intermediate Digits", "abc123abc")
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testIdentifier(name: String, input: String) {
        test(input, listOf(Token(TokenType.IDENTIFIER, input)))
    }

    private fun testInteger() = listOf(
        Arguments.of("Zero", "0"),
        Arguments.of("Single Digit", "1"),
        Arguments.of("Multiple Digits", "123"),
        Arguments.of("Leading Zeros", "001"),
        Arguments.of("Trailing Zeros", "100")
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testInteger(name: String, input: String) {
        test(input, listOf(Token(TokenType.INTEGER, input)))
    }

    private fun testDecimal() = listOf(
        Arguments.of("Zero", "0.0"),
        Arguments.of("Single Digit", "1.2"),
        Arguments.of("Multiple Digits", "123.456"),
        Arguments.of("Leading Zeros", "001.0"),
        Arguments.of("Trailing Zeros", "1.000")
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testDecimal(name: String, input: String) {
        test(input, listOf(Token(TokenType.DECIMAL, input)))
    }

    private fun testCharacter() = listOf(
        Arguments.of("Letter", "'a'"),
        Arguments.of("Digit", "'1'"),
        Arguments.of("Tilda", "'~'"),
        Arguments.of("Double Quote", "'\"'"),
        Arguments.of("Tab Escape", "'\\t'"),
        Arguments.of("Backspace Escape", "'\\b'"),
        Arguments.of("Newline Escape", "'\\n'"),
        Arguments.of("Carriage Return Escape", "'\\r'"),
        Arguments.of("Single Quote Escape", "'\\\''"),
        Arguments.of("Double Quote Escape", "'\\\"'"),
        Arguments.of("Backspace Escape", "'\\\\'"),
        Arguments.of("Dollar Sign Escape", "'\\\$'")
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testCharacter(name: String, input: String) {
        test(input, listOf(Token(TokenType.CHARACTER, input)))
    }

    private fun testString() = listOf(
        Arguments.of("Empty", "\"\""),
        Arguments.of("Single Character", "\"a\""),
        Arguments.of("Multiple Character", "\"abc\""),
        Arguments.of("Spaces", "\"   \""),
        Arguments.of("Single Quote", "\"'\""),
        Arguments.of("Escape Characters", "\"\\t\\b\\n\\r\\\'\\\"\\\\\\\$\"")
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testString(name: String, input: String) {
        test(input, listOf(Token(TokenType.STRING, input)))
    }

    private fun testOperator() = listOf(
        Arguments.of("Parentheses", "()"),
        Arguments.of("Braces", "{}"),
        Arguments.of("Brackets", "[]"),
        Arguments.of("Math", "+-*/"),
        Arguments.of("Comparison", "<=>"),
        Arguments.of("Separators", ".:,;"),
        Arguments.of("Symbols", "!@#$%^&"),
        Arguments.of("Unicode", "♔♕♖♗♘♙")
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testOperator(name: String, input: String) {
        test(input, input.toCharArray().map { Token(TokenType.OPERATOR, "$it") })
    }

    @Test
    fun testHelloWorld() {
        test("func main(args: Array<String>) { print(\"Hello, World!\"); }", listOf(
            Token(TokenType.IDENTIFIER, "func"),
            Token(TokenType.WHITESPACE, " "),
            Token(TokenType.IDENTIFIER, "main"),
            Token(TokenType.OPERATOR, "("),
            Token(TokenType.IDENTIFIER, "args"),
            Token(TokenType.OPERATOR, ":"),
            Token(TokenType.WHITESPACE, " "),
            Token(TokenType.IDENTIFIER, "Array"),
            Token(TokenType.OPERATOR, "<"),
            Token(TokenType.IDENTIFIER, "String"),
            Token(TokenType.OPERATOR, ">"),
            Token(TokenType.OPERATOR, ")"),
            Token(TokenType.WHITESPACE, " "),
            Token(TokenType.OPERATOR, "{"),
            Token(TokenType.WHITESPACE, " "),
            Token(TokenType.IDENTIFIER, "print"),
            Token(TokenType.OPERATOR, "("),
            Token(TokenType.STRING, "\"Hello, World!\""),
            Token(TokenType.OPERATOR, ")"),
            Token(TokenType.OPERATOR, ";"),
            Token(TokenType.WHITESPACE, " "),
            Token(TokenType.OPERATOR, "}")
        ))
    }

    private fun test(input: String, expected: List<Token>) {
        Assertions.assertEquals(expected, lex(input).toList())
    }

}
