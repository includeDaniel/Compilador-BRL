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
import java.nio.charset.StandardCharsets;

/**
 * Autores: Giovanna Silva Penido, João Victor Lisboa e Daniel Nunes
 *
 * Uso: BRL <arquivo.lc> <saida.asm>
 *
 * Fases implementadas:
 *   1. Analisador Léxico    — tokenização do código fonte
 *   2. Analisador Sintático — verificação gramatical e construção da AST
 *   3. Analisador Semântico — verificação de tipos e declarações
 */
public class BRL {

    public static void main(String[] args) throws UnsupportedEncodingException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        if (args.length < 2) {
            System.err.println("Uso: BRL <arquivo.lc> <saida.asm>");
            System.exit(1);
        }

        String arquivoFonte = args[0];
        String arquivoSaida = args[1];

        TabelaSimbolos tabela = new TabelaSimbolos();

        // ── Fase 1: Análise Léxica ─────────────────────────────────
        AnalisadorLexico lexer;
        try {
            lexer = new AnalisadorLexico(new FileReader(arquivoFonte), tabela);
        } catch (FileNotFoundException e) {
            System.err.println("Erro: arquivo não encontrado: " + arquivoFonte);
            System.exit(1);
            return;
        } catch (IOException e) {
            System.err.println("Erro ao abrir o arquivo: " + e.getMessage());
            System.exit(1);
            return;
        }

        // ── Fase 2: Análise Sintática ──────────────────────────────
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

        // ── Fase 3: Análise Semântica ──────────────────────────────
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

        // ── Saída: arquivo .asm (geração de código reservada para etapa futura) ──
        try (PrintWriter asm = new PrintWriter(new FileWriter(arquivoSaida))) {
            asm.println("; Compilador BRL");
            asm.println("; Autores: Giovanna Penido, Joao Victor Lisboa, Daniel Nunes");
            asm.println("; Arquivo gerado a partir de: " + arquivoFonte);
        } catch (IOException e) {
            System.err.println("Erro ao criar arquivo de saida: " + e.getMessage());
            System.exit(1);
        }
    }
}
