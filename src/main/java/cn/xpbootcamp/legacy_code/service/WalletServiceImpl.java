package cn.xpbootcamp.legacy_code.service;

import cn.xpbootcamp.legacy_code.entity.User;
import cn.xpbootcamp.legacy_code.repository.UserRepository;
import cn.xpbootcamp.legacy_code.repository.UserRepositoryImpl;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;

public class WalletServiceImpl implements WalletService {
    private UserRepository userRepository = new UserRepositoryImpl();

    public String moveMoney(String id, long buyerId, long sellerId, double amount) {
        User buyer = userRepository.find(buyerId);
        if (buyer.getBalance() >= amount) {
            User seller = userRepository.find(sellerId);
            seller.increase(amount);
            buyer.decrease(amount);
            return IdGenerator.generateTransactionId() + id;
        } else {
            return null;
        }
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

}
