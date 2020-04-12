package dev.rhovas.compiler.generator

import dev.rhovas.compiler.structure.*
import java.io.PrintWriter

class KotlinGenerator(private val writer: PrintWriter) {

    private var indent = 0;

    private fun newline(indent: Int) {
        writer.println()
        writer.print("    ".repeat(indent))
    }

    fun gen(obj: Any?) {
        when (obj) {
            is Type -> genType(obj)
            is Stmt -> genStmt(obj)
            is Expr -> genExpr(obj)
        }
    }

    private fun genType(type: Type) {
        writer.print(type.name)
        genSeq(type.generics, "<", ", ", ">")
        if (type.nullable) writer.print("?")
    }

    fun genStmt(stmt: Stmt) {
        when (stmt) {
            is BlockStmt -> genBlockStmt(stmt)
            is ExpressionStmt -> genExpressionStmt(stmt)
            is DeclarationStmt -> genDeclarationStmt(stmt)
            is AssignmentStmt -> genAssignmentStmt(stmt)
            is IfStmt -> genIfStmt(stmt)
            is MatchStmt -> genMatchStmt(stmt)
            is ForStmt -> genForStmt(stmt)
            is WhileStmt -> genWhileStmt(stmt)
            is TryStmt -> genTryStmt(stmt)
            is WithStmt -> genWithStmt(stmt)
            is LabelStmt -> genLabelStmt(stmt)
            is JumpStmt -> genJumpStmt(stmt)
            is AssertStmt -> genAssertStmt(stmt)
        }
    }

    private fun genBlockStmt(stmt: BlockStmt) {
        writer.print("{")
        genSeq(stmt.stmts, { newline(++indent) }, { newline(indent) }, { newline(--indent) })
        writer.print("}")
    }

    private fun genExpressionStmt(stmt: ExpressionStmt) {
        genExpr(stmt.expr)
        writer.print(";")
    }

    private fun genDeclarationStmt(stmt: DeclarationStmt) {
        writer.print(if (stmt.mut) "var " else "val ")
        writer.print(stmt.name)
        if (stmt.type != null) {
            writer.print(" ")
            genType(stmt.type)
        }
        if (stmt.expr != null) {
            writer.print(" ")
            genExpr(stmt.expr)
        }
        writer.print(";")
    }

    private fun genAssignmentStmt(stmt: AssignmentStmt) {
        genExpr(stmt.expr)
        writer.print(" = ")
        genExpr(stmt.value)
        writer.print(";")
    }

    private fun genIfStmt(stmt: IfStmt) {
        writer.print("if (")
        genExpr(stmt.expr)
        writer.print(") ")
        genStmt(stmt.then)
        if (stmt.else_ != null) {
            writer.print(" else ")
            genStmt(stmt.else_)
        }
    }

    private fun genMatchStmt(stmt: MatchStmt) {
        writer.print("run {")
        newline(++indent)
        stmt.exprs.withIndex().forEach {
            writer.print("val obj")
            writer.print(it.index)
            writer.print(" = ")
            genExpr(it.value)
            writer.print(";")
            newline(indent)
        }
        writer.print("when {")
        stmt.cases.forEach { c ->
            if (c === stmt.cases.first()) {
                newline(++indent)
            }
            val exprs = c.first.withIndex()
                .filter { it.value !is AccessExpr || (it.value as AccessExpr).name !in listOf("else", "_") }
            if (exprs.isNotEmpty()) {
                exprs.forEach {
                    writer.print("obj")
                    writer.print(it.index)
                    writer.print(" == ")
                    genExpr(it.value)
                    if (it !== exprs.last()) {
                        writer.print(" && ")
                    }
                }
            } else {
                writer.print("else")
            }
            writer.print(" -> ")
            genStmt(c.second)
            if (c !== stmt.cases.last()) {
                newline(indent)
            } else {
                newline(--indent)
            }
        }
        writer.print("}")
        newline(--indent)
        writer.print("}")
    }

    private fun genForStmt(stmt: ForStmt) {
        writer.print("for (")
        writer.print(stmt.name)
        writer.print(" in ")
        genExpr(stmt.expr)
        writer.print(") ")
        genStmt(stmt.stmt)
    }

