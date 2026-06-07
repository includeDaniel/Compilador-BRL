import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AnalisadorLexico {

    private static final int MAX_ID = 512;

    private static final Map<String, TipoToken> PALAVRAS_RESERVADAS = new HashMap<>();

    static {
        PALAVRAS_RESERVADAS.put("inteiro",    TipoToken.TIPO_INTEIRO);
        PALAVRAS_RESERVADAS.put("real",       TipoToken.TIPO_REAL);
        PALAVRAS_RESERVADAS.put("logico",     TipoToken.TIPO_LOGICO);
        PALAVRAS_RESERVADAS.put("caractere",  TipoToken.TIPO_CARACTERE);
        PALAVRAS_RESERVADAS.put("se",         TipoToken.SE);
        PALAVRAS_RESERVADAS.put("senao",      TipoToken.SENAO);
        PALAVRAS_RESERVADAS.put("entao",      TipoToken.ENTAO);
        PALAVRAS_RESERVADAS.put("enquanto",   TipoToken.ENQUANTO);
        PALAVRAS_RESERVADAS.put("faca",       TipoToken.FACA);
        PALAVRAS_RESERVADAS.put("inicio",     TipoToken.INICIO);
        PALAVRAS_RESERVADAS.put("fim",        TipoToken.FIM);
        PALAVRAS_RESERVADAS.put("leitura",    TipoToken.LEITURA);
        PALAVRAS_RESERVADAS.put("leia",       TipoToken.LEITURA);
        PALAVRAS_RESERVADAS.put("escrita",    TipoToken.ESCRITA);
        PALAVRAS_RESERVADAS.put("escreva",    TipoToken.ESCRITA);
        PALAVRAS_RESERVADAS.put("ou",         TipoToken.OU);
        PALAVRAS_RESERVADAS.put("nao",        TipoToken.NAO);
        PALAVRAS_RESERVADAS.put("not",        TipoToken.NAO); // alias conforme seção 2.6
        PALAVRAS_RESERVADAS.put("div",        TipoToken.DIV);
        PALAVRAS_RESERVADAS.put("mod",        TipoToken.MOD);
        PALAVRAS_RESERVADAS.put("verdadeiro", TipoToken.VERDADEIRO);
        PALAVRAS_RESERVADAS.put("falso",      TipoToken.FALSO);
    }

    private final Reader reader;
    private final TabelaSimbolos tabela;
    private int charAtual;
    private int linha;

    public AnalisadorLexico(Reader reader, TabelaSimbolos tabela) throws IOException {
        this.reader    = reader;
        this.tabela    = tabela;
        this.linha     = 1;
        this.charAtual = reader.read();
    }

    // Conforme seção 2.8: apenas espaço (0x20) e quebra de linha (0x0D / 0x0A)
    private boolean isDelimitador(int c) {
        return c == ' ' || c == '\n' || c == '\r';
    }

    // ---------------------------------------------------------------
    private void avancar() throws IOException {
        if (charAtual == '\n') linha++;
        charAtual = reader.read();
    }

    // ---------------------------------------------------------------
    public Token proximoToken() throws IOException {

        while (charAtual != -1 && isDelimitador(charAtual)) {
            avancar();
        }

        if (charAtual == -1) return new Token(TipoToken.EOF, "EOF", linha);

        int linhaToken = linha;
        char c = (char) charAtual;

        // Comentário /* ... */
        if (c == '/') {
            avancar();
            if (charAtual == '*') return lerComentario(linhaToken);
            return new Token(TipoToken.DIV_OP, "/", linhaToken);
        }

        // Literal de string
        if (c == '"') return lerString(linhaToken);

        // Número
        if (Character.isDigit(c)) return lerNumero(linhaToken);

        // Identificador ou palavra reservada
        if (Character.isLetter(c) || c == '_') return lerIdentificador(linhaToken);

        // Operadores e delimitadores
        return lerOperadorOuDelimitador(c, linhaToken);
    }

    // ---------------------------------------------------------------
    private Token lerComentario(int linhaInicio) throws IOException {
        avancar(); // consome '*'
        while (charAtual != -1) {
            if (charAtual == '*') {
                avancar();
                if (charAtual == '/') { avancar(); return proximoToken(); }
            } else {
                avancar();
            }
        }
        throw new ErroCompilacao("Comentario nao fechado (falta */)", linhaInicio);
    }

    // ---------------------------------------------------------------
    private Token lerString(int linhaInicio) throws IOException {
        avancar(); // consome '"' inicial
        StringBuilder sb = new StringBuilder();
        while (charAtual != -1 && charAtual != '"') {
            if (charAtual == '\n' || charAtual == '\r') {
                throw new ErroCompilacao("String nao fechada antes da quebra de linha", linhaInicio);
            }
            sb.append((char) charAtual);
            avancar();
        }
        if (charAtual == '"') { avancar(); return new Token(TipoToken.CONST_STR, "\"" + sb + "\"", linhaInicio); }
        throw new ErroCompilacao("String nao fechada antes do fim do arquivo", linhaInicio);
    }

    // ---------------------------------------------------------------
    private Token lerNumero(int linhaInicio) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (charAtual != -1 && Character.isDigit(charAtual)) {
            sb.append((char) charAtual); avancar();
        }
        if (charAtual == '.') {
            sb.append('.'); avancar();
            if (!Character.isDigit(charAtual))
                throw new ErroCompilacao("Numero real malformado: '" + sb + "' (faltam digitos apos o ponto)", linhaInicio);
            while (charAtual != -1 && Character.isDigit(charAtual)) {
                sb.append((char) charAtual); avancar();
            }
            return new Token(TipoToken.CONST_REAL, sb.toString(), linhaInicio);
        }
        return new Token(TipoToken.CONST_INT, sb.toString(), linhaInicio);
    }

    // ---------------------------------------------------------------
    private Token lerIdentificador(int linhaInicio) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (charAtual != -1 && (Character.isLetterOrDigit(charAtual) || charAtual == '_')) {
            if (sb.length() >= MAX_ID) {
                while (charAtual != -1 && (Character.isLetterOrDigit(charAtual) || charAtual == '_')) avancar();
                throw new ErroCompilacao("Identificador excede o limite de " + MAX_ID + " caracteres", linhaInicio);
            }
            sb.append((char) charAtual); avancar();
        }
        String lexema = sb.toString();
        TipoToken tipo = PALAVRAS_RESERVADAS.getOrDefault(lexema, TipoToken.ID);
        Token token = new Token(tipo, lexema, linhaInicio);

        // Identificadores são inseridos na tabela de símbolos ao serem encontrados
        if (tipo == TipoToken.ID) tabela.inserirId(token);

        return token;
    }

    // ---------------------------------------------------------------
    private Token lerOperadorOuDelimitador(char c, int linhaToken) throws IOException {
        avancar();
        switch (c) {
            case '+': return new Token(TipoToken.MAIS,          "+",  linhaToken);
            case '-': return new Token(TipoToken.MENOS,         "-",  linhaToken);
            case '*': return new Token(TipoToken.MULT,          "*",  linhaToken);
            case ';': return new Token(TipoToken.PONTO_VIRGULA, ";",  linhaToken);
            case ',': return new Token(TipoToken.VIRGULA,       ",",  linhaToken);
            case '(': return new Token(TipoToken.ABRE_PAR,      "(",  linhaToken);
            case ')': return new Token(TipoToken.FECHA_PAR,     ")",  linhaToken);
            case '[': return new Token(TipoToken.ABRE_COL,      "[",  linhaToken);
            case ']': return new Token(TipoToken.FECHA_COL,     "]",  linhaToken);
            case '{': return new Token(TipoToken.ABRE_CHAVE,    "{",  linhaToken);
            case '}': return new Token(TipoToken.FECHA_CHAVE,   "}",  linhaToken);
            case '=':
                if (charAtual == '=') { avancar(); return new Token(TipoToken.IGUAL,        "==", linhaToken); }
                return new Token(TipoToken.IGUAL_SIMPLES, "=", linhaToken);
            case ':':
                if (charAtual == '=') { avancar(); return new Token(TipoToken.ATRIB,        ":=", linhaToken); }
                return new Token(TipoToken.DOIS_PONTOS, ":", linhaToken);
            case '<':
                if (charAtual == '=') { avancar(); return new Token(TipoToken.MENOR_IGUAL,  "<=", linhaToken); }
                if (charAtual == '>') { avancar(); return new Token(TipoToken.DIFERENTE,    "<>", linhaToken); }
                return new Token(TipoToken.MENOR, "<", linhaToken);
            case '>':
                if (charAtual == '=') { avancar(); return new Token(TipoToken.MAIOR_IGUAL,  ">=", linhaToken); }
                return new Token(TipoToken.MAIOR, ">", linhaToken);
            case '&':
                if (charAtual == '&') { avancar(); return new Token(TipoToken.E_LOGICO, "&&", linhaToken); }
                throw new ErroCompilacao("Simbolo invalido: '&' (use '&&' para E logico)", linhaToken);

            // Caracteres permitidos pelo spec (seção 2.2) mas sem token próprio fora de strings
            case '\'': case '\\': case '|': case '!': case '?':
                throw new ErroCompilacao(
                    "Simbolo '" + c + "' valido apenas dentro de strings", linhaToken);

            default:
                throw new ErroCompilacao("Simbolo invalido: '" + c + "'", linhaToken);
        }
    }
}
