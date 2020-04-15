package dev.rhovas.compiler.structure

sealed class Stmt

data class BlockStmt(val stmts: List<Stmt>): Stmt()
data class ExpressionStmt(val expr: Expr): Stmt()
data class DeclarationStmt(val mut: Boolean, val name: String, val type: Type?, val expr: Expr?): Stmt()
data class AssignmentStmt(val expr: Expr, val value: Expr): Stmt()
data class IfStmt(val expr: Expr, val then: Stmt, val else_: Stmt?): Stmt()
data class MatchStmt(val name: String?, val exprs: List<Expr>, val cases: List<Pair<List<Expr>, Stmt>>): Stmt()
data class ForStmt(val name: String, val expr: Expr, val stmt: Stmt): Stmt()
data class WhileStmt(val expr: Expr, val stmt: Stmt): Stmt()
data class TryStmt(val stmt: Stmt, val catch: Stmt?, val finally: Stmt?): Stmt()
data class WithStmt(val name: String, val expr: Expr, val stmt: Stmt): Stmt()
data class LabelStmt(val label: String, val stmt: Stmt): Stmt()
data class JumpStmt(val type: JumpType, val label: String?, val expr: Expr?): Stmt()
enum class JumpType {BREAK, CONTINUE, RETURN, THROW}
data class AssertStmt(val type: AssertType, val expr: Expr): Stmt()
enum class AssertType {ASSERT, REQUIRE, ENSURE}
