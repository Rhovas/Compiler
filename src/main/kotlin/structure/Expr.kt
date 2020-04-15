package dev.rhovas.compiler.structure

sealed class Expr

data class LiteralExpr(val obj: Any?): Expr()
data class GroupExpr(val expr: Expr): Expr()
data class UnaryExpr(val op: String, val expr: Expr): Expr()
data class BinaryExpr(val op: String, val left: Expr, val right: Expr): Expr()
data class AccessExpr(val name: String, val expr: Expr?): Expr()
data class FunctionExpr(val name: String, val expr: Expr?, val exprs: List<Expr>): Expr()
data class IndexExpr(val expr: Expr, val exprs: List<Expr>): Expr()
data class LambdaExpr(val params: List<String>, val stmt: Stmt): Expr()
