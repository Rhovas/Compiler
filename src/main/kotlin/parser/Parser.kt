package dev.rhovas.compiler.parser

import dev.rhovas.compiler.lexer.*
import dev.rhovas.compiler.structure.*

class Parser(tokens: Sequence<Token>) {

    private val tokens = TokenStream(tokens.filter { it.type != TokenType.WHITESPACE }.toList())

    fun parseSrc(): Src {
        val impts = mutableListOf<Impt>()
        while (tokens[1]?.literal == "import") {
            impts.add(parseImpt())
        }
        val cmpts = mutableListOf<Cmpt>()
        val mbrs = mutableListOf<Mbr>()
        while (tokens[1] !== null) {
            when (tokens[1]?.literal) {
                "class", "interface", "struct" -> cmpts.add(parseCmpt())
                "var", "val", "ctor", "func" -> mbrs.add(parseMbr())
                else -> throw AssertionError(tokens[1])
            }
        }
        return Src(impts, cmpts, mbrs)
    }

    fun parseImpt(): Impt {
        assert(match("import"))
        val path = mutableListOf<String>()
        do {
            assert(match(TokenType.IDENTIFIER))
            path.add(tokens[0]!!.literal)
        } while (match("."))
        val alias = if (match("as")) {
            assert(match(TokenType.IDENTIFIER))
            tokens[0]!!.literal
        } else null
        assert(match(";"))
        return Impt(path, alias)
    }

    fun parseType(): Type {
        if (match("(")) {
            val types = parseSeq(",", ")", this::parseType)
            assert(match("-"))
            assert(match(">"))
            val ret = parseType()
            return FuncType(types, ret)
        }
        val mut = when {
            match("+") -> TypeMutability.MUTABLE
            match("-") -> TypeMutability.IMMUTABLE
            else -> TypeMutability.VIEWABLE
        }
        assert(match(TokenType.IDENTIFIER))
        val name = tokens[0]!!.literal
        val generics = if (match("<")) parseSeq(",", ">", this::parseType) else listOf()
        val nullable = match("?")
        return BaseType(mut, name, generics, nullable)
    }

    fun parseCmpt(): Cmpt {
        return when (tokens[1]?.literal) {
            "class" -> parseClassCmpt()
            "interface" -> parseInterfaceCmpt()
            "struct" -> parseStructCmpt()
            else -> throw AssertionError(tokens[1])
        }
    }

    private fun parseClassCmpt(): ClassCmpt {
        assert(match("class"))
        val type = parseType()
        val extds = if (match(":")) parseSeq(",", "{", this::parseType) else {
            assert(match("{"))
            listOf()
        }
        val mbrs = parseSeq(null, "}", this::parseMbr);
        return ClassCmpt(type, extds, mbrs)
    }

    private fun parseInterfaceCmpt(): InterfaceCmpt {
        assert(match("interface"))
        val type = parseType()
        val extds = if (match(":")) parseSeq(",", "{", this::parseType) else {
            assert(match("{"))
            listOf()
        }
        val mbrs = parseSeq(null, "}", this::parseMbr);
        return InterfaceCmpt(type, extds, mbrs)
    }

    private fun parseStructCmpt(): StructCmpt {
        assert(match("struct"))
        val type = parseType()
        val extds = if (match(":")) parseSeq(",", "{", this::parseType) else {
            assert(match("{"))
            listOf()
        }
        val mbrs = parseSeq(null, "}", this::parseMbr);
        return StructCmpt(type, extds, mbrs)
    }

    fun parseMbr(): Mbr {
        return when (tokens[1]?.literal) {
            "var", "val" -> parseFieldMbr()
            "ctor" -> parseCtorMbr()
            "func" -> parseFuncMbr()
            else -> throw AssertionError(tokens[1])
        }
    }

