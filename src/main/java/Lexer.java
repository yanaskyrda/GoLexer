import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Lexer {
    private final List<TokenWrapper> tokens = new ArrayList<>();

    private State currentState = State.START;

    private StringBuilder stateBuffer = new StringBuilder();


    public List<TokenWrapper> startLexer(String fileName) throws IOException {
        File f = new File(fileName);
        InputStream inputStream = new FileInputStream(f);
        Reader reader = new InputStreamReader(inputStream, Charset.defaultCharset());
        Reader bufferedReader = new BufferedReader(reader);

        int symbol;
        while ((symbol = bufferedReader.read()) != -1) {
            Character c = (char) symbol;
            processSymbol(c);
        }

        processSymbol('\n');
        if (currentState != State.START) {
            createToken(Token.ERROR);
        }

        return tokens;
    }

    private void processSymbol(Character c) {
        stateBuffer.append(c);

        switch (currentState) {
            case ERROR -> errorState(c);
            case START -> startState(c);
            case TEXT_PROCESSING -> textProcessingState(c);
            case QUO -> quoState(c);
            case DIGIT -> digitState(c);
            case CHAR -> charState(c);
            case STRING -> stringState(c);
            case PERIOD -> periodState(c);
            case GTR -> greaterState(c);
            case LSS -> lessState(c);
            case AND -> andState(c);
            case OR -> orState(c);
            case OPERATOR -> operatorState(c);
            case COLON -> colonState(c);
            case ADD -> addState(c);
            case SUB -> subState(c);
            case SINGLE_LINE_COMMENT -> singleLineCommentState(c);
            case BLOCK_COMMENT -> blockCommentState(c);
            case PERIOD_IN_DIGIT -> periodInDigitState(c);
            case FLOAT -> floatState(c);
            case COMPLEX_NUMBER -> complexState(c);
            case INVALID_NUMBER -> invalidFloatState(c);
            case CHAR_PROCESSING -> charProcessingState(c);
            case SH -> shState(c);
            case OPERATOR_AND_EQUAL -> operatorAndEqualState(c);
            case CLOSING_BLOCK_COMMENT -> closeBlockCommentState(c);
            default -> log.error("Couldn't recognize state {}.", currentState);
        }
    }

    private void addCompleteToken(Token token) {
        String substringWithoutLastChar = stateBuffer.substring(0, stateBuffer.length() - 1);
        Character lastChar = stateBuffer.charAt(stateBuffer.length() - 1);

        tokens.add(new TokenWrapper(token, substringWithoutLastChar));
        clearBuffer(lastChar);
    }

    private void clearBuffer(Character firstNewChar) {
        stateBuffer = new StringBuilder();
        if (firstNewChar != null) {
            stateBuffer.append(firstNewChar);
        }
    }

    private void createToken(Token token) {
        if (token == Token.WHITESPACE) {
            tokens.add(new TokenWrapper(token, " "));
        } else {
            tokens.add(new TokenWrapper(token, stateBuffer.toString()));
        }
        clearBuffer(null);
    }

    private void startState(Character c) {
        if (c == '/') {
            currentState = State.QUO;
        } else if (Character.isWhitespace(c) || c == '\n') {
            createToken(Token.WHITESPACE);
            currentState = State.START;
        } else if (Character.isDigit(c)) {
            currentState = State.DIGIT;
        } else if (c == '\'') {
            currentState = State.CHAR;
        } else if (c == '\"') {
            currentState = State.STRING;
        } else if (c == '.') {
            currentState = State.PERIOD;
        } else if (Utils.isSeparator(c)) {
            createToken(Token.SEPARATOR);
            currentState = State.START;
        } else if (c == '>') {
            currentState = State.GTR;
        } else if (c == '<') {
            currentState = State.LSS;
        } else if (c == '&') {
            currentState = State.AND;
        } else if (c == '^' || c == '!' || c == '*' || c == '=' || c == '%') {
            currentState = State.OPERATOR;
        } else if (c == ':') {
            currentState = State.COLON;
        } else if (c == '+') {
            currentState = State.ADD;
        } else if (c == '-') {
            currentState = State.SUB;
        } else if (c == '|') {
            currentState = State.OR;
        } else if (Character.isAlphabetic(c) || c == '_') {
            currentState = State.TEXT_PROCESSING;
        } else {
           log.warn("Can't find state for symbols {}", stateBuffer.toString());
           stateBuffer = new StringBuilder();
       }
    }

    private void errorState(Character c) {
        addCompleteToken(Token.ERROR);
        currentState = State.START;
        startState(c);
    }

    private void textProcessingState(Character c) {
        if (c == ' ' || Character.isWhitespace(c) || c == '\n'
                || Utils.isSeparator(c) || Utils.isOperator(c)) {
            if (Utils.isKeyword(stateBuffer.substring(0, stateBuffer.length() - 1))) {
                addCompleteToken(Token.KEYWORD);
            } else if (Utils.isBoolean(stateBuffer.substring(0, stateBuffer.length() - 1))) {
                addCompleteToken(Token.BOOLEAN);
            } else if (Utils.isNull(stateBuffer.substring(0, stateBuffer.length() - 1))) {
                addCompleteToken(Token.NULL);
            } else {
                addCompleteToken(Token.NAME);
            }
            currentState = State.START;
            startState(c);
        } else if (!(Character.isLetter(c) || Character.isDigit(c) || c == '_')) {
            currentState = State.ERROR;
        }
    }

    private void closeBlockCommentState(Character c) {
        if (c == '/') {
            createToken(Token.COMMENT);
            currentState = State.START;
        } else {
            currentState = State.BLOCK_COMMENT;
        }
    }

    private void operatorAndEqualState(Character c) {
        if (c == '-' || c == '+') {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        } else if (Utils.isOperator(c)) {
            currentState = State.ERROR;
        } else {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        }
    }

    private void shState(Character c) {
        if (c == '=') {
            createToken(Token.OPERATOR);
            currentState = State.START;
        } else if (c == '-' || c == '+') {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        } else if (Utils.isOperator(c)) {
            currentState = State.ERROR;
        } else {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        }
    }

    private void charProcessingState(Character c) {
        if (c == '\'') {
            createToken(Token.CHAR);
            currentState = State.START;
        } else {
            currentState = State.ERROR;
        }
    }

    private void periodInDigitState(Character c) {
        if (Character.isDigit(c)) {
            currentState = State.FLOAT;
        } else {
            currentState = State.ERROR;
        }
    }

    private void blockCommentState(Character c) {
        if (c == '*') {
            currentState = State.CLOSING_BLOCK_COMMENT;
        }
    }

    private void singleLineCommentState(Character c) {
        if (c == '\n') {
            addCompleteToken(Token.COMMENT);
            currentState = State.START;
            startState(c);
        }
    }

    private void subState(Character c) {
        if (c == '-') {
            currentState = State.OPERATOR;
        } else if (c == '=') {
            currentState = State.OPERATOR_AND_EQUAL;
        } else if (Utils.isOperator(c)) {
            currentState = State.ERROR;
        } else {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        }
    }

    private void addState(Character c) {
        if (c == '+') {
            currentState = State.OPERATOR;
        } else if (c == '=') {
            currentState = State.OPERATOR_AND_EQUAL;
        } else if (Utils.isOperator(c)) {
            currentState = State.ERROR;
        } else {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        }
    }

    private void colonState(Character c) {
        if (c == '=') {
            createToken(Token.OPERATOR);
            currentState = State.START;
        } else if (Utils.isOperator(c)) {
            currentState = State.ERROR;
        } else {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        }
    }

    private void operatorState(Character c) {
        if (c == '=') {
            currentState = State.OPERATOR_AND_EQUAL;
        } else if (c == '-' || c == '+') {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        } else if (Utils.isOperator(c)) {
            currentState = State.ERROR;
        } else {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        }
    }

    private void andState(Character c) {
        if (c == '&') {
            createToken(Token.OPERATOR);
            currentState = State.START;
        } else if (c == '=') {
            currentState = State.OPERATOR_AND_EQUAL;
        } else if (Utils.isOperator(c)) {
            currentState = State.ERROR;
        } else {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        }
    }

    private void orState(Character c) {
        if (c == '|') {
            createToken(Token.OPERATOR);
            currentState = State.START;
        } else if (c == '=') {
            currentState = State.OPERATOR_AND_EQUAL;
        } else if (Utils.isOperator(c)) {
            currentState = State.ERROR;
        } else {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        }
    }

    private void lessState(Character c) {
        if (c == '=' || c == '<' || c == '-') {
            createToken(Token.OPERATOR);
            currentState = State.START;
        } else if (c == '>') {
            currentState = State.SH;
        } else if (Utils.isOperator(c)) {
            currentState = State.ERROR;
        } else {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        }
    }

    private void greaterState(Character c) {
        if (c == '=') {
            createToken(Token.OPERATOR);
            currentState = State.START;
        } else if (c == '>') {
            currentState = State.SH;
        } else if (Utils.isOperator(c)) {
            currentState = State.ERROR;
        } else {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        }
    }

    private void quoState(Character c) {
        if (c == '/') {
            currentState = State.SINGLE_LINE_COMMENT;
        } else if (c == '*') {
            currentState = State.BLOCK_COMMENT;
        } else if (c == '=') {
            currentState = State.OPERATOR_AND_EQUAL;
        } else if (Utils.isOperator(c)) {
            currentState = State.ERROR;
        } else {
            addCompleteToken(Token.OPERATOR);
            currentState = State.START;
            startState(c);
        }
    }

    private void digitState(Character c) {
        if (c == '.') {
            currentState = State.PERIOD_IN_DIGIT;
        } else if (c == 'i') {
            currentState = State.COMPLEX_NUMBER;
        } else if (!Character.isDigit(c)) {
            if (Character.isWhitespace(c) || c == '\n') {
                addCompleteToken(Token.INT);
                currentState = State.START;
                startState(c);
            } else if (!Character.isDigit(c)) {
                currentState = State.INVALID_NUMBER;
            }
        }
    }

    private void floatState(Character c) {
        if (Character.isWhitespace(c) || c == '\n') {
            addCompleteToken(Token.FLOAT);
            currentState = State.START;
            startState(c);
        } else if (c == 'i') {
            currentState = State.COMPLEX_NUMBER;
        } else if (!Character.isDigit(c)) {
            currentState = State.INVALID_NUMBER;
        }
    }

    private void complexState(Character c) {
        if (Character.isWhitespace(c) || c == '\n') {
            addCompleteToken(Token.COMPLEX);
            currentState = State.START;
            startState(c);
        } else {
            currentState = State.INVALID_NUMBER;
        }
    }


    private void invalidFloatState(Character c) {
        if (Character.isWhitespace(c) || c == '\n') {
            addCompleteToken(Token.ERROR);
            currentState = State.START;
            startState(c);
        }
    }

    private void charState(Character c) {
        if (Character.isWhitespace(c) || c == '\n') {
            addCompleteToken(Token.ERROR);
            currentState = State.START;
            startState(c);
        } else {
            currentState = State.CHAR_PROCESSING;
        }
    }

    private void stringState(Character c) {
        if (c == '\"') {
            if (stateBuffer.charAt(stateBuffer.length() - 2) != '\\') {
                createToken(Token.STRING);
                currentState = State.START;
            } else {
                stateBuffer.deleteCharAt(stateBuffer.length() - 2);
            }
        }
    }

    private void periodState(Character c) {
        if (Character.isDigit(c)) {
            currentState = State.PERIOD_IN_DIGIT;
        } else {
            addCompleteToken(Token.SEPARATOR);
            currentState = State.START;
            startState(c);
        }
    }
}
