package com.sudokuengine

/**
 * Minimal, dependency-free JSON parser for test fixtures only.
 *
 * Produces a tree of [Map]<String, Any?> (objects), [List]<Any?> (arrays), [String], [Double],
 * [Boolean], and `null`. Sufficient for reading the golden-puzzle fixture without pulling in a
 * third-party library (which the project bans).
 */
internal object Json {
    fun parse(text: String): Any? {
        val parser = Parser(text)
        val value = parser.parseValue()
        parser.skipWhitespace()
        require(parser.atEnd()) { "trailing characters after JSON value" }
        return value
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun atEnd(): Boolean = i >= s.length

        fun skipWhitespace() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun parseValue(): Any? {
            skipWhitespace()
            return when (val c = s[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                else ->
                    if (c == '-' || c.isDigit()) {
                        parseNumber()
                    } else {
                        error("unexpected character '$c' at $i")
                    }
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val map = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                i++
                return map
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                map[key] = parseValue()
                skipWhitespace()
                when (val c = s[i++]) {
                    ',' -> continue
                    '}' -> return map
                    else -> error("expected ',' or '}' but found '$c'")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = ArrayList<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                i++
                return list
            }
            while (true) {
                list.add(parseValue())
                skipWhitespace()
                when (val c = s[i++]) {
                    ',' -> continue
                    ']' -> return list
                    else -> error("expected ',' or ']' but found '$c'")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                when (val c = s[i++]) {
                    '"' -> return sb.toString()
                    '\\' ->
                        when (val esc = s[i++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                val hex = s.substring(i, i + 4)
                                i += 4
                                sb.append(hex.toInt(16).toChar())
                            }
                            else -> error("invalid escape '\\$esc'")
                        }
                    else -> sb.append(c)
                }
            }
        }

        private fun parseNumber(): Double {
            val start = i
            if (peek() == '-') i++
            while (i < s.length && (s[i].isDigit() || s[i] in ".eE+-")) i++
            return s.substring(start, i).toDouble()
        }

        private fun parseBoolean(): Boolean =
            if (s.startsWith("true", i)) {
                i += 4
                true
            } else {
                require(s.startsWith("false", i)) { "invalid literal at $i" }
                i += 5
                false
            }

        private fun parseNull(): Any? {
            require(s.startsWith("null", i)) { "invalid literal at $i" }
            i += 4
            return null
        }

        private fun peek(): Char = s[i]

        private fun expect(c: Char) {
            require(s[i] == c) { "expected '$c' at $i but found '${s[i]}'" }
            i++
        }
    }
}