    private fun genWhileStmt(stmt: WhileStmt) {
        writer.print("while (")
        genExpr(stmt.expr)
        writer.print(") ")
        genStmt(stmt.stmt)
    }

    private fun genTryStmt(stmt: TryStmt) {
        writer.print("try ")
        writer.print(stmt.stmt)
        if (stmt.catch != null) {
            //TODO: Fix catch blocks
            writer.print(" catch(e: Exception) ")
            genStmt(stmt.catch)
        }
        if (stmt.finally != null) {
            writer.print(" finally ")
            genStmt(stmt.finally)
        }
    }

    private fun genWithStmt(stmt: WithStmt) {
        writer.print(stmt.expr)
        writer.print(".use { ")
        assert(stmt.mut) //TODO
        writer.print(stmt.name)
        writer.print(" -> ")
        if (stmt.stmt is BlockStmt) {
            genSeq(stmt.stmt.stmts, { newline(++indent) }, { newline(indent) }, { newline(--indent) })
            writer.print("}")
        } else {
            genStmt(stmt)
        }
    }

    private fun genLabelStmt(stmt: LabelStmt) {
        writer.print(stmt.label)
        writer.print("@ ")
        genStmt(stmt.stmt)
    }

    private fun genJumpStmt(stmt: JumpStmt) {
        writer.print(stmt.type.name.toLowerCase())
        if (stmt.label != null) {
            writer.print("@")
            writer.print(stmt.label)
        }
        if (stmt.expr != null) {
            writer.print(" ")
            genExpr(stmt.expr)
        }
        writer.print(";")
    }

    private fun genAssertStmt(stmt: AssertStmt) {
        writer.print(if (stmt.type != AssertType.ENSURE) stmt.type.name.toLowerCase() else "check")
        writer.print("(")
        genExpr(stmt.expr)
        writer.print(");")
    }

    fun genExpr(expr: Expr) {
        when (expr) {
            is LiteralExpr -> genLiteralExpr(expr)
            is GroupExpr -> genGroupExpr(expr)
            is UnaryExpr -> genUnaryExpr(expr)
            is BinaryExpr -> genBinaryExpr(expr)
            is AccessExpr -> genAccessExpr(expr)
            is FunctionExpr -> genFunctionExpr(expr)
            is IndexExpr -> genIndexExpr(expr)
        }
    }

    private fun genLiteralExpr(expr: LiteralExpr) {
        writer.print(when (expr.obj) {
            is String -> "\"${expr.obj}\""
            is Char -> "'${expr.obj}\'"
            else -> expr.obj
        })
    }

    private fun genGroupExpr(expr: GroupExpr) {
        writer.print("(")
        genExpr(expr)
        writer.print(")")
    }

    private fun genUnaryExpr(expr: UnaryExpr) {
        writer.print(expr.op)
        genExpr(expr)
    }

    private fun genBinaryExpr(expr: BinaryExpr) {
        genExpr(expr.left)
        writer.print(expr.op)
        genExpr(expr.right)
    }

    private fun genAccessExpr(expr: AccessExpr) {
        if (expr.expr != null) {
            genExpr(expr.expr)
            writer.print(".")
        }
        writer.print(expr.name)
    }

    private fun genFunctionExpr(expr: FunctionExpr) {
        if (expr.expr != null) {
            genExpr(expr.expr)
            writer.print(".")
        }
        writer.print(expr.name)
        writer.print("(")
        genSeq(expr.exprs, null, ", ", null)
        writer.print(")")
    }

    private fun genIndexExpr(expr: IndexExpr) {
        genExpr(expr.expr)
        writer.print("[")
        genSeq(expr.exprs, null,", ", null)
        writer.print("]")
    }

    private fun <T> genSeq(seq: List<T>, start: Any?, sep: Any?, end: Any?) {
        seq.forEach {
            if (it === seq.first() && start != null) {
                if (start is () -> Any?) start() else writer.print(start)
            }
            gen(it)
            if (it !== seq.last()) {
                if (sep is () -> Any?) sep() else writer.print(sep)
            } else if (end != null) {
                if (end is () -> Any?) end() else writer.print(end)
            }
        }
    }

}