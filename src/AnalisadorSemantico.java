/**
 * Analisador Semântico para a linguagem BRL.
 *
 * Responsabilidades:
 *   - Preencher a tabela de símbolos a partir das declarações.
 *   - Verificar se toda variável usada foi declarada.
 *   - Inferir e verificar tipos das expressões.
 *   - Verificar compatibilidade de tipos nas atribuições.
 *   - Verificar que condições de SE e ENQUANTO sejam do tipo lógico.
 */
public class AnalisadorSemantico {

    private final TabelaSimbolos tabela;

    public AnalisadorSemantico(TabelaSimbolos tabela) {
        this.tabela = tabela;
    }

    // ---------------------------------------------------------------
    // Entrada
    // ---------------------------------------------------------------

    public void analisar(No raiz) {
        analisarPrograma(raiz);
    }

    // ---------------------------------------------------------------
    // Programa
    // ---------------------------------------------------------------

    private void analisarPrograma(No no) {
        // Marca o nome do programa na tabela para não aparecer como "nao declarado"
        tabela.definirTipo(no.getValor(), "programa", tabela.getToken(no.getValor()));

        No blocoDecl  = no.getFilho(0);
        No blocoInstr = no.getFilho(1);
        processarDeclaracoes(blocoDecl);
        analisarBlocoInstr(blocoInstr);
    }

    // ---------------------------------------------------------------
    // Declarações — povoa a tabela de símbolos
    // ---------------------------------------------------------------

    private void processarDeclaracoes(No blocoDecl) {
        for (No decl : blocoDecl.getFilhos()) {
            // Último filho do nó DECLARACAO é o nó de tipo
            No noTipo = decl.getFilho(decl.numFilhos() - 1);
            String tipo = noTipo.getValor();

            // Os demais filhos são os IDs declarados
            for (int i = 0; i < decl.numFilhos() - 1; i++) {
                No noId = decl.getFilho(i);
                String nome = noId.getValor();

                // Se já tem tipo definido, foi declarada antes → erro de redeclaração
                if (tabela.getTipo(nome) != null) {
                    throw new ErroCompilacao(
                        "Variavel '" + nome + "' ja foi declarada", noId.getLinha());
                }
                tabela.definirTipo(nome, tipo, tabela.getToken(nome));
            }
        }
    }

    // ---------------------------------------------------------------
    // Instruções
    // ---------------------------------------------------------------

    private void analisarBlocoInstr(No bloco) {
        for (No instr : bloco.getFilhos()) {
            analisarInstrucao(instr);
        }
    }

    private void analisarInstrucao(No no) {
        switch (no.getTipo()) {
            case ATRIB:    analisarAtrib(no);    break;
            case SE:       analisarSe(no);       break;
            case ENQUANTO: analisarEnquanto(no); break;
            case LEITURA:  analisarLeitura(no);  break;
            case ESCRITA:  analisarEscrita(no);  break;
            default:
                throw new ErroCompilacao("No de instrucao inesperado", no.getLinha());
        }
    }

    // atrib → ID ':=' expressao
    private void analisarAtrib(No no) {
        No noId  = no.getFilho(0);
        No noExp = no.getFilho(1);

        String nomeVar = noId.getValor();
        verificarDeclarada(nomeVar, noId.getLinha());

        String tipoVar = tabela.getTipo(nomeVar);
        String tipoExp = inferirTipo(noExp);

        if (!tiposCompativeis(tipoVar, tipoExp)) {
            throw new ErroCompilacao(
                "Tipo incompativel: variavel '" + nomeVar + "' e " + tipoVar +
                " mas expressao e " + tipoExp, no.getLinha());
        }
        noExp.setTipoDado(tipoExp);
    }

    // se → cond then [senao]
    private void analisarSe(No no) {
        No cond = no.getFilho(0);
        String tipoCond = inferirTipo(cond);
        if (!tipoCond.equals("logico")) {
            throw new ErroCompilacao(
                "Condicao do SE deve ser logica, mas e " + tipoCond, cond.getLinha());
        }
        analisarBlocoInstr(no.getFilho(1));
        if (no.numFilhos() == 3) analisarBlocoInstr(no.getFilho(2));
    }

    // enquanto → cond corpo
    private void analisarEnquanto(No no) {
        No cond = no.getFilho(0);
        String tipoCond = inferirTipo(cond);
        if (!tipoCond.equals("logico")) {
            throw new ErroCompilacao(
                "Condicao do ENQUANTO deve ser logica, mas e " + tipoCond, cond.getLinha());
        }
        analisarBlocoInstr(no.getFilho(1));
    }

    // leitura → IDs
    private void analisarLeitura(No no) {
        for (No noId : no.getFilhos()) {
            verificarDeclarada(noId.getValor(), noId.getLinha());
        }
    }

