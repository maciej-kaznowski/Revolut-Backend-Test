package transfer

import Currencies.GBP
import Currencies.USD
import GBP
import USD
import customers.accounts.*
import customers.accounts.transactions.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import transfer.TransferResult.*

class MoneyTransfererImplTest {

    private lateinit var moneyTransferer: MoneyTransferer
    private lateinit var accountRepository: AccountRepository
    private lateinit var transactionCreator: TransactionCreator
    private lateinit var accountStateQuerier: AccountStateQuerier
    private lateinit var transactionRepository: TransactionRepository

    @Before
    fun beforeEachTest() {
        accountRepository = InMemoryAccountRepository()
        transactionRepository = InMemoryTransactionRepository()
        accountStateQuerier = AccountStateQuerierImpl(transactionRepository)
        transactionCreator = TransactionCreatorImpl(transactionRepository)
        moneyTransferer = MoneyTransfererImpl(transactionCreator, accountStateQuerier)
    }

    @Test
    fun `transfering money from account with incorrect currency should fail`() {
        val vlad = Customers.vlad(0)
        val vladsAccount = Account(0, vlad, GBP) //gbp
        val nikolay = Customers.nikolay(1)
        val nikolaysAccount = Account(1, nikolay, USD) //usd

        val result = moneyTransferer.transfer(10.USD, from = vladsAccount, to = nikolaysAccount)
        assertTrue(result is CurrencyMismatch)
    }

    @Test
    fun `transfering money to account with incorrect currency should fail`(){
        val vlad = Customers.vlad(0)
        val vladsAccount = Account(0, vlad, GBP) //gbp
        val nikolay = Customers.nikolay(1)
        val nikolaysAccount = Account(1, nikolay, USD) //usd

        val result = moneyTransferer.transfer(10.GBP, from = vladsAccount, to = nikolaysAccount)
        assertTrue(result is CurrencyMismatch)
    }

    @Test
    fun `transfering zero money should fail when source account has wrong currency`() {
        val vlad = Customers.vlad(0)
        val vladsAccount = Account(0, vlad, USD)
        val nikolay = Customers.nikolay(1)
        val nikolaysAccount = Account(1, nikolay, GBP)

        val result = moneyTransferer.transfer(0.GBP, from = vladsAccount, to = nikolaysAccount)
        assertTrue(result is CurrencyMismatch)
    }


    @Test
    fun `transfering zero money should fail when target account has wrong currency`() {
        val vlad = Customers.vlad(0)
        val vladsAccount = Account(0, vlad, USD)
        val nikolay = Customers.nikolay(1)
        val nikolaysAccount = Account(1, nikolay, GBP)

        val result = moneyTransferer.transfer(0.USD, from = vladsAccount, to = nikolaysAccount)
        assertTrue(result is CurrencyMismatch)
    }

    @Test
    fun `transfering zero money should succeed when accounts have same currency`(){
        val vlad  = Customers.vlad(0)
        val vladsUsdAccount = Account(0, vlad, USD)

        val nikolay = Customers.nikolay(1)
        val nikolaysUsdAccount = Account(1, nikolay, USD)

        val result = moneyTransferer.transfer(0.USD, from = vladsUsdAccount, to = nikolaysUsdAccount)
        assertTrue(result is Success)
    }

    @Test
    fun `transfering between same account shouldn't create a transaction`() {
        val vlad = Customers.vlad()
        val account = Account(0, vlad, USD)

        val result = moneyTransferer.transfer(10.USD, from = account, to = account)
        assertEquals(SameAccount, result)

        //check no transactions - we could have also used Mockito's `verifyNoMoreInteractions` were the object mocked
        assertTrue(transactionRepository.getAll(account).isEmpty())
    }

