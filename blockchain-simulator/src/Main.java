
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;


public class Main {
public static void main(String[] args) {
// difficulty 4 -> hash must start with 4 zeros (increase for stronger PoW)
Blockchain myCoin = new Blockchain(4, 50.0);


System.out.println("Creating transactions...");
myCoin.addTransaction(new Transaction("address1", "address2", 100));
myCoin.addTransaction(new Transaction("address2", "address1", 30));


System.out.println("Starting the miner (miner1)...");
myCoin.minePendingTransactions("miner1");


System.out.println("Balance of miner1: " + myCoin.getBalanceOfAddress("miner1"));


System.out.println("Creating more transactions...");
myCoin.addTransaction(new Transaction("address1", "address3", 200));


System.out.println("Starting the miner (miner2)...");
myCoin.minePendingTransactions("miner2");


System.out.println("Balance of miner2: " + myCoin.getBalanceOfAddress("miner2"));


// Export chain to JSON
Gson gson = new GsonBuilder().setPrettyPrinting().create();
String chainJson = gson.toJson(myCoin.getChain());
System.out.println("\n=== Blockchain JSON ===\n");
System.out.println(chainJson);


try (FileWriter writer = new FileWriter("chain.json")) {
writer.write(chainJson);
System.out.println("Saved chain.json to working directory.");
} catch (Exception e) {
System.err.println("Failed to write chain.json: " + e.getMessage());
}


// Verify integrity
System.out.println("Is chain valid? " + myCoin.isChainValid());
}
}