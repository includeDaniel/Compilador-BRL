import java.io.IOException;

/**
 * Parser recursivo descendente para a linguagem BRL.
 *
 * Gramática:
 *   programa     → INICIO ID ';' bloco_decl bloco_instr FIM
 *   bloco_decl   → (declaracao)*
 *   declaracao   → ID (',' ID)* ':' tipo ';'
 *   tipo         → inteiro | real | logico | caractere
 *   bloco_instr  → (instrucao)*
 *   instrucao    → atrib | cmd_se | cmd_enquanto | cmd_leitura | cmd_escrita
 *   atrib        → ID ':=' expressao ';'
 *   cmd_se       → SE exp ENTAO INICIO bloco_instr FIM (SENAO INICIO bloco_instr FIM)?
 *   cmd_enquanto → ENQUANTO exp FACA INICIO bloco_instr FIM
 *   cmd_leitura  → LEITURA '(' ID (',' ID)* ')' ';'
 *   cmd_escrita  → ESCRITA '(' exp ')' ';'
 *   exp          → expRel
 *   expRel       → expAdit (RELOP expAdit)?
 *   expAdit      → expMult (('+' | '-' | OU) expMult)*
 *   expMult      → expUnaria (('*'|'/'|DIV|MOD|E_LOGICO) expUnaria)*
 *   expUnaria    → NAO expUnaria | ('+' | '-') expUnaria | expPrimaria
 *   expPrimaria  → ID | CONST_INT | CONST_REAL | CONST_STR
 *                | VERDADEIRO | FALSO | '(' exp ')'
 */
public class AnalisadorSintatico {

    private final AnalisadorLexico lexer;
    private Token atual;
    private Token proximo; // 1 token de lookahead além do atual

    public AnalisadorSintatico(AnalisadorLexico lexer) throws IOException {
        this.lexer = lexer;
        atual   = this.lexer.proximoToken();
        proximo = this.lexer.proximoToken();
    }

    // ---------------------------------------------------------------
    // Controle de tokens
    // ---------------------------------------------------------------

    private void avancar() throws IOException {
        atual   = proximo;
        proximo = lexer.proximoToken();
    }

    private Token consumir(TipoToken esperado) throws IOException {
        if (atual.getTipo() != esperado) {
            throw new ErroCompilacao(
                "Esperado '" + esperado + "' mas encontrado '" + atual.getLexema() + "'",
                atual.getLinha());
        }
        Token t = atual;
        avancar();
        return t;
    }

    private boolean verifica(TipoToken tipo) {
        return atual.getTipo() == tipo;
    }

    private boolean verificaProximo(TipoToken tipo) {
        return proximo.getTipo() == tipo;
    }

    // ---------------------------------------------------------------
    // Entrada
    // ---------------------------------------------------------------

    public No parsear() throws IOException {
        No arvore = parsePrograma();
        consumir(TipoToken.EOF);
        return arvore;
    }

    // ---------------------------------------------------------------
    // programa → INICIO ID ';' bloco_decl bloco_instr FIM
    // ---------------------------------------------------------------

    private No parsePrograma() throws IOException {
        int linha = atual.getLinha();
        consumir(TipoToken.INICIO);
        Token nome = consumir(TipoToken.ID);
        consumir(TipoToken.PONTO_VIRGULA);

        No no = new No(TipoNo.PROGRAMA, nome.getLexema(), linha);
        no.addFilho(parseBlocoDecl());
        no.addFilho(parseBlocoInstr());
        consumir(TipoToken.FIM);
        return no;
    }

    // ---------------------------------------------------------------
    // bloco_decl → (declaracao)*
    // Uma declaração começa com ID seguido de ',' ou ':'
    // ---------------------------------------------------------------

    private No parseBlocoDecl() throws IOException {
        No bloco = new No(TipoNo.BLOCO_DECL, atual.getLinha());
        while (verifica(TipoToken.ID) &&
               (verificaProximo(TipoToken.VIRGULA) || verificaProximo(TipoToken.DOIS_PONTOS))) {
            bloco.addFilho(parseDeclaracao());
        }
        return bloco;
    }

    // declaracao → ID (',' ID)* ':' tipo ';'
    private No parseDeclaracao() throws IOException {
        int linha = atual.getLinha();
        No no = new No(TipoNo.DECLARACAO, linha);

        Token id = consumir(TipoToken.ID);
        no.addFilho(new No(TipoNo.EXP_ID, id.getLexema(), id.getLinha()));

        while (verifica(TipoToken.VIRGULA)) {
            avancar();
            Token idExtra = consumir(TipoToken.ID);
            no.addFilho(new No(TipoNo.EXP_ID, idExtra.getLexema(), idExtra.getLinha()));
        }

        consumir(TipoToken.DOIS_PONTOS);

        // tipo — armazenado como valor do nó
        String tipo = parseTipo();
        No noTipo = new No(TipoNo.DECLARACAO, tipo, atual.getLinha());
        no.addFilho(noTipo); // último filho = tipo

        consumir(TipoToken.PONTO_VIRGULA);
        return no;
    }

