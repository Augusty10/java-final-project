/*
Java Blockchain Simulator
Files included below (copy each into its own .java file in the same package/folder):
 - Transaction.java
 - Block.java
 - Blockchain.java
 - Main.java

Dependencies:
 - gson (com.google.code.gson:gson)

How to compile & run (CLI):
 1. Put all .java files in a folder, e.g. blockchain/
 2. Add gson jar to classpath, or if using Maven/Gradle add dependency.
 3. javac -cp gson-2.10.1.jar *.java
 4. java -cp .:gson-2.10.1.jar Main    (on Windows use ; instead of :)

Output:
 - Prints chain JSON to console and writes chain.json in working directory.

Flow diagram (Mermaid-like ASCII):

  [User] --> create Transaction(s)
                |
                v
         addTransaction() -> pendingTransactions
                |
                v
        minePendingTransactions(minerAddress)
                |
        create new Block(pendingTransactions, prevHash)
                |
        Block.mineBlock(difficulty)  -- proof-of-work loops until hash matches
                |
                v
        Blockchain.addBlock(minedBlock) -> chain.add(minedBlock)
                |
                v
        pendingTransactions cleared, reward added to pendingTransactions
                |
                v
        getChainStateAsJSON() -> printed & saved


========================
Transaction.java
========================
*/

import java.io.Serializable;

public class Transaction implements Serializable {
    private String fromAddress;
    private String toAddress;
    private double amount;

    public Transaction(String fromAddress, String toAddress, double amount) {
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.amount = amount;
    }

    public String getFromAddress() { return fromAddress; }
    public String getToAddress() { return toAddress; }
    public double getAmount() { return amount; }

    @Override
    public String toString() {
        return "Transaction{" +
                "from='" + fromAddress + '\'' +
                ", to='" + toAddress + '\'' +
                ", amount=" + amount +
                '}';
    }
}

/*
========================
Block.java
========================
*/

import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

public class Block {
    private int index;
    private long timestamp;
    private List<Transaction> transactions;
    private String previousHash;
    private String hash;
    private long nonce;

    public Block(int index, List<Transaction> transactions, String previousHash) {
        this.index = index;
        this.timestamp = Instant.now().toEpochMilli();
        // Make a defensive copy so later mutation of source list doesn't change the block
        this.transactions = transactions == null ? new ArrayList<>() : new ArrayList<>(transactions);
        this.previousHash = previousHash;
        this.nonce = 0;
        this.hash = calculateHash();
    }

    public String calculateHash() {
        try {
            Gson gson = new Gson();
            String data = index + Long.toString(timestamp) + gson.toJson(transactions) + previousHash + Long.toString(nonce);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void mineBlock(int difficulty) {
        String target = new String(new char[difficulty]).replace('\0', '0');
        while (!hash.substring(0, difficulty).equals(target)) {
            nonce++;
            hash = calculateHash();
        }
        System.out.println("Block mined: " + hash + " (nonce=" + nonce + ")");
    }

    // getters
    public String getHash() { return hash; }
    public String getPreviousHash() { return previousHash; }
    public List<Transaction> getTransactions() { return new ArrayList<>(transactions); }
    public int getIndex() { return index; }
    public long getTimestamp() { return timestamp; }
    public long getNonce() { return nonce; }
}

/*
========================
Blockchain.java
========================
*/

import java.util.ArrayList;
import java.util.List;

public class Blockchain {
    private List<Block> chain;
    private List<Transaction> pendingTransactions;
    private int difficulty;
    private double miningReward;

    public Blockchain(int difficulty, double miningReward) {
        this.difficulty = difficulty;
        this.miningReward = miningReward;
        this.chain = new ArrayList<>();
        this.pendingTransactions = new ArrayList<>();
        // create genesis block
        chain.add(createGenesisBlock());
    }

    private Block createGenesisBlock() {
        return new Block(0, new ArrayList<>(), "0");
    }

    public Block getLatestBlock() {
        return chain.get(chain.size() - 1);
    }

    public void addTransaction(Transaction tx) {
        if (tx.getFromAddress() == null || tx.getToAddress() == null) {
            throw new IllegalArgumentException("Transaction must include from and to address");
        }
        if (tx.getAmount() <= 0) {
            throw new IllegalArgumentException("Transaction amount should be > 0");
        }
        pendingTransactions.add(tx);
    }

    public void minePendingTransactions(String minerAddress) {
        // create block with all pending transactions and mine it
        Block block = new Block(chain.size(), pendingTransactions, getLatestBlock().getHash());
        block.mineBlock(difficulty);
        chain.add(block);
        System.out.println("Block added to chain.");
        // reward the miner by creating a transaction to minerAddress
        pendingTransactions = new ArrayList<>();
        Transaction rewardTx = new Transaction(null, minerAddress, miningReward);
        pendingTransactions.add(rewardTx);
    }

    public boolean isChainValid() {
        for (int i = 1; i < chain.size(); i++) {
            Block current = chain.get(i);
            Block previous = chain.get(i - 1);
            // verify hash integrity
            if (!current.getHash().equals(current.calculateHash())) {
                System.out.println("Invalid: Hash mismatch at block " + i);
                return false;
            }
            if (!current.getPreviousHash().equals(previous.getHash())) {
                System.out.println("Invalid: Previous hash mismatch at block " + i);
                return false;
            }
        }
        return true;
    }

    public List<Block> getChain() {
        return new ArrayList<>(chain);
    }

    public List<Transaction> getPendingTransactions() {
        return new ArrayList<>(pendingTransactions);
    }

    // convenience: compute balance of an address by scanning chain + pending
    public double getBalanceOfAddress(String address) {
        double balance = 0;
        for (Block blk : chain) {
            for (Transaction tx : blk.getTransactions()) {
                if (address.equals(tx.getFromAddress())) {
                    balance -= tx.getAmount();
                }
                if (address.equals(tx.getToAddress())) {
                    balance += tx.getAmount();
                }
            }
        }
        // also include pending transactions (not yet confirmed)
        for (Transaction tx : pendingTransactions) {
            if (address.equals(tx.getFromAddress())) balance -= tx.getAmount();
            if (address.equals(tx.getToAddress())) balance += tx.getAmount();
        }
        return balance;
    }
}

/*
========================
Main.java
========================
*/

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.util.Arrays;

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

/*
Notes & Extensions:
 - This simulator is intentionally simple (educational). For production you'd need digital signatures for transactions, persistent storage, peer-to-peer networking, mempool limits, better reward mechanisms, reorg handling, etc.
 - To add Gson to your project: if using Maven add dependency:

<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>2.10.1</version>
</dependency>

 - If you want a JavaFX GUI, I can add a minimal UI that shows pending transactions, start mining button, and shows chain JSON in a text area.
*/
