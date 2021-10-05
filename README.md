# Proxy Router Dealer Application
 This is a basic application proxy that can sit between multiple dealers and routers.
 
 
## Prerequisite
- JAVA >= 8
- Maven > 3.3
 
 ## Compile and run the code
 
 Just use below command to compile 

you can modify the src/main/java/config.properties to append multiple dealers IP/Port and also to change the router IP and Port.
 
 `mvn clean install`
 
After compilation is successful you can just run below command to start the application-:

`java -jar target\router-dealer-proxy-0.0.1-SNAPSHOT-jar-with-dependencies.jar`


