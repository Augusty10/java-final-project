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
