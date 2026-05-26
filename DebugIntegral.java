// Debug da função calcularIntegralLogaritmicaBigInteger
// Teste com número de 1233 dígitos

import java.math.BigInteger;

public class DebugIntegral {
    public static void main(String[] args) {
        // Criar número de 1233 dígitos
        StringBuilder sb = new StringBuilder();
        sb.append("1");
        for (int i = 0; i < 1232; i++) {
            sb.append("0");
        }
        
        BigInteger x = new BigInteger(sb.toString());
        
        System.out.println("Número de dígitos: " + x.toString().length());
        System.out.println("Bit length: " + x.bitLength());
        System.out.println("Bit count: " + x.bitCount());
        
        // Testar cada função individualmente
        System.out.println("\n=== TESTE 1: calcularLogaritmoNaturalBigInteger ===");
        try {
            double logX = calcularLogaritmoNaturalBigInteger(x);
            System.out.println("logX = " + logX);
            System.out.println("logX é finito: " + Double.isFinite(logX));
        } catch (Exception e) {
            System.out.println("ERRO em calcularLogaritmoNaturalBigInteger: " + e.getMessage());
        }
        
        System.out.println("\n=== TESTE 2: calcularXSobreLogXBigInteger ===");
        try {
            double logX = calcularLogaritmoNaturalBigInteger(x);
            double xSobreLogX = calcularXSobreLogXBigInteger(x, logX);
            System.out.println("xSobreLogX = " + xSobreLogX);
            System.out.println("xSobreLogX é finito: " + Double.isFinite(xSobreLogX));
        } catch (Exception e) {
            System.out.println("ERRO em calcularXSobreLogXBigInteger: " + e.getMessage());
        }
        
        System.out.println("\n=== TESTE 3: calcularIntegralLogaritmicaAssintotica ===");
        try {
            double li = calcularIntegralLogaritmicaAssintotica(x);
            System.out.println("Li(x) = " + li);
            System.out.println("Li(x) é finito: " + Double.isFinite(li));
        } catch (Exception e) {
            System.out.println("ERRO em calcularIntegralLogaritmicaAssintotica: " + e.getMessage());
        }
        
        System.out.println("\n=== TESTE 4: calcularIntegralLogaritmicaBigInteger ===");
        try {
            double resultado = calcularIntegralLogaritmicaBigInteger(x);
            System.out.println("Resultado final = " + resultado);
            System.out.println("Resultado é finito: " + Double.isFinite(resultado));
        } catch (Exception e) {
            System.out.println("ERRO em calcularIntegralLogaritmicaBigInteger: " + e.getMessage());
        }
    }
    
    private static double calcularLogaritmoNaturalBigInteger(BigInteger x) {
        int bits = x.bitLength();
        
        if (bits <= 53) {
            return Math.log(x.doubleValue());
        }
        
        double ln2 = Math.log(2.0);
        double aproximacao = bits * ln2;
        
        double densidadeBits = (double) x.bitCount() / bits;
        double fatorCorrecao = 1.0 - 0.1 * (1.0 - densidadeBits);
        
        return aproximacao * fatorCorrecao;
    }
    
    private static double calcularXSobreLogXBigInteger(BigInteger x, double logX) {
        int bits = x.bitLength();
        
        if (bits <= 53) {
            return x.doubleValue() / logX;
        }
        
        double ln2 = Math.log(2.0);
        double lnBits = Math.log(bits);
        double lnLn2 = Math.log(ln2);
        
        double expoente = bits * ln2 - (lnBits + lnLn2);
        
        System.out.println("  bits = " + bits);
        System.out.println("  ln2 = " + ln2);
        System.out.println("  lnBits = " + lnBits);
        System.out.println("  lnLn2 = " + lnLn2);
        System.out.println("  expoente = " + expoente);
        
        if (expoente > 700) {
            double resultado = Math.pow(10, expoente / Math.log(10));
            System.out.println("  Usando aproximação para expoente > 700: " + resultado);
            return resultado;
        }
        
        double aproximacao = Math.exp(expoente);
        System.out.println("  Math.exp(expoente) = " + aproximacao);
        
        double densidadeBits = (double) x.bitCount() / bits;
        double fatorCorrecao = 1.0 + 0.05 * (1.0 - densidadeBits);
        
        double resultado = aproximacao * fatorCorrecao;
        System.out.println("  densidadeBits = " + densidadeBits);
        System.out.println("  fatorCorrecao = " + fatorCorrecao);
        System.out.println("  resultado final = " + resultado);
        
        return resultado;
    }
    
    private static double calcularIntegralLogaritmicaAssintotica(BigInteger x) {
        double logX = calcularLogaritmoNaturalBigInteger(x);
        
        double primeiroTermo = 1.0;
        double segundoTermo = 1.0 / logX;
        double terceiroTermo = 2.0 / (logX * logX);
        
        System.out.println("  logX = " + logX);
        System.out.println("  segundoTermo = " + segundoTermo);
        System.out.println("  terceiroTermo = " + terceiroTermo);
        
        double fatorAssintotico = primeiroTermo + segundoTermo + terceiroTermo;
        System.out.println("  fatorAssintotico = " + fatorAssintotico);
        
        double xSobreLogX = calcularXSobreLogXBigInteger(x, logX);
        System.out.println("  xSobreLogX = " + xSobreLogX);
        
        double resultado = xSobreLogX * fatorAssintotico;
        System.out.println("  resultado final = " + resultado);
        
        return resultado;
    }
    
    private static double calcularIntegralLogaritmicaBigInteger(BigInteger x) {
        if (x.compareTo(BigInteger.valueOf(2)) < 0) return 0.0;
        
        if (x.toString().length() > 15) {
            return calcularIntegralLogaritmicaAssintotica(x);
        }
        
        try {
            double xDouble = x.doubleValue();
            if (Double.isFinite(xDouble) && xDouble < 1e15) {
                return calcularIntegralLogaritmicaNumerica(xDouble);
            }
        } catch (Exception e) {
            // Se falhou, usar método assintótico
        }
        
        return calcularIntegralLogaritmicaAssintotica(x);
    }
    
    private static double calcularIntegralLogaritmicaNumerica(double x) {
        // Implementação simplificada para teste
        return x / Math.log(x);
    }
}
