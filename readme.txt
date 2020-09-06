IMPORTANT UPDATE: Unfortunately the api where stock data was retrieved from was changed on 1 June 2019, and since this is only a sample application no effort will be made to get it working with the new api. Therefore submitting market orders will no longer work, but the program can still be run.

A multi-module maven project which consists of three units - the router, and the market & broker which connect to the router via asynchronous sockets. 

The project uses abbreviated and simplified FIX notation to send messages via the router between the market and the broker - simulating financial exchange where multiple brokers can send buy/sell orders to multiple markets via the router.

The supported stocks are artificially limited to Tesla (TSLA), MicroSoft (MSFT), Google (GOOGL) and Amazon (AMZN) to adhere to the project guidelines and create a routing table in the router.

The latest stock prices are retrieved via an api call and buy orders at a price below the latest market price will be rejected, and the inverse for sell orders. 

This is was a short term project and various improvements have not been made in the interest of time.

Dependencies:
- Java 8
- Maven

To compile and run the project:
In terminal cd into the fixme-parent folder and run: 
mvn clean package

Open 3 different terminals (cd into fixme-parent folder) and in each run:
java -jar fixme-router/target/fixme-router-1.0-SNAPSHOT.jar
java -jar fixme-market/target/fixme-market-1.0-SNAPSHOT.jar
java -jar fixme-broker/target/fixme-broker-1.0-SNAPSHOT.jar

