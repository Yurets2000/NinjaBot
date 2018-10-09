package messageNinjaBot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

public class Main{

    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi tba = new TelegramBotsApi();
        try {
            tba.registerBot(new MessageNinjaBot());
            System.out.println("bot started");
        } catch (TelegramApiRequestException ex) {
            ex.printStackTrace(System.out);
        }
    }
    
}
