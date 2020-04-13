package dev.rhovas.compiler.structure

sealed class Cmpt

data class ClassCmpt(val type: Type, val extds: List<Type>, val mbrs: List<Mbr>): Cmpt()
data class InterfaceCmpt(val type: Type, val extds: List<Type>, val mbrs: List<Mbr>): Cmpt()
