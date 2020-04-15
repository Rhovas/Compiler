package dev.rhovas.compiler.structure

sealed class Type

data class BaseType(val mut: TypeMutability, val name: String, val generics: List<Type>, val nullable: Boolean): Type()
enum class TypeMutability {VIEWABLE, MUTABLE, IMMUTABLE}
data class FuncType(val params: List<Type>, val ret: Type): Type()
