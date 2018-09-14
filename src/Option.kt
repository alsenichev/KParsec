package com.senichev.kParsec.option

/**
 * A wrapper of the type to represent nullable value to allow functional composition.
 */
sealed class Option<T>

/**
 * The underlying value has value.
 */
internal data class Some<T>(val value: T) : Option<T>() {
    override fun toString() = "Some($value)"
}

/**
 * The underlying value does not have value.
 */
internal class None<T>() : Option<T>() {
    override fun toString() = "None"
}