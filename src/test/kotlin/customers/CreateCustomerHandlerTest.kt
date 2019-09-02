package customers

import RevolutConfig
import UnirestTestConfig
import customers.CreateCustomer.RequestBody
import customers.CreateCustomer.ResponseBody
import io.javalin.Javalin
import kong.unirest.Unirest
import org.eclipse.jetty.http.HttpStatus.BAD_REQUEST_400
import org.eclipse.jetty.http.HttpStatus.OK_200
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.slf4j.LoggerFactory

class CreateCustomerHandlerTest {

    companion object {

        private const val URL = "http://localhost:7000/customers"

        private lateinit var javalin: Javalin

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            LoggerFactory.getLogger(this.javaClass).debug("HEre")
            val revolutConfig = RevolutConfig()
            LoggerFactory.getLogger(this.javaClass).debug("HEre2")
            UnirestTestConfig.init(revolutConfig.objectMapper)
            val customerRepository = InMemoryCustomerRepository()
            val customerCreator = CustomerCreatorImpl(customerRepository)
            val handler = CreateCustomerHandler(customerCreator)
            LoggerFactory.getLogger(this.javaClass).debug("HEre3")
            javalin = revolutConfig.javalin.start()
            LoggerFactory.getLogger(this.javaClass).debug("HEre4")
            handler.attach(javalin)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            LoggerFactory.getLogger(this.javaClass).debug("HEre5")
            UnirestTestConfig.shutdown()
            javalin.stop()
        }
    }

    @Test
    fun `should create correct Customer when called with expected body`() {
        val response = Unirest.post(URL)
            .body(RequestBody(name = "Nikolay", surname = "Storonsky"))
            .asObject(ResponseBody::class.java)

        assertEquals(OK_200, response.status)
        with(response.body.customer) {
            assertEquals("Nikolay", name)
            assertEquals("Storonsky", surname)
        }
    }

    @Test
    fun `should fail when invalid body given`() {
        val response = Unirest.post(URL)
            .body("{}")
            .asString()

        assertEquals(BAD_REQUEST_400, response.status)
    }
}