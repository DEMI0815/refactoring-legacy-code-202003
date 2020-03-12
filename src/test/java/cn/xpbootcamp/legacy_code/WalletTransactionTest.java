package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.STATUS;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.transaction.InvalidTransactionException;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RedisDistributedLock.class, IdGenerator.class})
public class WalletTransactionTest {

    private WalletTransaction walletTransaction;

    @Test
    public void should_return_preAssignedId_when_not_null() {
        walletTransaction = new WalletTransaction("t_preAssignedId", null, null, null);
        assertEquals(walletTransaction.getId(), "t_preAssignedId");
    }

    @Test
    public void should_return_transactionId_when_preAssignedId_is_null() {
        mockStatic(IdGenerator.class);
        when(IdGenerator.generateTransactionId()).thenReturn("t_uuid");
        walletTransaction = new WalletTransaction(null, null, null, null);

        assertEquals(walletTransaction.getId(), "t_uuid");
    }

    @Test
    public void should_return_transactionId_when_preAssignedId_is_null_and_not_star_t() {
        mockStatic(IdGenerator.class);
        when(IdGenerator.generateTransactionId()).thenReturn("uuid");
        walletTransaction = new WalletTransaction(null, null, null, null);

        assertEquals(walletTransaction.getId(), "t_null");
    }

    @Test
    public void should_throw_invalid_transaction_exception_when_execute_with_null_buyer_id() {
        walletTransaction = new WalletTransaction("t_preAssignedId", null, 123L, null);

        assertThatExceptionOfType(InvalidTransactionException.class)
                .isThrownBy(walletTransaction::execute)
                .withMessage("This is an invalid transaction");
    }

    @Test
    public void should_throw_invalid_transaction_exception_when_execute_with_null_sell_id() {
        walletTransaction = new WalletTransaction("t_preAssignedId", 123L, null, null);

        assertThatExceptionOfType(InvalidTransactionException.class)
                .isThrownBy(walletTransaction::execute)
                .withMessage("This is an invalid transaction");
    }

    @Test
    public void should_throw_invalid_transaction_exception_when_execute_with_amount_less_than_0() {
        walletTransaction = new WalletTransaction("t_preAssignedId", 123L, 456L, -1D);

        assertThatExceptionOfType(InvalidTransactionException.class)
                .isThrownBy(walletTransaction::execute)
                .withMessage("This is an invalid transaction");
    }

    @Test
    public void should_return_true_when_status_is_EXECUTED() throws InvalidTransactionException {
        walletTransaction = spy(new WalletTransaction("", 123L, 456L, 1.0));
        when(walletTransaction.isExecuted()).thenReturn(true);

        assertTrue(walletTransaction.execute());
    }

    @Test
    public void should_return_false_when_not_lock() throws InvalidTransactionException {
        walletTransaction = spy(new WalletTransaction("t_preAssignedId", 123L, 456L, 1.0));
        mockStatic(RedisDistributedLock.class);
        given(RedisDistributedLock.getSingletonInstance()).willReturn(mock(RedisDistributedLock.class));
        given(RedisDistributedLock.getSingletonInstance().lock("t_preAssignedId")).willReturn(false);

        assertFalse(walletTransaction.execute());
    }

    @Test
    public void should_return_false_when_created_time_over_20() throws InvalidTransactionException, NoSuchFieldException, IllegalAccessException {
        walletTransaction = spy(new WalletTransaction("t_preAssignedId", 123L, 456L, 1.0));
        mockStatic(RedisDistributedLock.class);
        when(RedisDistributedLock.getSingletonInstance()).thenReturn(mock(RedisDistributedLock.class));
        when(RedisDistributedLock.getSingletonInstance().lock("t_preAssignedId")).thenReturn((true));
        setCreatedTimeStampTo20DaysAgo(walletTransaction);

        assertFalse(walletTransaction.execute());
        assertEquals(walletTransaction.getStatus(), STATUS.EXPIRED);
    }

    @Test
    public void should_return_true_when_move_money_success() throws InvalidTransactionException {
        String preAssignedId = "t_preAssignedId";
        walletTransaction = new WalletTransaction(preAssignedId, 123L, 123L, 2.0);
        mockStatic(RedisDistributedLock.class);
        when(RedisDistributedLock.getSingletonInstance()).thenReturn(mock(RedisDistributedLock.class));
        when(RedisDistributedLock.getSingletonInstance().lock(preAssignedId)).thenReturn((true));

        WalletService walletService = mock(WalletService.class);
        when(walletService.moveMoney(preAssignedId, 123L, 123L, 2.0)).thenReturn("walletTransactionId");
        walletTransaction.setWalletService(walletService);

        assertTrue(walletTransaction.execute());
        assertEquals(walletTransaction.getStatus(), STATUS.EXECUTED);
    }

    @Test
    public void should_return_false_when_move_money_failed() throws InvalidTransactionException {
        String preAssignedId = "t_preAssignedId";
        walletTransaction = new WalletTransaction(preAssignedId, 123L, 123L, 2.0);
        mockStatic(RedisDistributedLock.class);
        when(RedisDistributedLock.getSingletonInstance()).thenReturn(mock(RedisDistributedLock.class));
        when(RedisDistributedLock.getSingletonInstance().lock(preAssignedId)).thenReturn((true));

        WalletService walletService = mock(WalletService.class);
        when(walletService.moveMoney(preAssignedId, 123L, 123L, 2.0)).thenReturn(null);
        walletTransaction.setWalletService(walletService);

        assertFalse(walletTransaction.execute());
        assertEquals(walletTransaction.getStatus(), STATUS.FAILED);
    }

    private void setCreatedTimeStampTo20DaysAgo(WalletTransaction walletTransaction)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = WalletTransaction.class.getDeclaredField("createdTimestamp");
        field.setAccessible(true);
        field.set(walletTransaction, 0L);
    }

}