import Foundation

enum PPFMath {
    static func isPrime(_ n: Int) -> Bool {
        guard n >= 2 else { return false }
        return ppf_is_prime_u64(UInt64(n))
    }

    enum SpecialFamily: String, CaseIterable, Identifiable {
        case twins = "Gêmeos"
        case sophie = "Sophie Germain"
        case cousins = "Cousins"
        case sexy = "Sexy"
        case palindromes = "Palíndromos"
        case fermat = "Fermat"

        var id: Self { self }

        var nativeID: Int32 {
            switch self {
            case .twins: return Int32(PPF_SPECIAL_TWINS)
            case .sophie: return Int32(PPF_SPECIAL_SOPHIE)
            case .cousins: return Int32(PPF_SPECIAL_COUSINS)
            case .sexy: return Int32(PPF_SPECIAL_SEXY)
            case .palindromes: return Int32(PPF_SPECIAL_PALINDROMES)
            case .fermat: return Int32(PPF_SPECIAL_FERMAT)
            }
        }
    }

    static let specialPrimeCountLimit = Int(PPF_SPECIAL_MAX_COUNT)

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
