package messageNinjaBot;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.objects.ChatMember;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.MessageEntity;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

public class MessageNinjaBot extends TelegramLongPollingBot{

    private final HashMap<Long, HashMap<Integer, Timer>> chatMap = new HashMap<>();
    private final HashMap<Long, Boolean> chatMuteMap = new HashMap<>();
    private final HashMap<Long, Integer> chatHourMap = new HashMap<>();
    private final Pattern timePattern = Pattern.compile("^halfday_bot\\s+settings:\\s*time\\s*=\\s*[1-100]\\s*$");
    private final Pattern mutePattern = Pattern.compile("^halfday_bot\\s+settings:\\s*mute\\s*=\\s*(true|false)\\s*$");
    
    @Override
    public String getBotToken() {
        return "644525094:AAHkEXHfiKoCB8Zc8rxGHb2gNo3LvctvH1k";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage()){
            Message msg = update.getMessage();   
            if(msg.hasText()){
                long chatId = msg.getChatId(); 
                User user = msg.getFrom();    
                int userId = user.getId();
                String text = msg.getText();
                //если новый чат - добавить в список чатов и инициализировать
                if(!chatMap.containsKey(chatId)){
                	initChatMap(chatId);
                }
                //если новый пользователь - добавить в список пользователей чата и иницализировать таймер
                HashMap<Integer, Timer> userInChatMap = chatMap.get(chatId);
                if(!userInChatMap.containsKey(userId)) {
                	userInChatMap.put(userId, new Timer());
                }
                //проверить сообщение на наличие ссылок
                boolean linksFound  = findLinks(msg);
                //если ссылок не найдено - проверить не внесены ли изменения в настройки
                if(!linksFound) {
                	//получить отправителя
	                GetChatMember member = new GetChatMember();
	                member.setUserId(userId);
	                member.setChatId(chatId);
	                ChatMember chatMember = null;
	                try {
	                    chatMember = getChatMember(member);
	                } catch (TelegramApiException ex) {
	                    ex.printStackTrace(System.out);
	                }
	                //если отправитель не администратор или создатель чата - ничего не предпринимать
	                if(chatMember!= null && (chatMember.getStatus().equals("administrator") || chatMember.getStatus().equals("creator"))){
	                    if(mutePattern.matcher(text).matches()){                 
	                        Boolean mute = chatMuteMap.get(chatId);
	                        if(text.endsWith("false")){
	                            mute = false;
	                        }else if(text.endsWith("true")){
	                            mute = true;
	                        }                  
	                        chatMuteMap.replace(chatId, mute);
	                        SendMessage toSend = new SendMessage(chatId, "Changes was applied.");
	                        try {
	                            execute(toSend);
	                        } catch (TelegramApiException ex) {
	                            ex.printStackTrace(System.out);
	                        }
	                    }
	                    if(timePattern.matcher(text).matches()){
	                        String[] parts = text.split(" ");
	                        Integer time = Integer.parseInt(parts[parts.length - 1]);
	                        chatHourMap.replace(chatId, time);
	                        SendMessage toSend = new SendMessage(chatId, "Changes was applied.");
	                        try {
	                            execute(toSend);
	                        } catch (TelegramApiException ex) {
	                            ex.printStackTrace(System.out);
	                        }
	                    }
	                }     
                }else {
                    HashMap<Integer, Timer> users = chatMap.get(chatId);
                    Boolean mute = chatMuteMap.get(chatId);
                    Integer time = chatHourMap.get(chatId); 
                    Timer timer = users.get(userId);
                    GregorianCalendar currentTime = new GregorianCalendar();
                    GregorianCalendar firstMessageTime = (GregorianCalendar) timer.getTime().clone();
                    firstMessageTime.add(Calendar.SECOND, time);
                    if(currentTime.before(firstMessageTime)){
                        DeleteMessage toDelete = new DeleteMessage(chatId, msg.getMessageId());
                        SendMessage toSend = new SendMessage(chatId, "Removed message from <b>" + user.getUserName() + "</b>. Reason: <i>new user</i> + <i>external link</i>.");
                        toSend.enableHtml(true);
                        try {
                            execute(toDelete);
                            if(mute == false){
                                execute(toSend);
                            }
                        } catch (TelegramApiException ex) {
                            ex.printStackTrace(System.out);
                        }
                    }
                }
                
            }
        }
    }

    @Override
    public String getBotUsername() {
        return "MessageNinjaBot";
    }
    
    private void initChatMap(long chatId) {
        HashMap<Integer, Timer> userInChatMap = new HashMap<>();
        chatMap.put(chatId, userInChatMap);
        chatMuteMap.put(chatId, Boolean.FALSE);
        chatHourMap.put(chatId, 45);
    }
    
    private boolean findLinks(Message msg) {
        if(msg.hasEntities()){
            List<MessageEntity> entities = msg.getEntities();
            for(MessageEntity entity : entities){
                String type = entity.getType();
                if(type.equals("url") || type.equals("text_link") || type.equals("mention")){
                	return true;
                }
            }    
        }
        return false;
    }
     
}
