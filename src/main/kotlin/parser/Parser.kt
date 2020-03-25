package dev.rhovas.compiler.parser

import dev.rhovas.compiler.lexer.*
import dev.rhovas.compiler.structure.*

fun parse(tokens: Sequence<Token>): Expr {
    return Parser(TokenStream(tokens.filter { it.type != TokenType.WHITESPACE }.toList())).parseExpr()
}

private class Parser(private val tokens: TokenStream) {

    fun parseExpr(): Expr {
        return parseLogicalOrExpr()
    }

    private fun parseLogicalOrExpr(): Expr {
        return parseBinaryExpr(this::parseLogicalAndExpr, "||")
    }

    private fun parseLogicalAndExpr(): Expr {
        return parseBinaryExpr(this::parseComparisonExpr, "&&")
    }

    private fun parseComparisonExpr(): Expr {
        return parseBinaryExpr(this::parseAdditiveExpr, "<", "<=", ">", ">=", "==", "!=", "===", "!==")
    }

    private fun parseAdditiveExpr(): Expr {
        return parseBinaryExpr(this::parseMultiplicativeExpr, "+", "-")
    }

    private fun parseMultiplicativeExpr(): Expr {
        return parseBinaryExpr(this::parseUnaryExpr, "*", "/")
    }

    private fun parseBinaryExpr(parser: () -> Expr, vararg ops: String): Expr {
        var expr = parser()
        while (true) {
            /* Sort and search from back to match the longest operator first. */
            val op = ops.sorted().lastOrNull {op ->
                op.toCharArray().withIndex().all { isMatch(it.index + 1, it.value) }
            } ?: break
            repeat(op.length) { tokens.advance() }
            expr = BinaryExpr(op, expr, parser())
        }
        return expr
    }

    private fun parseUnaryExpr(): Expr {
        if (match("+", "-", "!")) {
            val op = tokens[0]!!.literal
            val expr = parseUnaryExpr()
            return UnaryExpr(op, expr)
        }
        return parseSecondaryExpr()
    }

    private fun parseSecondaryExpr(): Expr {
        var expr = parsePrimaryExpr()
        while (true) {
            expr = when {
                match(".") -> {
                    assert(match(TokenType.IDENTIFIER))
                    val name = tokens[0]!!.literal
                    when {
                        match("(") -> FunctionExpr(name, expr, parseSeq(",", ")", this::parseExpr))
                        else -> AccessExpr(name, expr)
                    }
                }
                match("[") -> IndexExpr(expr, parseSeq(",", "]", this::parseExpr))
                else -> return expr
            }
        }
    }

    private fun parsePrimaryExpr(): Expr {
        return when {
            match("null") -> LiteralExpr(null)
            match("true", "false") -> LiteralExpr(tokens[0]!!.literal.toBoolean())
            match(TokenType.INTEGER) -> LiteralExpr(tokens[0]!!.literal.toInt())
            match(TokenType.DECIMAL) -> LiteralExpr(tokens[0]!!.literal.toDouble())
            match(TokenType.CHARACTER) -> LiteralExpr(tokens[0]!!.literal.removeSurrounding("\'"))
            match(TokenType.STRING) -> LiteralExpr(tokens[0]!!.literal.removeSurrounding("\"")
                .replace("\\t", "\t")
                .replace("\\b", "\b")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\'", "\'")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\\$", "\$"))
            match(TokenType.IDENTIFIER) -> {
                val name = tokens[0]!!.literal
                when {
                    match("(") -> FunctionExpr(name, null, parseSeq(",", ")", this::parseExpr))
                    else -> AccessExpr(name, null)
                }
            }
            match("(") -> {
                val expr = parseExpr()
                assert(match(")"))
                GroupExpr(expr)
            }
            else -> throw AssertionError(tokens[1])
        }
    }

    private fun <T> parseSeq(sep: String, end: String, parser: () -> T): List<T> {
        val list = mutableListOf<T>()
        if (!match(end)) {
            do {
                list.add(parser())
            } while (match(sep))
            assert(match(end))
        }
        return list
    }

    private fun match(vararg objs: Any): Boolean {
        if (objs.any { isMatch(1, it) }) {
            tokens.advance()
            return true
        }
        return false
    }

    private fun isMatch(offset: Int, obj: Any?): Boolean {
        return when (obj) {
            is TokenType -> obj == tokens[offset]?.type
            else -> obj.toString() == tokens[offset]?.literal
        }
    }

}

private class TokenStream(private val tokens: List<Token>) {

    private var index = -1

    operator fun get(offset: Int): Token? {
        return tokens.elementAtOrNull(index + offset)
    }

    fun advance() {
        index++
    }

}
