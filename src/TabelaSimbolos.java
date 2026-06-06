import java.util.LinkedHashMap;
import java.util.Map;

public class TabelaSimbolos {

    private static class Simbolo {
        String tipo;   // null enquanto não declarado; preenchido pelo semântico
        final Token token;

        Simbolo(String tipo, Token token) {
            this.tipo  = tipo;
            this.token = token;
        }
    }

    private final Map<String, Simbolo> tabela = new LinkedHashMap<>();

    /** Inserção pelo léxico: registra o identificador sem tipo ainda. */
    public void inserirId(Token token) {
        tabela.putIfAbsent(token.getLexema(), new Simbolo(null, token));
    }

    /** Atualização pelo semântico: define o tipo do símbolo já registrado. */
    public void definirTipo(String nome, String tipo, Token tokenDecl) {
        Simbolo s = tabela.get(nome);
        if (s != null) {
            s.tipo = tipo;
        } else {
            tabela.put(nome, new Simbolo(tipo, tokenDecl));
        }
    }

    public boolean contem(String nome) {
        return tabela.containsKey(nome);
    }

    /** Retorna o tipo declarado, ou null se ainda não declarado. */
    public String getTipo(String nome) {
        Simbolo s = tabela.get(nome);
        return s != null ? s.tipo : null;
    }

    public Token getToken(String nome) {
        Simbolo s = tabela.get(nome);
        return s != null ? s.token : null;
    }

    public void imprimir() {
        if (tabela.isEmpty()) return;
        System.out.println("\n=== TABELA DE SIMBOLOS ===");
        tabela.forEach((k, v) ->
            System.out.printf("%-30s | %s%n", k, v.tipo != null ? v.tipo : "(nao declarado)"));
    }
}
