public class TokenWrapper {

    private final Token token;
    private final String data;

    public TokenWrapper(Token token, String data) {
        this.token = token;
        this.data = data;
    }

    public Token getToken() {
        return token;
    }

    public String getData() {
        return data;
    }
}