    // escrita → exp
    private void analisarEscrita(No no) {
        inferirTipo(no.getFilho(0));
    }

    // ---------------------------------------------------------------
    // Inferência de tipos para expressões
    // ---------------------------------------------------------------

    private String inferirTipo(No no) {
        String tipo;
        switch (no.getTipo()) {
            case EXP_ID:
                verificarDeclarada(no.getValor(), no.getLinha());
                tipo = tabela.getTipo(no.getValor());
                break;

            case EXP_CONST_INT:
                tipo = "inteiro";
                break;

            case EXP_CONST_REAL:
                tipo = "real";
                break;

            case EXP_CONST_STR:
                tipo = "caractere";
                break;

            case EXP_VERDADEIRO:
            case EXP_FALSO:
                tipo = "logico";
                break;

            case OP_UNARIO:
                tipo = inferirTipoUnario(no);
                break;

            case OP_BINARIO:
                tipo = inferirTipoBinario(no);
                break;

            default:
                throw new ErroCompilacao("No de expressao inesperado", no.getLinha());
        }
        no.setTipoDado(tipo);
        return tipo;
    }

    private String inferirTipoUnario(No no) {
        String op   = no.getValor();
        String tipoOperando = inferirTipo(no.getFilho(0));

        if (op.equals("nao")) {
            if (!tipoOperando.equals("logico")) {
                throw new ErroCompilacao(
                    "Operador 'nao' requer operando logico, mas e " + tipoOperando, no.getLinha());
            }
            return "logico";
        }
        if (op.equals("+") || op.equals("-")) {
            if (!isNumerico(tipoOperando)) {
                throw new ErroCompilacao(
                    "Operador unario '" + op + "' requer operando numerico, mas e " + tipoOperando,
                    no.getLinha());
            }
            return tipoOperando;
        }
        throw new ErroCompilacao("Operador unario desconhecido: " + op, no.getLinha());
    }

    private String inferirTipoBinario(No no) {
        String op   = no.getValor();
        String esq  = inferirTipo(no.getFilho(0));
        String dir  = inferirTipo(no.getFilho(1));

        // Operadores relacionais → resultado lógico
        if (op.equals("==") || op.equals("<>") || op.equals("<") ||
            op.equals(">")  || op.equals("<=") || op.equals(">=")) {
            if (!tiposCompativeis(esq, dir)) {
                throw new ErroCompilacao(
                    "Operador '" + op + "' com operandos incompativeis: " + esq + " e " + dir,
                    no.getLinha());
            }
            return "logico";
        }

        // Operadores lógicos
        if (op.equals("ou") || op.equals("&&")) {
            if (!esq.equals("logico") || !dir.equals("logico")) {
                throw new ErroCompilacao(
                    "Operador '" + op + "' requer operandos logicos", no.getLinha());
            }
            return "logico";
        }

        // Operadores aritméticos
        if (op.equals("+") || op.equals("-") || op.equals("*") ||
            op.equals("/")  || op.equals("div") || op.equals("mod")) {

            // Concatenação de strings com '+'
            if (op.equals("+") && esq.equals("caractere") && dir.equals("caractere")) {
                return "caractere";
            }

            if (!isNumerico(esq) || !isNumerico(dir)) {
                throw new ErroCompilacao(
                    "Operador '" + op + "' requer operandos numericos, mas sao: " + esq + " e " + dir,
                    no.getLinha());
            }

            // div e mod só para inteiros
            if ((op.equals("div") || op.equals("mod")) &&
                (!esq.equals("inteiro") || !dir.equals("inteiro"))) {
                throw new ErroCompilacao(
                    "Operador '" + op + "' requer operandos inteiros", no.getLinha());
            }

            // Se qualquer operando for real → resultado real
            if (esq.equals("real") || dir.equals("real")) return "real";
            return "inteiro";
        }

        throw new ErroCompilacao("Operador desconhecido: " + op, no.getLinha());
    }

    // ---------------------------------------------------------------
    // Utilitários
    // ---------------------------------------------------------------

    private void verificarDeclarada(String nome, int linha) {
        if (tabela.getTipo(nome) == null) {
            throw new ErroCompilacao("Variavel '" + nome + "' nao declarada", linha);
        }
    }

    private boolean isNumerico(String tipo) {
        return tipo.equals("inteiro") || tipo.equals("real");
    }

    private boolean tiposCompativeis(String a, String b) {
        if (a.equals(b)) return true;
        // Promoção implícita inteiro → real
        if ((a.equals("inteiro") && b.equals("real")) ||
            (a.equals("real")    && b.equals("inteiro"))) return true;
        return false;
    }
}
