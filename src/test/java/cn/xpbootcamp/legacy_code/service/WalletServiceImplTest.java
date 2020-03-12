package cn.xpbootcamp.legacy_code.service;

import cn.xpbootcamp.legacy_code.entity.User;
import cn.xpbootcamp.legacy_code.repository.UserRepository;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WalletServiceImplTest {

    @Test
    public void should_return_id_when_buyer_have_enough_money() {
        User buyer = new User();
        User seller = new User();
        buyer.setBalance(10);
        seller.setBalance(1);

        UserRepository userRepository = mock(UserRepository.class);
        WalletServiceImpl walletService = new WalletServiceImpl();

        when(userRepository.find(1)).thenReturn(buyer);
        when(userRepository.find(2)).thenReturn(seller);
        walletService.setUserRepository(userRepository);
        String walletId = walletService.moveMoney("123", 1, 2, 10);

        assertEquals(0, buyer.getBalance(), 0.001);
        assertEquals(11, seller.getBalance(), 0.001);
        assertTrue(walletId.endsWith("123"));
    }

    @Test
    public void should_return_null_when_buyer_have_not_enough_money() {
        User buyer = new User();
        buyer.setBalance(9);

        UserRepository userRepository = mock(UserRepository.class);
        WalletServiceImpl walletService = new WalletServiceImpl();

        when(userRepository.find(1)).thenReturn(buyer);
        when(userRepository.find(2)).thenReturn(new User());
        walletService.setUserRepository(userRepository);
        String walletId = walletService.moveMoney("123", 1, 2, 10);

        assertNull(walletId);
    }

}