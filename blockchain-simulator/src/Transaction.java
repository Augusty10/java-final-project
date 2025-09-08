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