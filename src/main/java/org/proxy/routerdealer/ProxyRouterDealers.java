package org.proxy.routerdealer;
import java.util.Objects;
import org.zeromq.SocketType;

 import org.zeromq.ZContext;

 import org.zeromq.ZMQ;

 import org.zeromq.ZMQ.Context;

public class ProxyRouterDealers {



public final static String URI = "tcp://127.0.0.1:6001";



public static void main(String[] args) throws InterruptedException {



new Thread(new ProxyRouter("ProxyRouter", URI)).start();

 Thread.sleep(1000);

 new Thread(new FtegCTDealer("FTEG-CT Dealer-1", URI)).start();

 new Thread(new FtegCTDealer("FTEG-CT Dealer-2", URI)).start();

 }



}



class ProxyRouter implements Runnable {

 String name;

 String uri;

 private ZMQ.Context context;

 private ZMQ.Socket socket;



ProxyRouter(String name, String uri) {

 this.name = name;

 this.uri = uri;

 context = ZMQ.context(1);

 socket = context.socket(SocketType.ROUTER);

 socket.setReceiveTimeOut(-1);

 socket.setIdentity(name.getBytes());

 socket.bind(uri);

 System.out.println("Router Started listening on Server=> "+ uri);

 }



void start() {

 //System.out.println("In Start method");

 while (!Thread.currentThread().isInterrupted()) {

 //System.out.println("In While");

 String dealerId = socket.recvStr();

 System.out.println("Received request from: "+ dealerId);

 if (Objects.isNull(dealerId)) {

 System.out.println("Received nothing from the dealers...");

 continue;

 }

 String dealerData;

 if(socket.hasReceiveMore()) {

 dealerData = socket.recvStr();

 System.out.println("Received Dealer Data "+ dealerData);

 }

 socket.sendMore(dealerId);

 socket.send("Message from Router");

 }

 System.out.print("Router: "+ name+ " thread Intruppted.");

 }



public void run() {

 start();

 }

 }



class FtegCTDealer implements Runnable {

 String name;

 String uri;

 private final Context context;

 private final ZMQ.Socket socket;



FtegCTDealer(String name, String uri) {

 this.name = name;

 this.uri = uri;

 context = ZMQ.context(1);

 socket = context.socket(SocketType.DEALER);

 socket.setIdentity(name.getBytes());

 boolean isConnected = socket.connect(uri);

 System.out.println("Dealer "+ name+ " isConnected? "+ isConnected);

 }



void start() {

 String request = "Request:";

 int i = 1;

 while (!Thread.currentThread().isInterrupted()) {

 //System.out.println("Inside Dealer: "+ name);

 socket.sendMore(request + i);

 socket.send("Working our ass's off..");

 String routerData = socket.recvStr();;

 if (Objects.isNull(routerData)) {

 System.out.println("Nothing received from Router");

 continue;

 }

 System.out.println(name +" received router data:-" + routerData);

 i++;

 }

 System.out.print("Dealer: "+ name+ " thread Intruppted.");


 }



public void run() {

 start();



}

 }