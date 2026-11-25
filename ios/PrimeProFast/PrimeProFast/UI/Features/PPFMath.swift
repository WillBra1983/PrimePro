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
        return primeFlags(upTo: limit).enumerated().compactMap { $1 ? $0 : nil }
    }

    private static func primeFlags(upTo limit: Int) -> [Bool] {
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
        return sieve
    }

    enum SpecialFamily: String, CaseIterable, Identifiable {
        case twins = "Gêmeos"
        case sophie = "Sophie Germain"
        case cousins = "Cousins"
        case sexy = "Sexy"
        case palindromes = "Palíndromos"
        case fermat = "Fermat"

        var id: Self { self }

        func partner(of prime: Int) -> Int? {
            switch self {
            case .twins: return prime + 2
            case .sophie: return 2 * prime + 1
            case .cousins: return prime + 4
            case .sexy: return prime + 6
            case .palindromes, .fermat: return nil
            }
        }
    }

    static let specialPrimeLimit = 10_000_000
    static let specialPrimeCountLimit = 1_000

    static func specialPrimes(_ family: SpecialFamily, limit: Int, count: Int) -> String {
        guard (1...specialPrimeCountLimit).contains(count) else { return "" }
        if family == .fermat {
            let known = [3, 5, 17, 257, 65537]
            let lines = known.prefix(count).enumerated().map { "F\($0.offset) = \($0.element)" }
            return "Primos de Fermat conhecidos (\(lines.count) de 5)\n\n" + lines.joined(separator: "\n")
        }
        guard (2...specialPrimeLimit).contains(limit) else { return "" }
        let sieve = primeFlags(upTo: limit)
        var sample: [Int] = []
        var matches = 0
        // Reservoir sampling keeps a uniform sample without storing every match.
        for p in 2...limit where sieve[p] {
            if let q = family.partner(of: p) {
                guard q <= limit, sieve[q] else { continue }
            } else {
                let digits = String(p)
                guard digits.elementsEqual(digits.reversed()) else { continue }
            }
            matches += 1
            if sample.count < count {
                sample.append(p)
            } else {
                let slot = Int.random(in: 0..<matches)
                if slot < count { sample[slot] = p }
            }
        }
        guard !sample.isEmpty else { return "Nenhum resultado até \(limit.formatted())." }
        let lines = sample.shuffled().map { p -> String in
            guard let q = family.partner(of: p) else { return String(p) }
            return family == .sophie ? "\(p) (2 × \(p) + 1 = \(q))" : "(\(p), \(q))"
        }
        return "\(family.rawValue) | \(lines.count) de \(matches) até \(limit.formatted())\n\n" +
            lines.joined(separator: "\n")
    }

    static func mersennePrimes(maxExponent: Int) -> String {
        var lines: [String] = []
        for exp in primesUpTo(maxExponent) {
            if let candidate = PPFNative.evaluateInteger("2^\(exp)-1"),
               PPFNative.testarPrimalidade(candidate) {
                lines.append("2^\(exp) - 1 = \(candidate)")
            }
        }
        return lines.isEmpty ? "Nenhum encontrado no limite." : "Mersenne provavelmente primos (\(lines.count)):\n" + lines.joined(separator: "\n")
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

    static func properDivisorSum(_ n: Int) -> Int {
        guard n > 1 else { return 0 }
        var remaining = n
        var sum = 1
        var factor = 2
        while factor <= remaining / factor {
            if remaining.isMultiple(of: factor) {
                var power = 1
                var term = 1
                while remaining.isMultiple(of: factor) {
                    remaining /= factor
                    power *= factor
                    term += power
                }
                sum *= term
            }
            factor += factor == 2 ? 1 : 2
        }
        if remaining > 1 { sum *= 1 + remaining }
        return sum - n
    }

}
