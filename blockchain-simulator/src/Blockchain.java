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
