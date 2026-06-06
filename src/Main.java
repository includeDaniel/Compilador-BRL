/*
 * Compilador BRL
 * Disciplina: Compiladores
 * Instituicao: Dom Helder Escola Superior de Ciencia da Computacao
 * Professor: Prof. Dr. Marcos W. Rodrigues
 *
 * Autores:
 *   - Giovanna Penido
 *   - Joao Victor Lisboa
 *   - Daniel Nunes
 */

import java.io.*;

/**
 * Ponto de entrada do compilador BRL.
 *
 * Uso: BRL <arquivo.lc> <saida.asm>
 *
 * Fases implementadas:
 *   1. Analisador Lexico    — tokenizacao do codigo fonte
 *   2. Analisador Sintatico — verificacao gramatical e construcao da AST
 *   3. Analisador Semantico — verificacao de tipos e declaracoes
 */
public class Main {

    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("Uso: BRL <arquivo.lc> <saida.asm>");
            System.exit(1);
        }

        String arquivoFonte = args[0];
        // args[1] = arquivo .asm (reservado para a fase de geracao de codigo)

        // A tabela e criada antes do lexico para que os IDs sejam
        // registrados assim que forem encontrados no fonte.
        TabelaSimbolos tabela = new TabelaSimbolos();

        // ── Fase 1: Analise Lexica ─────────────────────────────────
        AnalisadorLexico lexer;
        try {
            lexer = new AnalisadorLexico(new FileReader(arquivoFonte), tabela);
        } catch (FileNotFoundException e) {
            System.err.println("Erro: arquivo nao encontrado: " + arquivoFonte);
            System.exit(1);
            return;
        } catch (IOException e) {
            System.err.println("Erro ao abrir o arquivo: " + e.getMessage());
            System.exit(1);
            return;
        }

        // ── Fase 2: Analise Sintatica ──────────────────────────────
        No arvore;
        try {
            AnalisadorSintatico parser = new AnalisadorSintatico(lexer);
            arvore = parser.parsear();
        } catch (ErroCompilacao e) {
            System.err.println(e);
            System.exit(1);
            return;
        } catch (IOException e) {
            System.err.println("Erro de leitura: " + e.getMessage());
            System.exit(1);
            return;
        }

        // ── Fase 3: Analise Semantica ──────────────────────────────
        try {
            AnalisadorSemantico semantico = new AnalisadorSemantico(tabela);
            semantico.analisar(arvore);
        } catch (ErroCompilacao e) {
            System.err.println(e);
            System.exit(1);
            return;
        }

        System.out.println("Analise concluida com sucesso: " + arquivoFonte);
        tabela.imprimir();

        // ── Saída: cria arquivo .asm indicando que a geração de código
        //          será implementada em etapa futura do projeto ──────
        String arquivoSaida = args[1];
        try (PrintWriter asm = new PrintWriter(new FileWriter(arquivoSaida))) {
            asm.println("; Compilador BRL");
            asm.println("; Autores: Giovanna Penido, jOÃO Victor Lisboa, Daniel Nunes");
            asm.println(";");
            asm.println("; Arquivo gerado a partir de: " + arquivoFonte);
            asm.println(";");
            asm.println("; ATENÇÃO: Main.java cria sempre um .asm vazio para cumprir a interface formalmente sem gerar código real.");
        } catch (IOException e) {
            System.err.println("Erro ao criar arquivo de saida: " + e.getMessage());
            System.exit(1);
        }
    }
}
