package com.example.calculator

import kotlin.math.*

enum class AngleMode {
    DEGREE,
    RADIAN
}

object ScientificCalculatorEngine {

    fun evaluate(expression: String, angleMode: AngleMode): Result<Double> {
        return try {
            val sanitized = sanitize(expression)
            val tokens = tokenize(sanitized)
            val rpn = shuntingYard(tokens)
            val result = evaluateRpn(rpn, angleMode)
            if (result.isNaN() || result.isInfinite()) {
                Result.failure(ArithmeticException("Math error"))
            } else {
                Result.success(result)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sanitize(input: String): String {
        return input
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("π", "${Math.PI}")
            .replace("e", "${Math.E}")
            .replace(" ", "")
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    tokens.add(sb.toString())
                    continue
                }
                c == '+' || c == '*' || c == '/' || c == '^' || c == '%' || c == '(' || c == ')' -> {
                    tokens.add(c.toString())
                    i++
                }
                c == '-' -> {
                    // Check if unary minus
                    val isUnary = tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/", "^", "(", "%")
                    if (isUnary) {
                        tokens.add("neg")
                    } else {
                        tokens.add("-")
                    }
                    i++
                }
                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < expr.length && expr[i].isLetter()) {
                        sb.append(expr[i])
                        i++
                    }
                    val word = sb.toString()
                    tokens.add(word)
                    continue
                }
                else -> i++
            }
        }
        return tokens
    }

    private fun precedence(op: String): Int {
        return when (op) {
            "+", "-" -> 1
            "*", "/", "%" -> 2
            "^" -> 3
            "neg" -> 4
            else -> 0
        }
    }

    private fun isOperator(t: String): Boolean = t in listOf("+", "-", "*", "/", "^", "%", "neg")

    private fun isFunction(t: String): Boolean = t in listOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "ln", "log", "sqrt", "abs", "fact"
    )

    private fun shuntingYard(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val stack = ArrayDeque<String>()

        for (token in tokens) {
            when {
                token.toDoubleOrNull() != null -> output.add(token)
                isFunction(token) -> stack.addLast(token)
                isOperator(token) -> {
                    while (stack.isNotEmpty() && isOperator(stack.last()) &&
                        (precedence(stack.last()) > precedence(token) ||
                                (precedence(stack.last()) == precedence(token) && token != "^" && token != "neg"))
                    ) {
                        output.add(stack.removeLast())
                    }
                    stack.addLast(token)
                }
                token == "(" -> stack.addLast(token)
                token == ")" -> {
                    while (stack.isNotEmpty() && stack.last() != "(") {
                        output.add(stack.removeLast())
                    }
                    if (stack.isNotEmpty() && stack.last() == "(") {
                        stack.removeLast()
                    }
                    if (stack.isNotEmpty() && isFunction(stack.last())) {
                        output.add(stack.removeLast())
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            output.add(stack.removeLast())
        }

        return output
    }

    private fun evaluateRpn(tokens: List<String>, angleMode: AngleMode): Double {
        val stack = ArrayDeque<Double>()

        for (token in tokens) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                stack.addLast(num)
            } else if (token == "neg") {
                val a = stack.removeLastOrNull() ?: 0.0
                stack.addLast(-a)
            } else if (isFunction(token)) {
                val a = stack.removeLastOrNull() ?: throw IllegalArgumentException("Missing operand for $token")
                val res = when (token) {
                    "sin" -> {
                        val rad = if (angleMode == AngleMode.DEGREE) Math.toRadians(a) else a
                        sin(rad)
                    }
                    "cos" -> {
                        val rad = if (angleMode == AngleMode.DEGREE) Math.toRadians(a) else a
                        cos(rad)
                    }
                    "tan" -> {
                        val rad = if (angleMode == AngleMode.DEGREE) Math.toRadians(a) else a
                        tan(rad)
                    }
                    "asin" -> {
                        val resRad = asin(a)
                        if (angleMode == AngleMode.DEGREE) Math.toDegrees(resRad) else resRad
                    }
                    "acos" -> {
                        val resRad = acos(a)
                        if (angleMode == AngleMode.DEGREE) Math.toDegrees(resRad) else resRad
                    }
                    "atan" -> {
                        val resRad = atan(a)
                        if (angleMode == AngleMode.DEGREE) Math.toDegrees(resRad) else resRad
                    }
                    "ln" -> {
                        if (a <= 0) throw ArithmeticException("Domain error")
                        ln(a)
                    }
                    "log" -> {
                        if (a <= 0) throw ArithmeticException("Domain error")
                        log10(a)
                    }
                    "sqrt" -> {
                        if (a < 0) throw ArithmeticException("Domain error")
                        sqrt(a)
                    }
                    "abs" -> abs(a)
                    "fact" -> factorial(a.toInt())
                    else -> throw UnsupportedOperationException("Unknown function $token")
                }
                stack.addLast(res)
            } else if (isOperator(token)) {
                val b = stack.removeLastOrNull() ?: throw IllegalArgumentException("Missing operand")
                val a = stack.removeLastOrNull() ?: throw IllegalArgumentException("Missing operand")
                val res = when (token) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> {
                        if (b == 0.0) throw ArithmeticException("Division by zero")
                        a / b
                    }
                    "^" -> a.pow(b)
                    "%" -> a % b
                    else -> throw UnsupportedOperationException("Unknown operator $token")
                }
                stack.addLast(res)
            }
        }

        return stack.singleOrNull() ?: throw IllegalArgumentException("Invalid expression")
    }

    private fun factorial(n: Int): Double {
        if (n < 0 || n > 20) throw ArithmeticException("Factorial out of range")
        var res = 1.0
        for (i in 2..n) res *= i
        return res
    }

    fun formatResult(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            val rounded = (round(value * 1000000.0) / 1000000.0)
            if (abs(rounded) < 1e-6 && rounded != 0.0) {
                String.format("%.4e", value)
            } else {
                rounded.toString()
            }
        }
    }
}
