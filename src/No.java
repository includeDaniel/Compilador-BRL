import java.util.ArrayList;
import java.util.List;

public class No {

    private final TipoNo    tipo;
    private final String    valor;    // lexema, operador ou tipo declarado
    private final int       linha;
    private String          tipoDado; // preenchido pelo analisador semântico
    private final List<No>  filhos = new ArrayList<>();

    public No(TipoNo tipo, int linha) {
        this(tipo, null, linha);
    }

    public No(TipoNo tipo, String valor, int linha) {
        this.tipo  = tipo;
        this.valor = valor;
        this.linha = linha;
    }

    public void        addFilho(No f)       { filhos.add(f); }
    public TipoNo      getTipo()            { return tipo; }
    public String      getValor()           { return valor; }
    public int         getLinha()           { return linha; }
    public String      getTipoDado()        { return tipoDado; }
    public void        setTipoDado(String t){ tipoDado = t; }
    public List<No>    getFilhos()          { return filhos; }
    public No          getFilho(int i)      { return filhos.get(i); }
    public int         numFilhos()          { return filhos.size(); }
}