    private fun parseFieldMbr(): FieldMbr {
        assert(match("var", "val"))
        val mut = tokens[0]!!.literal == "var"
        assert(match(TokenType.IDENTIFIER))
        val name = tokens[0]!!.literal
        val type = if (match(":")) parseType() else null
        val expr = if (match("=")) parseExpr() else null
        assert(match(";"))
        return FieldMbr(mut, name, type, expr)
    }

    private fun parseCtorMbr(): CtorMbr {
        assert(match("ctor"))
        assert(match("("))
        val params = parseSeq(",", ")", this::parseFuncParam)
        val stmt = parseStmt()
        return CtorMbr(params, stmt)
    }

    private fun parseFuncMbr(): FuncMbr {
        assert(match("func"))
        assert(match(TokenType.IDENTIFIER))
        val name = tokens[0]!!.literal
        assert(match("("))
        val params = parseSeq(",", ")", this::parseFuncParam)
        val ret = if (match(":")) parseType() else null
        val stmt = parseStmt()
        return FuncMbr(name, params, ret, stmt)
    }

    private fun parseFuncParam(): FuncParam {
        assert(match(TokenType.IDENTIFIER))
        val name = tokens[0]!!.literal
        assert(match(":"))
        val type = parseType()
        val expr = if (match("=")) parseExpr() else null
        return FuncParam(name, type, expr)
    }

    fun parseStmt(): Stmt {
        return when(tokens[1]?.literal) {
            "{" -> parseBlockStmt()
            "var", "val" -> parseDeclarationStmt()
            "if" -> parseIfStmt()
            "match" -> parseMatchStmt()
            "for" -> parseForStmt()
            "while" -> parseWhileStmt()
            "try" -> parseTryStmt()
            "with" -> parseWithStmt()
            "break", "continue", "return", "throw" -> parseJumpStmt()
            "assert", "require", "ensure" -> parseAssertStmt()
            else -> {
                if (isMatch(1, TokenType.IDENTIFIER) && isMatch(2, ":")) {
                    assert(match(TokenType.IDENTIFIER))
                    val label = tokens[0]!!.literal
                    assert(match(":"))
                    val stmt = parseStmt()
                    LabelStmt(label, stmt)
                } else {
                    val expr = parseExpr()
                    val stmt = if (match("=")) {
                        AssignmentStmt(expr, parseExpr())
                    } else {
                        ExpressionStmt(expr)
                    }
                    assert(match(";"))
                    stmt
                }
            }
        }
    }

    private fun parseBlockStmt(): Stmt {
        assert(match("{"))
        return BlockStmt(parseSeq(null, "}", this::parseStmt))
    }

    private fun parseDeclarationStmt(): Stmt {
        assert(match("var", "val"))
        val mut = tokens[0]!!.literal == "var"
        assert(match(TokenType.IDENTIFIER))
        val name = tokens[0]!!.literal
        val type = if (match(":")) parseType() else null
        val expr = if (match("=")) parseExpr() else null
        assert(match(";"))
        return DeclarationStmt(mut, name, type, expr)
    }

    private fun parseIfStmt(): Stmt {
        assert(match("if"))
        assert(match("("))
        val expr = parseExpr()
        assert(match(")"))
        val then = parseStmt()
        val else_ = if (match("else")) parseStmt() else null
        return IfStmt(expr, then, else_)
    }

    private fun parseMatchStmt(): Stmt {
        assert(match("match"))
        assert(match("("))
        val name: String? = if (isMatch(1, TokenType.IDENTIFIER) && isMatch(2, "=")) {
            assert(match(TokenType.IDENTIFIER))
            val name = tokens[0]!!.literal
            assert(match("="))
            name
        } else null
        val exprs = parseSeq(",", ")", this::parseExpr)
        assert(match("{"))
        val cases = parseSeq(null, "}") {
            val pattern = if (match("is")) {
                val type = parseType()
                assert(match(":"))
                MatchPattern(null, type)
            } else {
                MatchPattern(parseSeq(",", ":") {
                    if (match("_")) AccessExpr(this.tokens[0]!!.literal, null) else parseExpr()
                }, null)
            }
            Pair(pattern, parseStmt())
        }
        return MatchStmt(name, exprs, cases)
    }

