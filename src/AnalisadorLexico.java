import java.util.regex.Pattern;

public class AnalisadorLexico {

    private static final Pattern ID = Pattern.compile("[a-zA-Z][a-zA-Z0-9]*");
    private static final Pattern INTEIRO = Pattern.compile("[0-9]+");
    private static final Pattern REAL = Pattern.compile("[0-9]+\\.[0-9]+");
    private static final Pattern CARACTERE = Pattern.compile("[a-zA-Z]+");
    private static final Pattern LOGICO = Pattern.compile("(verdadeiro|falso|0|1)");

    public Token analisar(String lexema) {

        if (REAL.matcher(lexema).matches()) {
            return new Token(TipoToken.REAL, lexema);
        }

        if (INTEIRO.matcher(lexema).matches()) {
            return new Token(TipoToken.INTEIRO, lexema);
        }

        if (LOGICO.matcher(lexema).matches()) {
            return new Token(TipoToken.LOGICO, lexema);
        }

        if (CARACTERE.matcher(lexema).matches()) {
            return new Token(TipoToken.CARACTERE, lexema);
        }

        if (ID.matcher(lexema).matches()) {
            return new Token(TipoToken.ID, lexema);
        }

        return new Token(TipoToken.INVALIDO, lexema);
    }
}