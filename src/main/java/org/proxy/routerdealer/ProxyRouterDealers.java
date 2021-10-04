package org.proxy.routerdealers;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
public class ProxyRouterDealers {

    public static List<String>  dealerURIs = new ArrayList<>();
    
    public static void main(String[] args) throws InterruptedException, IOException {
    	File file = new File("src/main/resources/config.properties");
    	FileReader reader = new FileReader(file);
    	Properties props = new Properties();
        props.load(reader);
        String routerUri = "tcp://" + props.getProperty("router");
        String[] dealers = props.getProperty("dealers").split(",");
        for (int i = 0; i < dealers.length; i++) {
        	dealerURIs.add("tcp://" + dealers[i]);
		}
    	ProxyRouter proxyRouter = new ProxyRouter("ProxyRouter", routerUri, dealerURIs);
        Thread proxyRouterThread = new Thread(proxyRouter);         
    	proxyRouterThread.start();
    }
}

class ProxyRouter implements Runnable {
    String name;
    String uri;
    private ZMQ.Context context;
    private ZMQ.Socket socket;
    Map<String, ProxyDealer> dealersMap = new HashMap<>();
    List<String> dealerURIs;
    int dealerIndex = 0;
    ProxyRouter(String name, String uri, List<String> dealerURIs) {
        this.name = name;
        this.uri = uri;
        this.dealerURIs = dealerURIs;
        context = ZMQ.context(1);
        socket = context.socket(SocketType.ROUTER);
        socket.setReceiveTimeOut(-1);
        socket.setIdentity(name.getBytes());
        socket.bind(uri);
        System.out.println("Router Started listening on Server=> " + uri);
    }

    void start() {
        while (!Thread.currentThread().isInterrupted()) {
            String dealerId = socket.recvStr();
            System.out.println("Received request from: " + dealerId);
            if (Objects.isNull(dealerId) || dealerId.isEmpty()) {
                System.out.println("Received nothing from the dealers...");
                continue;
            }
            if(null == dealersMap.get(dealerId)) {
            	createAndStartNewDealer(dealerId);
            }
            byte[] dealerData = socket.recv();       
            dealerData = socket.recv();
            String toDealerData = new String(dealerData, ZMQ.CHARSET);
            System.out.println("Received Dealer Data " + toDealerData);
            ProxyDealer proxyDealer = dealersMap.get(dealerId);
            proxyDealer.sendDataToFtligViaProxyDealer(dealerData);
        }
        System.out.print("Router: " + name + " thread Intruppted.");
    }
    private void createAndStartNewDealer(String dealerId) {
    	ProxyDealer dealer = new ProxyDealer(dealerId, dealerURIs.get(dealerIndex++), this);
    	new Thread(dealer).start();
    	System.out.println("Started new dealer "+ dealerId);
    	dealersMap.put(dealerId, dealer);
	}
    
    public void sendRouterResponse(String toDealer, byte[] routerData) {    	
    	socket.sendMore(toDealer);
    	socket.sendMore("");
    	socket.send(routerData);
    }

	public void run() {
        start();
    }
}

class ProxyDealer implements Runnable {
    String name;
    String uri;
    private final Context context;
    private final ZMQ.Socket socket;
    private ProxyRouter proxyRouter;
    ProxyDealer(String name, String uri, ProxyRouter proxyRouter) {
        this.name = name;
        this.uri = uri;
        this.proxyRouter = proxyRouter;
        context = ZMQ.context(1);
        socket = context.socket(SocketType.DEALER);
        socket.setIdentity(name.getBytes());
        socket.setReceiveTimeOut(-1);
        boolean isConnected = socket.connect(uri);        
        System.out.println("Dealer " + name + " isConnected? " + isConnected);
    }

    public void sendDataToFtligViaProxyDealer(byte[] input) {
        String in = new String(input, ZMQ.CHARSET);
        System.out.println("Input at Dealer "+ name + " is "+ in);
        socket.sendMore("");
        boolean isQueued = socket.send(input);
        System.out.println("FTLIG MessagMoe isQueued? "+isQueued);
        }

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			byte[] routerData = socket.recv();
			routerData = socket.recv();
            if (Objects.isNull(routerData)) {
                System.out.println("Nothing received from Router for Dealer "+ name);
                continue;
            }
            String routerDataToString = new String(routerData, ZMQ.CHARSET);
            System.out.println(name + " received router data:-" + routerDataToString);
            proxyRouter.sendRouterResponse(name, routerData);
		}
		
	}        
}
