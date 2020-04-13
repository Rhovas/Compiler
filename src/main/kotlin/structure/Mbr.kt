package dev.rhovas.compiler.structure

sealed class Mbr

data class FieldMbr(val mut: Boolean, val name: String, val type: Type?, val expr: Expr?): Mbr()
data class CtorMbr(val params: List<FuncParam>, val stmt: Stmt): Mbr()
data class FuncMbr(val name: String, val params: List<FuncParam>, val type: Type?, val stmt: Stmt): Mbr()
data class FuncParam(val name: String, val type: Type, val expr: Expr?)