    private fun parseForStmt(): Stmt {
        assert(match("for"))
        assert(match("("))
        assert(match(TokenType.IDENTIFIER))
        val name = tokens[0]!!.literal
        assert(match("in"))
        val expr = parseExpr()
        assert(match(")"))
        val stmt = parseStmt()
        return ForStmt(name, expr, stmt)
    }

    private fun parseWhileStmt(): Stmt {
        assert(match("while"))
        assert(match("("))
        val expr = parseExpr()
        assert(match(")"))
        val stmt = parseStmt()
        return WhileStmt(expr, stmt)
    }

    private fun parseTryStmt(): Stmt {
        assert(match("try"))
        val stmt = parseStmt()
        val catch = if (match("catch")) {
            match("(")
            match(TokenType.IDENTIFIER)
            val name = tokens[0]!!.literal
            match(":")
            val type = parseType()
            match(")")
            val stmt = parseStmt()
            Triple(name, type, stmt)
        } else null
        val finally = if (match("finally")) parseStmt() else null
        return TryStmt(stmt, catch, finally)
    }

    private fun parseWithStmt(): Stmt {
        assert(match("with"))
        assert(match("("))
        assert(match(TokenType.IDENTIFIER))
        val name = tokens[0]!!.literal
        assert(match("="))
        val expr = parseExpr()
        assert(match(")"))
        val stmt = parseStmt()
        return WithStmt(name, expr, stmt)
    }

    private fun parseJumpStmt(): Stmt {
        assert(match("break", "continue", "return", "throw"))
        val stmt = when (val type = JumpType.valueOf(tokens[0]!!.literal.toUpperCase())) {
            JumpType.BREAK, JumpType.CONTINUE -> {
                val label = if (match(TokenType.IDENTIFIER)) tokens[0]!!.literal else null
                JumpStmt(type, label, null)
            }
            JumpType.RETURN -> {
                val expr = if (tokens[1]?.literal == ";") null else parseExpr()
                JumpStmt(type, null, expr)
            }
            JumpType.THROW -> JumpStmt(type, null, parseExpr())
        }
        assert(match(";"))
        return stmt
    }

    private fun parseAssertStmt(): Stmt {
        assert(match("assert", "require", "ensure"))
        val type = AssertType.valueOf(tokens[0]!!.literal.toUpperCase())
        val expr = parseExpr()
        assert(match(";"))
        return AssertStmt(type, expr)
    }

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
            match(TokenType.CHARACTER) -> LiteralExpr(escape(tokens[0]!!.literal)[1])
            match(TokenType.STRING) -> LiteralExpr(escape(tokens[0]!!.literal).removeSurrounding("\""))
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
            isMatch(1, "{") -> parseLambdaExpr()
            else -> throw AssertionError(tokens[1])
        }
    }

    private fun parseLambdaExpr(): LambdaExpr {
        assert(match("{"))
        val params = if (match("(")) parseSeq(",", ")") {
            assert(match(TokenType.IDENTIFIER))
            tokens[0]!!.literal
        } else listOf()
        if (params.isNotEmpty()) {
            assert(match("-"))
            assert(match(">"))
        }
        val stmts = parseSeq(null, "}", this::parseStmt)
        return LambdaExpr(params, if (stmts.size == 1) stmts[0] else BlockStmt(stmts))
    }

    private fun <T> parseSeq(sep: String?, end: String, parser: () -> T): List<T> {
        val list = mutableListOf<T>()
        if (!match(end)) {
            do {
                list.add(parser())
            } while (if (sep != null) match(sep) else !isMatch(1, end))
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

    private fun escape(string: String): String {
        return string
            .replace("\\t", "\t")
            .replace("\\b", "\b")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\\'", "\'")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\\$", "\$")
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
