package dev.rhovas.compiler.structure

data class Src(val impts: List<Impt>, val cmpts: List<Cmpt>, val mbrs: List<Mbr>)
data class Impt(val path: List<String>, val alias: String?)
