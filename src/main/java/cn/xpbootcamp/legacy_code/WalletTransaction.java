package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.STATUS;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;

import javax.transaction.InvalidTransactionException;

public class WalletTransaction {
    public static final String ID_PRE = "t_";
    public static final int TWENTY_DAYS = 20 * 24 * 60 * 60 * 1000;
    private String id;
    private Long buyerId;
    private Long sellerId;
    private Long createdTimestamp;
    private Double amount;
    private STATUS status;

    private WalletService walletService;

    public WalletTransaction(String preAssignedId, Long buyerId, Long sellerId, Double amount) {
        getWalletTransactionId(preAssignedId);
        this.amount = amount;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.status = STATUS.TO_BE_EXECUTED;
        this.createdTimestamp = System.currentTimeMillis();
    }

    private void getWalletTransactionId(String preAssignedId) {
        if (hasPreAssignedId(preAssignedId)) {
            this.id = preAssignedId;
        } else {
            this.id = IdGenerator.generateTransactionId();
        }
        if (!this.id.startsWith(ID_PRE)) {
            this.id = ID_PRE + preAssignedId;
        }
    }

    private boolean hasPreAssignedId(String preAssignedId) {
        return preAssignedId != null && !preAssignedId.isEmpty();
    }

    public boolean execute() throws InvalidTransactionException {
        if (isTransactionExceptional()) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }
        if (isExecuted()) {
            return true;
        }
        boolean isLocked = false;
        try {
            isLocked = RedisDistributedLock.getSingletonInstance().lock(id);

            if (!isLocked) {
                return false;
            }
            if (isExecuted()) {
                return true;
            }
            if (isTimeout()) {
                this.status = STATUS.EXPIRED;
                return false;
            }
            if (isMoveMoneySuccess()) {
                this.status = STATUS.EXECUTED;
                return true;
            } else {
                this.status = STATUS.FAILED;
                return false;
            }
        } finally {
            if (isLocked) {
                RedisDistributedLock.getSingletonInstance().unlock(id);
            }
        }
    }

    private boolean isMoveMoneySuccess() {
        String walletTransactionId = walletService.moveMoney(id, buyerId, sellerId, amount);
        return walletTransactionId != null;
    }

    private boolean isTimeout() {
        long executionInvokedTimestamp = System.currentTimeMillis();
        return executionInvokedTimestamp - createdTimestamp > TWENTY_DAYS;
    }

    private boolean isTransactionExceptional() {
        return buyerId == null || (sellerId == null || amount < 0.0);
    }

    public void setWalletService(WalletService walletService) {
        this.walletService = walletService;
    }

    public boolean isExecuted() {
        return status == STATUS.EXECUTED;
    }

    public String getId() {
        return id;
    }

    public STATUS getStatus() {
        return status;
    }
}