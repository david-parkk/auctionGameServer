package matching;

import java.util.Queue;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MatchingQueue {

    private Queue<MatchingUser> matchingUserQueue= new ConcurrentLinkedQueue<>();

    public MatchingQueue() {
    }

    public void add(MatchingUser matchingUser){
        matchingUserQueue.add(matchingUser);
    }

    public int getMatchingSize(){
        return matchingUserQueue.size();
    }

    @Override
    public String toString(){
        StringJoiner stringJoiner = new StringJoiner(",");
        matchingUserQueue.forEach(matchingUser -> stringJoiner.add(matchingUser.getName()));
        return stringJoiner.toString();
    }


}
