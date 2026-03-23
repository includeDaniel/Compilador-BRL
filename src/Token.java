public class Token {

    private TipoToken tipo;
    private String lexema;

    public Token(TipoToken tipo, String lexema) {
        this.tipo = tipo;
        this.lexema = lexema;
    }

    public TipoToken getTipo() {
        return tipo;
    }

    public String getLexema() {
        return lexema;
    }

    @Override
    public String toString() {
        return tipo + " -> " + lexema;
    }
}