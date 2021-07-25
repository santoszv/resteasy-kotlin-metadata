package test

import jakarta.ws.rs.*

@Consumes("application/json")
@Produces("application/json")
@Path("test")
class TestResource {

    @GET
    fun getEmptyBodyUnitResult() {
        throw RuntimeException("Not Implemented")
    }

    @POST
    fun postEmptyBodyUnitResult() {
        throw RuntimeException("Not Implemented")
    }

    @GET
    fun getEmptyBodyNullableListResult(): List<Any> {
        throw RuntimeException("Not Implemented")
    }

    @POST
    fun postEmptyBodyNotNullListResult1(): List<Any> {
        throw RuntimeException("Not Implemented")
    }

    @POST
    fun postEmptyBodyNotNullListResult2(): List<Any> {
        throw RuntimeException("Not Implemented")
    }
}