    private String parseTipo() throws IOException {
        switch (atual.getTipo()) {
            case TIPO_INTEIRO:   avancar(); return "inteiro";
            case TIPO_REAL:      avancar(); return "real";
            case TIPO_LOGICO:    avancar(); return "logico";
            case TIPO_CARACTERE: avancar(); return "caractere";
            default:
                throw new ErroCompilacao(
                    "Tipo invalido: '" + atual.getLexema() + "'", atual.getLinha());
        }
    }

    // ---------------------------------------------------------------
    // bloco_instr → (instrucao)*
    // ---------------------------------------------------------------

    private No parseBlocoInstr() throws IOException {
        No bloco = new No(TipoNo.BLOCO_INSTR, atual.getLinha());
        while (!verifica(TipoToken.FIM) && !verifica(TipoToken.EOF)) {
            bloco.addFilho(parseInstrucao());
        }
        return bloco;
    }

    private No parseInstrucao() throws IOException {
        switch (atual.getTipo()) {
            case ID:       return parseAtrib();
            case SE:       return parseSe();
            case ENQUANTO: return parseEnquanto();
            case LEITURA:  return parseLeitura();
            case ESCRITA:  return parseEscrita();
            default:
                throw new ErroCompilacao(
                    "Instrucao invalida: '" + atual.getLexema() + "'", atual.getLinha());
        }
    }

    // atrib → ID ':=' expressao ';'
    private No parseAtrib() throws IOException {
        int linha = atual.getLinha();
        Token id = consumir(TipoToken.ID);
        consumir(TipoToken.ATRIB);
        No exp = parseExpressao();
        consumir(TipoToken.PONTO_VIRGULA);

        No no = new No(TipoNo.ATRIB, linha);
        no.addFilho(new No(TipoNo.EXP_ID, id.getLexema(), id.getLinha()));
        no.addFilho(exp);
        return no;
    }

    // cmd_se → SE exp ENTAO INICIO bloco_instr FIM (SENAO INICIO bloco_instr FIM)?
    private No parseSe() throws IOException {
        int linha = atual.getLinha();
        consumir(TipoToken.SE);
        No cond = parseExpressao();
        consumir(TipoToken.ENTAO);
        consumir(TipoToken.INICIO);
        No then = parseBlocoInstr();
        consumir(TipoToken.FIM);

        No no = new No(TipoNo.SE, linha);
        no.addFilho(cond);
        no.addFilho(then);

        if (verifica(TipoToken.SENAO)) {
            avancar();
            consumir(TipoToken.INICIO);
            No senao = parseBlocoInstr();
            consumir(TipoToken.FIM);
            no.addFilho(senao);
        }
        return no;
    }

    // cmd_enquanto → ENQUANTO exp FACA INICIO bloco_instr FIM
    private No parseEnquanto() throws IOException {
        int linha = atual.getLinha();
        consumir(TipoToken.ENQUANTO);
        No cond = parseExpressao();
        consumir(TipoToken.FACA);
        consumir(TipoToken.INICIO);
        No corpo = parseBlocoInstr();
        consumir(TipoToken.FIM);

        No no = new No(TipoNo.ENQUANTO, linha);
        no.addFilho(cond);
        no.addFilho(corpo);
        return no;
    }

    // cmd_leitura → LEITURA '(' ID (',' ID)* ')' ';'
    private No parseLeitura() throws IOException {
        int linha = atual.getLinha();
        consumir(TipoToken.LEITURA);
        consumir(TipoToken.ABRE_PAR);

        No no = new No(TipoNo.LEITURA, linha);
        Token id = consumir(TipoToken.ID);
        no.addFilho(new No(TipoNo.EXP_ID, id.getLexema(), id.getLinha()));

        while (verifica(TipoToken.VIRGULA)) {
            avancar();
            Token idExtra = consumir(TipoToken.ID);
            no.addFilho(new No(TipoNo.EXP_ID, idExtra.getLexema(), idExtra.getLinha()));
        }

        consumir(TipoToken.FECHA_PAR);
        consumir(TipoToken.PONTO_VIRGULA);
        return no;
    }

    // cmd_escrita → ESCRITA '(' exp ')' ';'
    private No parseEscrita() throws IOException {
        int linha = atual.getLinha();
        consumir(TipoToken.ESCRITA);
        consumir(TipoToken.ABRE_PAR);
        No exp = parseExpressao();
        consumir(TipoToken.FECHA_PAR);
        consumir(TipoToken.PONTO_VIRGULA);

        No no = new No(TipoNo.ESCRITA, linha);
        no.addFilho(exp);
        return no;
    }

