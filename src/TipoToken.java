public enum TipoToken {

    // Tipos de dados
    TIPO_INTEIRO, TIPO_REAL, TIPO_LOGICO, TIPO_CARACTERE,

    // Comandos
    SE, SENAO, ENTAO, ENQUANTO, FACA, INICIO, FIM,
    LEITURA, ESCRITA,

    // Operadores booleanos / aritméticos (palavras reservadas)
    OU, NAO, DIV, MOD, VERDADEIRO, FALSO,

    // Literais e identificadores
    ID, CONST_INT, CONST_REAL, CONST_STR,

    // Operadores relacionais
    IGUAL,          // ==
    DIFERENTE,      // <>
    MENOR,          // <
    MAIOR,          // >
    MENOR_IGUAL,    // <=
    MAIOR_IGUAL,    // >=

    // Operadores aritméticos / lógicos
    MAIS,           // +
    MENOS,          // -
    MULT,           // *
    DIV_OP,         // /
    E_LOGICO,       // &&

    // Atribuição
    ATRIB,          // :=
    IGUAL_SIMPLES,  // =  (reservado)

    // Delimitadores
    PONTO_VIRGULA,  // ;
    DOIS_PONTOS,    // :
    VIRGULA,        // ,
    ABRE_PAR,       // (
    FECHA_PAR,      // )
    ABRE_COL,       // [
    FECHA_COL,      // ]
    ABRE_CHAVE,     // {
    FECHA_CHAVE,    // }

    // Controle
    EOF,
    INVALIDO
}
