import Foundation

enum PPFMath {
    static func isPrime(_ n: Int) -> Bool {
        if n < 2 { return false }
        if n < 4 { return true }
        if n % 2 == 0 { return false }
        var i = 3
        while i * i <= n {
            if n % i == 0 { return false }
            i += 2
        }
        return true
    }

    static func primesUpTo(_ limit: Int) -> [Int] {
        guard limit >= 2 else { return [] }
        var sieve = [Bool](repeating: true, count: limit + 1)
        sieve[0] = false
        sieve[1] = false
        var p = 2
        while p * p <= limit {
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

    static func mersennePrimes(maxExponent: Int, maxCount: Int) -> String {
        var lines: [String] = []
        var found = 0
        for p in primesUpTo(maxExponent) where found < maxCount {
            let exp = p
            if exp > 30 { break }
            let candidate = (1 << exp) - 1
            if isPrime(candidate) {
                lines.append("2^\(exp)−1 = \(candidate)")
                found += 1
            }
        }
        return lines.isEmpty ? "Nenhum encontrado no limite." : lines.joined(separator: "\n")
    }

    static func perfectNumbers(mode: Int, limit: Int) -> String {
        var result: [String] = []
        for p in primesUpTo(20) {
            let mersenne = (1 << p) - 1
            if !isPrime(mersenne) { continue }
            let perfect = (1 << (p - 1)) * mersenne
            if perfect > limit { break }
            result.append("Perfeito: \(perfect) (via 2^\(p - 1) * (2^\(p) - 1))")
        }
        return result.joined(separator: "\n")
    }

    private static func formatPairs(_ pairs: [(Int, Int)], title: String) -> String {
        if pairs.isEmpty { return "Nenhum par encontrado." }
        return title + ":\n" + pairs.map { "(\($0.0), \($0.1))" }.joined(separator: "\n")
    }
}