    @Test
    fun `transfering too much money should fail`() {
        //vlad has $1000
        val vlad = Customers.vlad(0)
        val vladsUsdAccount = Account(0, vlad, USD)
        transactionRepository.add(Transaction(id = -1, account = vladsUsdAccount, money = 1000.USD))

        //nikolay has a USD account to accept the transfer
        val nikolay = Customers.nikolay(1)
        val nikolaysUsdAccount = Account(1, nikolay, USD)

        //transfer $1001 from vlad to nikolay
        val result = moneyTransferer.transfer(1001.USD, from = vladsUsdAccount, to = nikolaysUsdAccount)
        assertTrue(result is InsufficientFunds)
    }

    @Test
    fun `transferring entire accounts funds should succeed`() {
        //vlad has $1000
        val vlad = Customers.vlad(0)
        val vladsUsdAccount = Account(0, vlad, USD)
        transactionRepository.add(Transaction(id = -1, account = vladsUsdAccount, money = 1000.USD))

        //nikolay has a USD account to accept the transfer
        val nikolay = Customers.nikolay(1)
        val nikolaysUsdAccount = Account(1, nikolay, USD)

        //transfer all of vlads USD money to nikolay
        val result = moneyTransferer.transfer(from = vladsUsdAccount, to = nikolaysUsdAccount, money = 1000.USD)
        assertTrue(result is Success)
    }

    @Test
    fun `transferring money should return correct transactions in result`(){
        //vlad has $1000
        val vlad = Customers.vlad(0)
        val vladsUsdAccount = Account(0, vlad, USD)
        transactionRepository.add(Transaction(id = -1, account = vladsUsdAccount, money = 1000.USD))

        //nikolay has $1000
        val nikolay = Customers.nikolay(1)
        val nikolaysUsdAccount = Account(1, nikolay, USD)
        transactionRepository.add(Transaction(id = -2, account = nikolaysUsdAccount, money = 1000.USD))

        //transfer $10 from vlad to nikolay
        val result = moneyTransferer.transfer(from = vladsUsdAccount, to = nikolaysUsdAccount, money = 10.USD)
        assertTrue(result is Success)
        result as Success

        //returned fromTransaction should say we withdrew $10
        val expectedFromTransaction = Transaction(0, 1, vladsUsdAccount, (-10).USD)
        assertEquals(expectedFromTransaction, result.fromTransaction)

        //returned toTransaction should say nikolay received $10
        val expectedToTransaction = Transaction(1, 0, nikolaysUsdAccount, 10.USD)
        assertEquals(expectedToTransaction, result.toTransaction)
    }

    @Test
    fun `transferring money should deduct from senders account`() {
        //vlad has $1000
        val vlad = Customers.vlad(0)
        val vladsUsdAccount = Account(0, vlad, USD)
        transactionRepository.add(Transaction(id = -1, account = vladsUsdAccount, money = 1000.USD))

        //nikolay has a USD account to accept the transfer
        val nikolay = Customers.nikolay(1)
        val nikolaysUsdAccount = Account(1, nikolay, USD)

        //transfer $500 from vlad to nikolay
        val result = moneyTransferer.transfer(500.USD, from = vladsUsdAccount, to = nikolaysUsdAccount)
        assertTrue(result is Success)

        //vlad should now have $500
        val vladsUsdAccountState = accountStateQuerier.getCurrentState(vladsUsdAccount)
        assertEquals(500.USD, vladsUsdAccountState.money)
    }

    @Test
    fun `transferring money should add money to targets account`() {
        //vlad has $1000
        val vlad = Customers.vlad(0)
        val vladsUsdAccount = Account(0, vlad, USD)
        transactionRepository.add(Transaction(id = -1, account = vladsUsdAccount, money = 1000.USD))

        //nikolay has a USD account to accept the transfer
        val nikolay = Customers.nikolay(1)
        val nikolaysUsdAccount = Account(1, nikolay, USD)

        //transfer $500 from vlad to nikolay
        val result = moneyTransferer.transfer(500.USD, from = vladsUsdAccount, to = nikolaysUsdAccount)
        assertTrue(result is Success)

        //nikolay should now have $500
        val nikolaysUsdAccountState = accountStateQuerier.getCurrentState(nikolaysUsdAccount)
        assertEquals(500.USD, nikolaysUsdAccountState.money)
    }
}