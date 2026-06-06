public class Token {

    private final TipoToken tipo;
    private final String lexema;
    private final int linha;

    public Token(TipoToken tipo, String lexema, int linha) {
        this.tipo   = tipo;
        this.lexema = lexema;
        this.linha  = linha;
    }

    public TipoToken getTipo()  { return tipo;   }
    public String    getLexema(){ return lexema;  }
    public int       getLinha() { return linha;   }

    @Override
    public String toString() {
        return String.format("Linha %3d | %-16s | %s", linha, tipo, lexema);
    }
}
