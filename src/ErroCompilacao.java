public class ErroCompilacao extends RuntimeException {

    private final int linha;

    public ErroCompilacao(String mensagem, int linha) {
        super(mensagem);
        this.linha = linha;
    }

    public int getLinha() { return linha; }

    @Override
    public String toString() {
        return String.format("Erro na linha %d: %s", linha, getMessage());
    }
}
