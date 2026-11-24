import Foundation

enum PPFMath {
    static func isPrime(_ n: Int) -> Bool {
        guard n >= 2 else { return false }
        return isPrime64(UInt64(n))
    }

    private static func isPrime64(_ n: UInt64) -> Bool {
        if n < 2 { return false }
        let smallPrimes: [UInt64] = [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37]
        for p in smallPrimes {
            if n == p { return true }
            if n % p == 0 { return false }
        }

        var d = n - 1
        var s = 0
        while d % 2 == 0 {
            d /= 2
            s += 1
        }

        let bases: [UInt64] = [2, 325, 9_375, 28_178, 450_775, 9_780_504, 1_795_265_022]
        for base in bases {
            let a = base % n
            if a == 0 {
                continue
            }
            var x = powMod(a, d, n)
            if x == 1 || x == n - 1 {
                continue
            }

            var passed = false
            if s > 1 {
                for _ in 0..<(s - 1) {
                    x = mulMod(x, x, n)
                    if x == n - 1 {
                        passed = true
                        break
                    }
                }
            }
            if !passed {
                return false
            }
        }
        return true
    }

    private static func powMod(_ base: UInt64, _ exponent: UInt64, _ modulus: UInt64) -> UInt64 {
        var result: UInt64 = 1
        var b = base % modulus
        var e = exponent
        while e > 0 {
            if e & 1 == 1 {
                result = mulMod(result, b, modulus)
            }
            e >>= 1
            if e > 0 {
                b = mulMod(b, b, modulus)
            }
        }
        return result
    }

    private static func mulMod(_ a: UInt64, _ b: UInt64, _ modulus: UInt64) -> UInt64 {
        let product = a.multipliedFullWidth(by: b)
        return modulus.dividingFullWidth(product).remainder
    }

    static func primesUpTo(_ limit: Int) -> [Int] {
        guard limit >= 2 else { return [] }
        var sieve = [Bool](repeating: true, count: limit + 1)
        sieve[0] = false
        sieve[1] = false
        var p = 2
        while p <= limit / p {
            if sieve[p] {
                var m = p * p
                while m <= limit {
                    sieve[m] = false
                    m += p
                }
            }
            p += 1
        }
        return sieve.enumerated().compactMap { $1 ? $0 : nil }
    }

    static func twinPrimes(limit: Int, count: Int) -> String {
        var found: [(Int, Int)] = []
        var p = 3
        while p < limit && found.count < count {
            if isPrime(p), isPrime(p + 2) {
                found.append((p, p + 2))
            }
            p += 2
        }
        return formatPairs(found, title: "Primos gêmeos")
    }

    static func sophieGermain(limit: Int, count: Int) -> String {
        var found: [Int] = []
        for p in primesUpTo(limit) where found.count < count {
            let q = 2 * p + 1
            if q <= limit, isPrime(q) {
                found.append(p)
            }
        }
        return "Sophie Germain (\(found.count)):\n" + found.map(String.init).joined(separator: ", ")
    }

    static func cousinPrimes(limit: Int, count: Int) -> String {
        primePairs(limit: limit, count: count, gap: 4, title: "Primos cousins")
    }

    static func sexyPrimes(limit: Int, count: Int) -> String {
        primePairs(limit: limit, count: count, gap: 6, title: "Primos sexy")
    }

    static func palindromicPrimes(limit: Int, count: Int) -> String {
        var found: [Int] = []
        for p in primesUpTo(limit) where found.count < count {
            let text = String(p)
            if text == String(text.reversed()) {
                found.append(p)
            }
        }
        if found.isEmpty { return "Nenhum primo palíndromo encontrado." }
        return "Primos palíndromos (\(found.count)):\n" + found.map(String.init).joined(separator: ", ")
    }

    static func fermatPrimes(limit: Int, count: Int) -> String {
        var lines: [String] = []
        var n = 0
        while lines.count < count {
            let exponent = 1 << n
            if exponent >= 63 {
                break
            }
            let value = (UInt64(1) << UInt64(exponent)) + 1
            if value > UInt64(limit) {
                break
            }
            if isPrime64(value) {
                lines.append("F\(n) = 2^\(exponent) + 1 = \(value)")
            }
            n += 1
        }
        return lines.isEmpty ? "Nenhum primo de Fermat encontrado no limite." : "Primos de Fermat (\(lines.count)):\n" + lines.joined(separator: "\n")
    }

    static func mersennePrimes(maxExponent: Int) -> String {
        var lines: [String] = []
        var primeCount = 0
        var stoppedAt64Bit = false
        for exp in primesUpTo(maxExponent) {
            if exp >= 63 {
                stoppedAt64Bit = true
                break
            }
            let candidate = (UInt64(1) << UInt64(exp)) - 1
            if isPrime64(candidate) {
                lines.append("2^\(exp) - 1 = \(candidate)")
                primeCount += 1
            }
        }
        if stoppedAt64Bit {
            lines.append("Observação: expoentes >= 63 exigem a rota nativa/GMP para decimal completo.")
        }
        return primeCount == 0 ? "Nenhum encontrado no limite." : "Mersenne primos (\(primeCount)):\n" + lines.joined(separator: "\n")
    }

    static func perfectNumbers(mode: Int, limit: Int) -> String {
        var result: [String] = []
        for p in primesUpTo(62) {
            let mersenne = (UInt64(1) << UInt64(p)) - 1
            if !isPrime64(mersenne) { continue }

            let multiplier = UInt64(1) << UInt64(p - 1)
            let multiplied = multiplier.multipliedReportingOverflow(by: mersenne)
            if multiplied.overflow {
                break
            }
            let perfect = multiplied.partialValue
            if perfect > UInt64(Int.max) {
                break
            }
            if perfect > UInt64(limit) { break }
            result.append("Perfeito: \(perfect) (via 2^\(p - 1) * (2^\(p) - 1))")
        }
        return result.isEmpty ? "Nenhum número perfeito encontrado no limite." : result.joined(separator: "\n")
    }

    private static func formatPairs(_ pairs: [(Int, Int)], title: String) -> String {
        if pairs.isEmpty { return "Nenhum par encontrado." }
        return title + ":\n" + pairs.map { "(\($0.0), \($0.1))" }.joined(separator: "\n")
    }

    private static func primePairs(limit: Int, count: Int, gap: Int, title: String) -> String {
        var found: [(Int, Int)] = []
        var p = 3
        while p + gap <= limit && found.count < count {
            if isPrime(p), isPrime(p + gap) {
                found.append((p, p + gap))
            }
            p += 2
        }
        return formatPairs(found, title: title)
    }
}
