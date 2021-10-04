package org.proxy.routerdealer;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import java.util.*;
public class ProxyRouterDealers {

    public final static String URI = "tcp://127.0.0.1:6001";
    public final static String DEALER_URI_1 = "tcp://127.0.0.1:5002";
    public final static String DEALER_URI_2 = "tcp://127.0.0.1:5003";
    public static ProxyDealer[]  dealers = new ProxyDealer[2];
    
    public static void main(String[] args) throws InterruptedException {
    	ProxyDealer dealer1 = new ProxyDealer("dealer1", DEALER_URI_1);
    	dealers[0] = dealer1;
    	ProxyDealer dealer2 = new ProxyDealer("dealer1", DEALER_URI_1);
    	dealers[1] = dealer2;   	
        new Thread(new ProxyRouter("ProxyRouter", URI, dealers)).start();
        Thread.sleep(1000);
    }
}

class ProxyRouter implements Runnable {
    String name;
    String uri;
    private ZMQ.Context context;
    private ZMQ.Socket socket;
    ProxyDealer[] dealers;
    Map<String, Integer> map = new HashMap<String, Integer>();
    String[] pairs = new String[2];
    ProxyRouter(String name, String uri, ProxyDealer[] dealers) {
        this.name = name;
        this.uri = uri;
        this.dealers = dealers;
        context = ZMQ.context(1);
        socket = context.socket(SocketType.ROUTER);
        socket.setReceiveTimeOut(-1);
        socket.setIdentity(name.getBytes());
        socket.bind(uri);
        System.out.println("Router Started listening on Server=> " + uri);
    }

    void start() {
        //System.out.println("In Start method");
        while (!Thread.currentThread().isInterrupted()) {
            //System.out.println("In While");
            String dealerId = socket.recvStr();
            System.out.println("Received request from: " + dealerId);
            if (Objects.isNull(dealerId)) {
                System.out.println("Received nothing from the dealers...");
                continue;
            }
            byte[] dealerData;
//            if (socket.hasReceiveMore()) {
                dealerData = socket.recv();
                System.out.println("Received Dealer Data " + dealerData);
 //           }
            ProxyDealer proxyDealer = fetchProxyDealer(map, dealers, dealerId);
            dealerData = proxyDealer.sendAndReceive(dealerData);
            socket.sendMore(dealerId);
            socket.send(dealerData);
        }
        System.out.print("Router: " + name + " thread Intruppted.");
    }
    private ProxyDealer fetchProxyDealer(Map<String, Integer> map, ProxyDealer[] dealers2, String dealerId) {
    	if(map.get(dealerId) != null) {
    		return dealers[map.get(dealerId)];
    	}  else if (map.size() == 0) {
    		map.put(dealerId, 0);
    		return dealers[0];
    	} else {
    		map.put(dealerId, 1);
    		return dealers[1];
    	}
	}

	public void run() {
        start();
    }
}

class ProxyDealer {
    String name;
    String uri;
    private final Context context;
    private final ZMQ.Socket socket;

    ProxyDealer(String name, String uri) {
        this.name = name;
        this.uri = uri;
        context = ZMQ.context(1);
        socket = context.socket(SocketType.DEALER);
        socket.setIdentity(name.getBytes());
        boolean isConnected = socket.connect(uri);
        System.out.println("Dealer " + name + " isConnected? " + isConnected);
    }

    public byte[] sendAndReceive(byte[] input) {
            //System.out.println("Inside Dealer: "+ name);
            socket.send(input);
            byte[] routerData = socket.recv();
            if (Objects.isNull(routerData)) {
                System.out.println("Nothing received from Router for Dealer "+ name);
                return null;
            }
//            System.out.println(name + " received router data:-" + routerData);
            return routerData;
        }        
}
