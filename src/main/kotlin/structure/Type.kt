package dev.rhovas.compiler.structure

data class Type(val mut: TypeMutability, val name: String, val generics: List<Type>, val nullable: Boolean)
enum class TypeMutability {VIEWABLE, MUTABLE, IMMUTABLE}
