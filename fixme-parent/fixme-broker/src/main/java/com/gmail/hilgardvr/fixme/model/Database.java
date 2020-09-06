package com.gmail.hilgardvr.fixme.model;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import org.bson.Document;

import com.mongodb.client.MongoCursor;

public class Database {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    
    public Database() {
        try {
            MongoClientURI connectionString = new MongoClientURI("mongodb+srv://hilgardvr:mMottie%40007@cluster0-qubgx.mongodb.net/test?retryWrites=true"); 
            mongoClient = new MongoClient(connectionString);
            /* mongoClient = new MongoClient(); */
            database = mongoClient.getDatabase("mydb");
            collection = database.getCollection("fixme");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void createDocument(FixMessage fix) {
        System.out.println("Saving into db: " + fix.toString());
        Document doc = new Document("OrderType", fix.orderType)
        .append("Instrument", fix.instrument)
        .append("Quantity", fix.quantity)
        .append("Price", fix.price)
        .append("Time", System.currentTimeMillis());

        try {
            collection.insertOne(doc);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void getDb() {
        if (collection != null) {
            System.out.println("Previously save db content:");
            MongoCursor<Document> cursor = collection.find().iterator();
            try {
                while (cursor.hasNext()) {
                    System.out.println(cursor.next().toJson());
                }
            } finally {
                cursor.close();
            }
        } else {
            System.out.println("DB is empty/doesn't exist");
        }
    }
}