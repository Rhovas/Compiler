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

    private fun testSrc() = listOf(
        Arguments.of("Import", "import path;",
            Src(listOf(Impt(listOf("path"), null)), listOf(), listOf())),
        Arguments.of("Component", "class Type {}",
            Src(listOf(), listOf(ClassCmpt(type(), listOf(), listOf())), listOf())),
        Arguments.of("Member", "func name() stmt;",
            Src(listOf(), listOf(), listOf(FuncMbr("name", listOf(), null, stmt())))),
        Arguments.of("All", "import path; class Type {} func name() stmt;",
            Src(
                listOf(Impt(listOf("path"), null)),
                listOf(ClassCmpt(type(), listOf(), listOf())),
                listOf(FuncMbr("name", listOf(), null, stmt()))
            )
        )
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testSrc(name: String, input: String, expected: Src) {
        Assertions.assertEquals(expected, Parser(Lexer(input).lex()).parseSrc())
    }

    private fun testImpt() = listOf(
        Arguments.of("Single Name", "import name;", Impt(listOf("name"), null)),
        Arguments.of("Multiple Names", "import x.y.z;", Impt(listOf("x", "y", "z"), null)),
        Arguments.of("Alias", "import path as name;", Impt(listOf("path"), "name"))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testImpt(name: String, input: String, expected: Impt) {
        Assertions.assertEquals(expected, Parser(Lexer(input).lex()).parseImpt())
    }

    private fun testType() = listOf(
        Arguments.of("Type", "Type", type()),
        Arguments.of("Mutable", "+Type",
            Type(TypeMutability.MUTABLE, "Type", listOf(), false)),
        Arguments.of("Immutable", "-Type",
            Type(TypeMutability.IMMUTABLE, "Type", listOf(), false)),
        Arguments.of("Nullable", "Type?",
            Type(TypeMutability.VIEWABLE, "Type", listOf(), true)),
        Arguments.of("Single Generic", "Type<Type1>",
            Type(TypeMutability.VIEWABLE, "Type", listOf(type(1)), false)),
        Arguments.of("Multiple Generics", "Type<Type1, Type2, Type3>",
            Type(TypeMutability.VIEWABLE, "Type", listOf(type(1), type(2), type(3)), false))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testType(name: String, input: String, expected: Type) {
        Assertions.assertEquals(expected, Parser(Lexer(input).lex()).parseType())
    }

    private fun type(n: Int? = null): Type {
        return Type(TypeMutability.VIEWABLE, "Type${n ?: ""}", listOf(), false)
    }

    private fun testClassCmpt() = listOf(
        Arguments.of("Empty", "class Type {}", ClassCmpt(type(), listOf(), listOf())),
        Arguments.of("Extends", "class Type1: Type2 {}",
            ClassCmpt(type(1), listOf(type(2)), listOf())),
        Arguments.of("Members", "class Type1 { val name: Type2; ctor() stmt1; func name() stmt2; }",
            ClassCmpt(type(1), listOf(), listOf(
                FieldMbr(false, "name", type(2), null),
                CtorMbr(listOf(), stmt(1)),
                FuncMbr("name", listOf(), null, stmt(2))
            )))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testClassCmpt(name: String, input: String, expected: Cmpt) {
        Assertions.assertEquals(expected, Parser(Lexer(input).lex()).parseCmpt())
    }

    private fun testInterfaceCmpt() = listOf(
        Arguments.of("Empty", "interface Type {}", InterfaceCmpt(type(), listOf(), listOf())),
        Arguments.of("Extends", "interface Type1: Type2 {}",
            InterfaceCmpt(type(1), listOf(type(2)), listOf())),
        Arguments.of("Members", "interface Type1 { val name: Type2; func name() stmt; }",
            InterfaceCmpt(type(1), listOf(), listOf(
                FieldMbr(false, "name", type(2), null),
                FuncMbr("name", listOf(), null, stmt())
            )))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testInterfaceCmpt(name: String, input: String, expected: Cmpt) {
        Assertions.assertEquals(expected, Parser(Lexer(input).lex()).parseCmpt())
    }

    private fun testFieldMbr() = listOf(
        Arguments.of("Var", "var name: Type;", FieldMbr(true, "name", type(), null)),
        Arguments.of("Val", "val name = expr;", FieldMbr(false, "name", null, expr())),
        Arguments.of("Type & Expr", "var name: Type = expr;",
            FieldMbr(true, "name", type(), expr()))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testFieldMbr(name: String, input: String, expected: Mbr) {
        Assertions.assertEquals(expected, Parser(Lexer(input).lex()).parseMbr())
    }

    private fun testCtorMbr() = listOf(
        Arguments.of("Empty", "ctor() {}", CtorMbr(listOf(), BlockStmt(listOf()))),
        Arguments.of("Single Parameter", "ctor(param: Type) stmt;",
            CtorMbr(listOf(FuncParam("param", type(), null)), stmt())),
        Arguments.of("Multiple Parameters", "ctor(param1: Type1, param2: Type2, param3: Type3) stmt;",
            CtorMbr(listOf(
                FuncParam("param1", type(1), null),
                FuncParam("param2", type(2), null),
                FuncParam("param3", type(3), null)
            ), stmt())),
        Arguments.of("Default Argument", "ctor(param: Type = expr) stmt;",
            CtorMbr(listOf(FuncParam("param", type(), expr())), stmt()))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testCtorMbr(name: String, input: String, expected: Mbr) {
        Assertions.assertEquals(expected, Parser(Lexer(input).lex()).parseMbr())
    }

    private fun testFuncMbr() = listOf(
        Arguments.of("Empty", "func name() {}",
            FuncMbr("name", listOf(), null, BlockStmt(listOf()))),
        Arguments.of("Single Parameter", "func name(param: Type) stmt;",
            FuncMbr("name", listOf(FuncParam("param", type(), null)), null, stmt())),
        Arguments.of("Multiple Parameters", "func name(param1: Type1, param2: Type2, param3: Type3) stmt;",
            FuncMbr("name", listOf(
                FuncParam("param1", type(1), null),
                FuncParam("param2", type(2), null),
                FuncParam("param3", type(3), null)
            ), null, stmt())),
        Arguments.of("Default Argument", "func name(param: Type = expr) stmt;",
            FuncMbr("name", listOf(FuncParam("param", type(), expr())), null, stmt())),
        Arguments.of("Return Type", "func name(): Type stmt;",
            FuncMbr("name", listOf(), type(), stmt()))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testFuncMbr(name: String, input: String, expected: Mbr) {
        Assertions.assertEquals(expected, Parser(Lexer(input).lex()).parseMbr())
    }

    private fun test(input: String, expected: Mbr) {
        Assertions.assertEquals(expected, Parser(Lexer(input).lex()).parseMbr())
    }

    private fun testBlockStmt() = listOf(
        Arguments.of("Empty", "{}", BlockStmt(listOf())),
        Arguments.of("Single Statement", "{stmt;}", BlockStmt(listOf(stmt()))),
        Arguments.of("Multiple Statements", "{stmt1; stmt2; stmt3;}",
            BlockStmt(listOf(stmt(1), stmt(2), stmt(3))))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testBlockStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testExpressionStmt() = listOf(
        Arguments.of("Expression", "expr;", ExpressionStmt(expr())),
        Arguments.of("Function", "func();",
            ExpressionStmt(FunctionExpr("func", null, listOf())))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testExpressionStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testDeclarationStmt() = listOf(
        Arguments.of("Var", "var name: Type;",
            DeclarationStmt(true, "name", type(), null)),
        Arguments.of("Val", "val name = expr;",
            DeclarationStmt(false, "name", null, expr())),
        Arguments.of("Type & Expr", "var name: Type = expr;",
            DeclarationStmt(true, "name", type(), expr()))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testDeclarationStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testAssignmentStmt() = listOf(
        Arguments.of("Variable", "name = expr;",
            AssignmentStmt(AccessExpr("name", null), expr())),
        Arguments.of("Property", "expr1.prop = expr2;",
            AssignmentStmt(AccessExpr("prop", expr(1)), expr(2))),
        Arguments.of("Index", "expr1[expr2] = expr3;",
            AssignmentStmt(IndexExpr(expr(1), listOf(expr(2))), expr(3)))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testAssignmentStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testIfStmt() = listOf(
        Arguments.of("If", "if (expr) stmt;", IfStmt(expr(), stmt(), null)),
        Arguments.of("Else", "if (expr) stmt1; else stmt2;", IfStmt(expr(), stmt(1), stmt(2))),
        Arguments.of("Blocks", "if (expr) {} else {stmt1; stmt2; stmt3;}",
            IfStmt(expr(), BlockStmt(listOf()), BlockStmt(listOf(stmt(1), stmt(2), stmt(3)))))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testIfStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testMatchStmt() = listOf(
        Arguments.of("Conditional", "match () {expr1: stmt1; expr2: stmt2; expr3: stmt3;}",
            MatchStmt(null, listOf(), listOf(
                Pair(listOf(expr(1)), stmt(1)),
                Pair(listOf(expr(2)), stmt(2)),
                Pair(listOf(expr(3)), stmt(3))
            ))),
        Arguments.of("Structural", "match (expr1, expr2) {expr3, expr4: stmt;}",
            MatchStmt(null, listOf(expr(1), expr(2)), listOf(
                Pair(listOf(expr(3), expr(4)), stmt())
            ))),
        Arguments.of("Declaration", "match (name = expr) {}",
            MatchStmt("name", listOf(expr()), listOf())),
        Arguments.of("Else", "match (expr) {else: stmt;}",
            MatchStmt(null, listOf(expr()), listOf(
                Pair(listOf(AccessExpr("else", null)), stmt())
            )))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testMatchStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testForStmt() = listOf(
        Arguments.of("For", "for (name in expr) stmt;", ForStmt("name", expr(), stmt())),
        Arguments.of("Block", "for (name in expr) {stmt1; stmt2; stmt3;}",
            ForStmt("name", expr(), BlockStmt(listOf(stmt(1), stmt(2), stmt(3)))))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testForStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testWhileStmt() = listOf(
        Arguments.of("While", "while (expr) stmt;", WhileStmt(expr(), stmt())),
        Arguments.of("Block", "while (expr) {stmt1; stmt2; stmt3;}",
            WhileStmt(expr(), BlockStmt(listOf(stmt(1), stmt(2), stmt(3)))))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testWhileStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testTryStmt() = listOf(
        Arguments.of("Try", "try stmt;", TryStmt(stmt(), null, null)),
        Arguments.of("Catch", "try stmt1; catch stmt2;", TryStmt(stmt(1), stmt(2), null)),
        Arguments.of("Finally", "try stmt1; finally stmt2;", TryStmt(stmt(1), null, stmt(2))),
        Arguments.of("All", "try stmt1; catch {} finally {stmt2; stmt3; stmt4;}",
            TryStmt(stmt(1), BlockStmt(listOf()), BlockStmt(listOf(stmt(2), stmt(3), stmt(4)))))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testTryStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testWithStmt() = listOf(
        Arguments.of("With", "with (name = expr) stmt;",
            WithStmt("name", expr(), stmt())),
        Arguments.of("Block", "with (name = expr) {stmt1; stmt2; stmt3;}",
            WithStmt("name", expr(), BlockStmt(listOf(stmt(1), stmt(2), stmt(3)))))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testWithStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testLabelStmt() = listOf(
        Arguments.of("Label", "label: stmt;", LabelStmt("label", stmt())),
        Arguments.of("Loop", "label: while(expr) stmt;",
            LabelStmt("label", WhileStmt(expr(), stmt())))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testLabelStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testJumpStmt() = listOf(
        Arguments.of("Break", "break;", JumpStmt(JumpType.BREAK, null, null)),
        Arguments.of("Continue", "continue;", JumpStmt(JumpType.CONTINUE, null, null)),
        Arguments.of("Label", "break label;", JumpStmt(JumpType.BREAK, "label", null)),
        Arguments.of("Return", "return;", JumpStmt(JumpType.RETURN, null, null)),
        Arguments.of("Return Value", "return expr;", JumpStmt(JumpType.RETURN, null, expr())),
        Arguments.of("Throw", "throw expr;", JumpStmt(JumpType.THROW, null, expr()))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testJumpStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun testAssertStmt() = listOf(
        Arguments.of("Assert", "assert expr;", AssertStmt(AssertType.ASSERT, expr())),
        Arguments.of("Require", "require expr;", AssertStmt(AssertType.REQUIRE, expr())),
        Arguments.of("Ensure", "ensure expr;", AssertStmt(AssertType.ENSURE, expr()))
    )

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource
    fun testAssertStmt(name: String, input: String, expected: Stmt) {
        test(input, expected)
    }

    private fun test(input: String, expected: Stmt) {
        Assertions.assertEquals(expected, Parser(Lexer(input).lex()).parseStmt());
    }

    private fun stmt(n: Int? = null): Stmt {
        return ExpressionStmt(AccessExpr("stmt${n ?: ""}", null))
    }

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
