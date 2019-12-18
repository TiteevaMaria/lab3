package lab3;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Random;

@org.springframework.stereotype.Controller
public class Controller
{
    private WebSocketSession playerX;
    private WebSocketSession playerO;
    private WebSocketSession currentPlayer;

    private char[][] field = new char[][]
            {
                {'1', '1', '1'},
                {'1', '1', '1'},
                {'1', '1', '1'}
            };

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    WebSocketConfigurer webSocketConfigurer()
    {
        return new WebSocketConfigurer()
        {
            @Override
            public void registerWebSocketHandlers(WebSocketHandlerRegistry registry)
            {
                registry.addHandler(new TextWebSocketHandler()
                {
                    @Override
                    public void afterConnectionEstablished(WebSocketSession session) throws Exception
                    {
                        if (playerX == null && playerO == null)
                        {
                            Random random = new Random();
                            if (random.nextInt(2) == 1)
                            {
                                playerX = session;
                                currentPlayer = session;
                                playerX.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("XO", "X"))));
                                System.out.println("X " + playerX + " connected");
                            }
                            else
                                {
                                    playerO = session;
                                    playerO.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("XO", "O"))));
                                    System.out.println("O " + playerO + " connected");
                                }

                        }
                        else
                            {
                                if (playerO == null)
                                {
                                    playerO = session;
                                    playerO.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("XO", "O"))));
                                    System.out.println("O " + playerO + " connected");
                                }
                                if (playerX == null)
                                {
                                    playerX = session;
                                    currentPlayer = session;
                                    playerX.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("XO", "X"))));
                                    System.out.println("X " + playerX + " connected");
                                }
                            }
                    }

                    @Override
                    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception
                    {
                        if (session == playerX)
                        {
                            System.out.println("X " + playerX + " disconnected");
                            playerX = null;
                        }
                        else
                            {
                                if (session == playerO)
                                {
                                    System.out.println("O " + playerO + " disconnected");
                                    playerO = null;
                                }
                            }

                        if (playerO == null && playerX == null) { restart(); }
                    }

                    @Override
                    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception
                    {
                        String payload = message.getPayload();
                        Mes mes = objectMapper.readValue(payload, Mes.class);

                        if (currentPlayer == session)
                        {
                            char player = mes.type.toCharArray()[0];
                            String[] spl = mes.content.split(",");
                            int x = Integer.parseInt(spl[0]);
                            int y = Integer.parseInt(spl[1]);

                            if (field[x][y] == '1')
                            {
                                if (player == 'X') { currentPlayer = playerO; }
                                else { if (player == 'O') { currentPlayer = playerX; }}

                                field[x][y] = player;

                                playerO.sendMessage(new TextMessage(objectMapper.writeValueAsString(mes)));
                                playerX.sendMessage(new TextMessage(objectMapper.writeValueAsString(mes)));

                                if (player == 'X')
                                {
                                    playerO.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Ход O."))));
                                    playerX.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Ход O."))));
                                }
                                else
                                    {
                                        playerO.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Ход X."))));
                                        playerX.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Ход X."))));
                                    }

                                if (checkAndWriteWinDraw(player))
                                {
                                    playerO.close();
                                    playerX.close();
                                    restart();
                                }

                            }
                            else
                                {
                                    if (field[x][y] == 'X' || field[x][y] == 'O')
                                    {
                                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Место занято."))));
                                    }
                                }
                        }
                        else
                            {
                                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Не ваш ход."))));
                            }
                    }
                }, "/ws");
            }
        };
    }

    private void restart()
    {
        currentPlayer = null;
        field = new char[][]
                {
                    {'1', '1', '1'},
                    {'1', '1', '1'},
                    {'1', '1', '1'}
                };
    }

    public boolean checkAndWriteWinDraw(char player) throws IOException {
        int drawCounter = 0;
        int rowWinCounter = 0;
        int columnWinCounter = 0;
        int diagonalWinCounter = 0;

        for (int i = 0; i < 3; i++)
        {
            if (field[i][0] == player && field[i][1] == player && field[i][2] == player) { rowWinCounter++; }
            if (field[0][i] == player && field[1][i] == player && field[2][i] == player) { columnWinCounter++; }
        }

        if (field[0][2] == player && field[1][1] == player && field[2][0] == player) { diagonalWinCounter++; }
        if (field[0][0] == player && field[1][1] == player && field[2][2] == player) { diagonalWinCounter++; }

        if (rowWinCounter + columnWinCounter + diagonalWinCounter > 0)
        {
            if (player == 'X')
            {
                playerO.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Вы проиграли."))));
                playerX.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Вы выиграли."))));
                return true;
            }
            if (player == 'O')
            {
                playerO.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Вы выиграли."))));
                playerX.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Вы проиграли."))));
                return true;
            }
        }

        for (int x = 0; x < 3; x++)
        {
            for (int y = 0; y < 3; y++)
            {
                if (field[x][y] == '1') { drawCounter++; }
            }
        }
        if (drawCounter == 0)
        {
            playerO.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Ничья."))));
            playerX.sendMessage(new TextMessage(objectMapper.writeValueAsString(new Mes("PlayerInf", "Ничья."))));
            System.out.println("Ничья!");
            return true;
        }
        return false;
    }

    @GetMapping("/TicTacToe")
    public String ticTacToe()
    {
        return "TicTacToe";
    }
}

class Mes
{
    String type;
    String content;

    public Mes(String type, String content)
    {
        this.type = type;
        this.content = content;
    }

    @Override
    public String toString()
    {
        return "{type='" + type + '\'' + ", content=" + content + "}";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}