    // ---------------------------------------------------------------
    // Expressões (hierarquia de precedência)
    // ---------------------------------------------------------------

    private No parseExpressao() throws IOException {
        return parseExpRel();
    }

    // expRel → expAdit (RELOP expAdit)?
    private No parseExpRel() throws IOException {
        No esq = parseExpAdit();
        if (isRelop(atual.getTipo())) {
            String op = atual.getLexema(); int linha = atual.getLinha();
            avancar();
            No dir = parseExpAdit();
            No no = new No(TipoNo.OP_BINARIO, op, linha);
            no.addFilho(esq); no.addFilho(dir);
            return no;
        }
        return esq;
    }

    private boolean isRelop(TipoToken t) {
        return t == TipoToken.IGUAL || t == TipoToken.DIFERENTE ||
               t == TipoToken.MENOR || t == TipoToken.MAIOR     ||
               t == TipoToken.MENOR_IGUAL || t == TipoToken.MAIOR_IGUAL;
    }

    // expAdit → expMult (('+' | '-' | OU) expMult)*
    private No parseExpAdit() throws IOException {
        No esq = parseExpMult();
        while (verifica(TipoToken.MAIS) || verifica(TipoToken.MENOS) || verifica(TipoToken.OU)) {
            String op = atual.getLexema(); int linha = atual.getLinha();
            avancar();
            No dir = parseExpMult();
            No no = new No(TipoNo.OP_BINARIO, op, linha);
            no.addFilho(esq); no.addFilho(dir);
            esq = no;
        }
        return esq;
    }

    // expMult → expUnaria (('*'|'/'|DIV|MOD|&&) expUnaria)*
    private No parseExpMult() throws IOException {
        No esq = parseExpUnaria();
        while (isMulop(atual.getTipo())) {
            String op = atual.getLexema(); int linha = atual.getLinha();
            avancar();
            No dir = parseExpUnaria();
            No no = new No(TipoNo.OP_BINARIO, op, linha);
            no.addFilho(esq); no.addFilho(dir);
            esq = no;
        }
        return esq;
    }

    private boolean isMulop(TipoToken t) {
        return t == TipoToken.MULT    || t == TipoToken.DIV_OP ||
               t == TipoToken.DIV     || t == TipoToken.MOD    ||
               t == TipoToken.E_LOGICO;
    }

    // expUnaria → NAO expUnaria | ('+' | '-') expUnaria | expPrimaria
    private No parseExpUnaria() throws IOException {
        if (verifica(TipoToken.NAO)) {
            int linha = atual.getLinha();
            avancar();
            No operando = parseExpUnaria();
            No no = new No(TipoNo.OP_UNARIO, "nao", linha);
            no.addFilho(operando);
            return no;
        }
        if (verifica(TipoToken.MAIS) || verifica(TipoToken.MENOS)) {
            String op = atual.getLexema(); int linha = atual.getLinha();
            avancar();
            No operando = parseExpUnaria();
            No no = new No(TipoNo.OP_UNARIO, op, linha);
            no.addFilho(operando);
            return no;
        }
        return parseExpPrimaria();
    }

    // expPrimaria → ID | CONST_INT | CONST_REAL | CONST_STR | VERDADEIRO | FALSO | '(' exp ')'
    private No parseExpPrimaria() throws IOException {
        int linha = atual.getLinha();
        switch (atual.getTipo()) {
            case ID: {
                String nome = atual.getLexema();
                avancar();
                return new No(TipoNo.EXP_ID, nome, linha);
            }
            case CONST_INT: {
                String v = atual.getLexema(); avancar();
                return new No(TipoNo.EXP_CONST_INT, v, linha);
            }
            case CONST_REAL: {
                String v = atual.getLexema(); avancar();
                return new No(TipoNo.EXP_CONST_REAL, v, linha);
            }
            case CONST_STR: {
                String v = atual.getLexema(); avancar();
                return new No(TipoNo.EXP_CONST_STR, v, linha);
            }
            case VERDADEIRO: {
                avancar();
                return new No(TipoNo.EXP_VERDADEIRO, "verdadeiro", linha);
            }
            case FALSO: {
                avancar();
                return new No(TipoNo.EXP_FALSO, "falso", linha);
            }
            case ABRE_PAR: {
                avancar();
                No exp = parseExpressao();
                consumir(TipoToken.FECHA_PAR);
                return exp;
            }
            default:
                throw new ErroCompilacao(
                    "Expressao invalida: '" + atual.getLexema() + "'", linha);
        }
    }
}
