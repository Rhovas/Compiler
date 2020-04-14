package dev.rhovas.compiler.generator

import dev.rhovas.compiler.structure.*
import java.io.PrintWriter

class KotlinGenerator(private val writer: PrintWriter) {

    private var indent = 0

    private fun newline(indent: Int) {
        writer.println()
        writer.print("    ".repeat(indent))
    }

    fun gen(obj: Any?) {
        when (obj) {
            is Src -> genSrc(obj)
            is Impt -> genImpt(obj)
            is Type -> genType(obj)
            is Cmpt -> genCmpt(obj)
            is Mbr -> genMbr(obj)
            is FuncParam -> genFuncParam(obj)
            is Stmt -> genStmt(obj)
            is Expr -> genExpr(obj)
        }
    }

    private fun genSrc(src: Src) {
        genSeq(src.impts, null, { newline(indent) }, { newline(0) })
        genSeq(src.cmpts + src.mbrs,
            { if (src.impts.isNotEmpty()) newline(indent) },
            { newline(0); newline(indent) },
            { newline(0) })
    }

    private fun genImpt(impt: Impt) {
        writer.print("import ")
        writer.print(impt.path.joinToString("."))
        if (impt.alias != null) {
            writer.print(" as ")
            writer.print(impt.alias)
        }
    }

    private fun genType(type: Type) {
        writer.print(type.name)
        genSeq(type.generics, "<", ", ", ">")
        if (type.nullable) writer.print("?")
    }

    fun genCmpt(cmpt: Cmpt) {
        when (cmpt) {
            is ClassCmpt -> genClassCmpt(cmpt)
            is InterfaceCmpt -> genInterfaceCmpt(cmpt)
        }
    }

    private fun genClassCmpt(cmpt: ClassCmpt) {
        writer.print("class ")
        genType(cmpt.type)
        if (cmpt.extds.isNotEmpty()) {
            writer.print(": ")
            genSeq(cmpt.extds, null, ", ", null)
        }
        writer.print(" {")
        genSeq(cmpt.mbrs,
            { newline(0); newline(++indent) },
            { newline(0); newline(indent) },
            { newline(0); newline(--indent) }
        )
        writer.print("}")
    }

    private fun genInterfaceCmpt(cmpt: InterfaceCmpt) {
        writer.print("interface ")
        genType(cmpt.type)
        if (cmpt.extds.isNotEmpty()) {
            writer.print(": ")
            genSeq(cmpt.extds, null, ", ", null)
        }
        writer.print("{")
        genSeq(cmpt.mbrs,
            { newline(0); newline(++indent) },
            { newline(0); newline(indent) },
            { newline(0); newline(--indent) }
        )
        writer.print("}")
    }

    fun genMbr(mbr: Mbr) {
        when (mbr) {
            is FieldMbr -> genFieldMbr(mbr)
            is CtorMbr -> genCtorMbr(mbr)
            is FuncMbr -> genFuncMbr(mbr)
        }
    }

    private fun genFieldMbr(mbr: FieldMbr) {
        writer.print(if (mbr.mut) "var " else "val ")
        writer.print(mbr.name)
        if (mbr.type != null) {
            writer.print(": ")
            genType(mbr.type)
        }
        if (mbr.expr != null) {
            writer.print(" = ")
            genExpr(mbr.expr)
        }
    }

    private fun genCtorMbr(mbr: CtorMbr) {
        writer.print("constructor")
        genSeq(mbr.params, "(", ", ", ")")
        writer.print(" ")
        genStmt(mbr.stmt)
    }

    private fun genFuncMbr(mbr: FuncMbr) {
        writer.print("fun ")
        writer.print(mbr.name)
        writer.print("(")
        genSeq(mbr.params, null, ", ", null)
        writer.print(")")
        if (mbr.type != null) {
            writer.print(": ")
            genType(mbr.type)
        }
        writer.print(" ")
        genStmt(mbr.stmt)
    }

    private fun genFuncParam(param: FuncParam) {
        writer.print(param.name)
        writer.print(": ")
        genType(param.type)
        if (param.expr != null) {
            writer.print(" = ")
            genExpr(param.expr)
        }
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
    }

    private fun genDeclarationStmt(stmt: DeclarationStmt) {
        writer.print(if (stmt.mut) "var " else "val ")
        writer.print(stmt.name)
        if (stmt.type != null) {
            writer.print(": ")
            genType(stmt.type)
        }
        if (stmt.expr != null) {
            writer.print(" = ")
            genExpr(stmt.expr)
        }
    }

    private fun genAssignmentStmt(stmt: AssignmentStmt) {
        genExpr(stmt.expr)
        writer.print(" = ")
        genExpr(stmt.value)
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
            genStmt(stmt.stmt)
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
    }

    private fun genAssertStmt(stmt: AssertStmt) {
        writer.print(if (stmt.type != AssertType.ENSURE) stmt.type.name.toLowerCase() else "check")
        writer.print("(")
        genExpr(stmt.expr)
        writer.print(")")
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
        genExpr(expr.expr)
        writer.print(")")
    }

    private fun genUnaryExpr(expr: UnaryExpr) {
        writer.print(expr.op)
        genExpr(expr.expr)
    }

    private fun genBinaryExpr(expr: BinaryExpr) {
        genExpr(expr.left)
        writer.print(" ")
        writer.print(expr.op)
        writer.print(" ")
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