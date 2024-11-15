import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import matching.MatchingFactory;
import matching.MatchingQueue;
import matching.MatchingUser;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;
    int count=5;

    //경매 참여 여부 플래그
    private boolean participating = false;

    //소지금
    private int balance = 100;

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> messageTask;
    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public String getClientName() {
        return clientName;
    }

    public boolean isParticipating() {
        return participating;
    }

    public void addFunds(int amount) {
        balance += amount;
        sendMessage("잔액 추가됨: " + amount + "원. 현재 잔액: " + balance + "원");
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        MatchingFactory matchingFactory = MatchingFactory.INSTANCE;
        MatchingQueue matchingQueue=matchingFactory.getMatchingQueue();


        try {
            clientName = in.readLine();
            MatchingUser matchingUser = new MatchingUser(clientName);
            matchingQueue.add(matchingUser);
            System.out.println("클라이언트 \"" + clientName+ "\" 가 연결되었습니다: " + socket);

            AuctionServer.broadcastMessage("Matching;"+matchingQueue.toString());
            if(matchingQueue.getMatchingSize()==4){
                informMatching(out);
            }
            while (true) {
                String command = in.readLine();
                if (command == null) break;

                if (command.startsWith("참가")) {
                    participating = true;
                    AuctionServer.broadcastMessage("matching;"+clientName + " 님이 경매에 참가했습니다.");
                } else if (command.startsWith("호가")) {
                    int bidAmount = Integer.parseInt(command.split(" ")[1]);
                    if (balance >= bidAmount) {
                        balance -= bidAmount;
                        AuctionServer.placeBid(this, bidAmount);
                    } else {
                        sendMessage("잔액 부족으로 호가 실패.");
                    }
                } else if (command.startsWith("불참여")) {
                    participating = false;
                    sendMessage("경매에 불참했습니다.");
                } else if (command.startsWith("채팅")) {
                    String chatMessage = command.substring(3); // "채팅 " 부분을 제거
                    AuctionServer.broadcastMessage("채팅 " + clientName + ": " + chatMessage);
                }
            }
        } catch (IOException e) {
            System.out.println("연결 종료: " + clientName);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("소켓 종료 오류");
            }
        }
    }

    private void informMatching(PrintWriter out) {

        // 3초 후 1초 간격으로 메시지를 보내는 작업을 시작

        messageTask = scheduler.scheduleAtFixedRate(() -> {
            if (count >= 0) {
                AuctionServer.broadcastMessage("MatchingFinished;" + count);
                count--;
            } else {
                messageTask.cancel(false); // 반복 작업을 취소
                System.out.println("Stopping message task.");
            }
        }, 3, 1, TimeUnit.SECONDS);

    }
}
