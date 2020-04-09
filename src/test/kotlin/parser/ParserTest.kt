package dev.rhovas.compiler.parser

import dev.rhovas.compiler.lexer.*
import dev.rhovas.compiler.structure.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParserTest {

    private fun testLiteralExpr() = listOf(
        Arguments.of("Null", "null", null),
        Arguments.of("True", "true", true),
        Arguments.of("False", "false", false),
        Arguments.of("Integer", "123", 123),
        Arguments.of("Decimal", "123.456", 123.456),
        Arguments.of("Character", "'a'", 'a'),
        Arguments.of("Escaped Character", "'\\\''", '\''),
        Arguments.of("String", "\"abc\"", "abc"),
        Arguments.of("Escaped String", "\"\\t\\b\\n\\r\\\'\\\"\\\\\\\$\"", "\t\b\n\r\'\"\\\$")
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testLiteralExpr(name: String, input: String, expected: Any?) {
        test(input, LiteralExpr(expected))
    }

    private fun testGroupExpr() = listOf(
        Arguments.of("Literal", "(expr)", GroupExpr(expr())),
        Arguments.of("Priority", "expr1 * (expr2 + expr3)",
            BinaryExpr("*", expr(1), GroupExpr(BinaryExpr("+", expr(2), expr(3)))))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testGroupExpr(name: String, input: String, expected: Expr) {
        test(input, expected)
    }

    private fun testUnaryExpr() = listOf(
        Arguments.of("Plus", "+expr", UnaryExpr("+", expr())),
        Arguments.of("Minus", "-expr", UnaryExpr("-", expr())),
        Arguments.of("Negation", "!expr", UnaryExpr("!", expr())),
        Arguments.of("Addition Priority", "+expr1 - -expr2",
            BinaryExpr("-", UnaryExpr("+", expr(1)), UnaryExpr("-", expr(2)))),
        Arguments.of("Function Priority", "!obj.method()",
            UnaryExpr("!", FunctionExpr("method", AccessExpr("obj", null), listOf())))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testUnaryExpr(name: String, input: String, expected: Expr) {
        test(input, expected)
    }

    private fun testBinaryExpr() = listOf(
        Arguments.of("Logical Or", "expr1 || expr2", BinaryExpr("||", expr(1), expr(2))),
        Arguments.of("Logical And", "expr1 && expr2", BinaryExpr("&&", expr(1), expr(2))),
        Arguments.of("Logical Priority", "expr1 || expr2 && expr3",
            BinaryExpr("||", expr(1), BinaryExpr("&&", expr(2), expr(3)))),
        Arguments.of("Comparison", "expr1 < expr2 && expr3 >= expr4",
            BinaryExpr("&&",
                BinaryExpr("<", expr(1), expr(2)),
                BinaryExpr(">=", expr(3), expr(4))
            )),
        Arguments.of("Equality", "expr1 == expr2 && expr3 != expr4",
            BinaryExpr("&&",
                BinaryExpr("==", expr(1), expr(2)),
                BinaryExpr("!=", expr(3), expr(4))
            )),
        Arguments.of("Additive", "expr1 + expr2 == expr3 - expr4",
            BinaryExpr("==",
                BinaryExpr("+", expr(1), expr(2)),
                BinaryExpr("-", expr(3), expr(4))
            )),
        Arguments.of("Multiplicative", "expr1 * expr2 == expr3 / expr4",
            BinaryExpr("==",
                BinaryExpr("*", expr(1), expr(2)),
                BinaryExpr("/", expr(3), expr(4))
            ))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testBinaryExpr(name: String, input: String, expected: Expr) {
        test(input, expected)
    }

    private fun testAccessExpr() = listOf(
        Arguments.of("Variable", "variable", AccessExpr("variable", null)),
        Arguments.of("Type", "Type", AccessExpr("Type", null)),
        Arguments.of("Member", "expr.member", AccessExpr("member", expr())),
        Arguments.of("Priority", "expr.first.second",
            AccessExpr("second", AccessExpr("first", expr())))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testAccessExpr(name: String, input: String, expected: Expr) {
        test(input, expected)
    }

    private fun testFunctionExpr() = listOf(
        Arguments.of("Zero Arguments", "func()", FunctionExpr("func", null, listOf())),
        Arguments.of("Single Argument", "func(expr)", FunctionExpr("func", null, listOf(expr()))),
        Arguments.of("Multiple Arguments", "func(expr1, expr2, expr3)",
            FunctionExpr("func", null, listOf(expr(1), expr(2), expr(3)))),
        Arguments.of("Constructor", "Type()", FunctionExpr("Type", null, listOf())),
        Arguments.of("Method", "expr.method()", FunctionExpr("method", expr(), listOf()))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testFunctionExpr(name: String, input: String, expected: Expr) {
        test(input, expected)
    }

    private fun testIndexExpr() = listOf(
        Arguments.of("Zero Arguments", "expr[]", IndexExpr(expr(), listOf())),
        Arguments.of("Single Argument", "expr[expr1]", IndexExpr(expr(), listOf(expr(1)))),
        Arguments.of("Multiple Arguments", "expr[expr1, expr2, expr3]",
            IndexExpr(expr(), listOf(expr(1), expr(2), expr(3)))),
        Arguments.of("Priority", "expr[expr1][expr2]",
            IndexExpr(IndexExpr(expr(), listOf(expr(1))), listOf(expr(2))))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testIndexExpr(name: String, input: String, expected: Expr) {
        test(input, expected)
    }

    @Test
    fun testExpressions() {
        test("x <= (1 + 2) / 3 && y == \"ca\t\" || z(4.5, '@') && !obj.field.method()[6]",
            BinaryExpr("||",
                BinaryExpr("&&",
                    BinaryExpr("<=",
                        AccessExpr("x", null),
                        BinaryExpr("/",
                            GroupExpr(BinaryExpr("+",
                                LiteralExpr(1),
                                LiteralExpr(2)
                            )),
                            LiteralExpr(3)
                        )
                    ),
                    BinaryExpr("==",
                        AccessExpr("y", null),
                        LiteralExpr("ca\t")
                    )
                ),
                BinaryExpr("&&",
                    FunctionExpr("z", null, listOf(
                        LiteralExpr(4.5),
                        LiteralExpr('@')
                    )),
                    UnaryExpr("!",
                        IndexExpr(
                            FunctionExpr("method",
                                AccessExpr("field",
                                    AccessExpr("obj", null)
                                ),
                                listOf()
                            ),
                            listOf(LiteralExpr(6))
                        )
                    )
                )
            )
        )
    }

    private fun test(input: String, expected: Expr) {
        Assertions.assertEquals(expected, Parser(Lexer(input).lex()).parseExpr())
    }

    private fun expr(n: Int? = null): Expr {
        return AccessExpr("expr${n ?: ""}", null)
    }

}
