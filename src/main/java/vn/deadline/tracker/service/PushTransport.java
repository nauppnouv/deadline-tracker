package vn.deadline.tracker.service;

public interface PushTransport {
    record Message(String endpoint,String publicKey,String auth,String notificationId){}
    record Result(int status,String retryAfter){}
    Result send(Message message)throws Exception;
}
