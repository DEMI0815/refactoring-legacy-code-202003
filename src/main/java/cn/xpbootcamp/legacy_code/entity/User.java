package cn.xpbootcamp.legacy_code.entity;

public class User {
    private long id;
    private double balance;

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void increase(double amount) {
        balance += amount;
    }

    public void decrease(double amount) {
        balance -= amount;
    }
}
