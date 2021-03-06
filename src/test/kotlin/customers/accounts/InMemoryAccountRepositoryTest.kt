package customers.accounts

import Customers
import Currencies.GBP
import Currencies.USD
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class InMemoryAccountRepositoryTest {

    private lateinit var repository: AccountRepository
    private val customer = Customers.nikolay()

    @Before
    fun beforeEachTest() {
        repository = InMemoryAccountRepository()
    }

    @Test
    fun `adding an account for the first time should succeed`() {
        val account = Account(0, customer, USD)

        assertTrue(repository.addAccount(account))
        assertEquals(account, repository.getAccount(0))
    }

    @Test
    fun `adding another account with the same currency should succeed`() {
        val usdAccount = Account(0, customer, USD)
        val usdAccountCopy = usdAccount.copy(id = 1)

        assertTrue(repository.addAccount(usdAccount))
        assertTrue(repository.addAccount(usdAccountCopy))
    }

    @Test
    fun `adding multiple accounts with different currencies should succeed`() {
        val usdAccount = Account(0, customer, USD)
        val gbpAccount = Account(1, customer, GBP)

        assertTrue(repository.addAccount(usdAccount))
        assertTrue(repository.addAccount(gbpAccount))
        assertEquals(setOf(usdAccount, gbpAccount), repository.getAccounts(customer))
    }

    @Test
    fun `getAccount(accountId) should return null when no such account exists`() {
        assertNull(repository.getAccount(1000))
    }

    @Test
    fun `getAccounts() should be empty when no accounts created`() {
        assertTrue(repository.getAccounts(customer).isEmpty())
    }

    @Test
    fun `deleteAll() implies getAccounts() is empty`(){
        val vlad = Customers.vlad()
        assertTrue(repository.addAccount(Account(0, vlad, USD)))
        assertEquals(1, repository.getAccounts(vlad).size)
        repository.deleteAll()
        assertTrue(repository.getAccounts(vlad).isEmpty())
    }
}