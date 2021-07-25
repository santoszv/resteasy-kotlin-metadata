package resteasy.metadat.kotlin

import jakarta.json.Json
import jakarta.json.JsonArray
import jakarta.json.JsonObject
import jakarta.servlet.annotation.WebServlet
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.jboss.resteasy.core.ResourceLocatorInvoker
import org.jboss.resteasy.core.ResourceMethodInvoker
import org.jboss.resteasy.core.ResourceMethodRegistry
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters
import org.jboss.resteasy.spi.ResteasyDeployment
import org.jboss.resteasy.spi.metadata.ResourceLocator
import org.jboss.resteasy.spi.metadata.ResourceMethod
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.kotlinFunction

@WebServlet("/resteasy/KotlinMetadata")
class KotlinMetadata : HttpServlet() {

    private var metadata: JsonArray? = null

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val metadata = synchronized(this) {
            var result = this.metadata
            if (result == null) {
                val resourceLocators = getResourceLocators()
                if (resourceLocators == null) {
                    resp.contentType = "text/plain"
                    resp.writer.println("RestEasy Deployment Was Not Found")
                    return
                }
                result = getMetadata(resourceLocators)
                this.metadata = result
            }
            result
        }
        resp.contentType = "application/json"
        val writer = Json.createWriter(resp.writer)
        writer.writeArray(metadata)
    }

    private fun getResourceLocators(): List<ResourceLocator>? {
        @Suppress("UNCHECKED_CAST") val deployments = servletContext.getAttribute(ResteasyContextParameters.RESTEASY_DEPLOYMENTS) as? Map<String, ResteasyDeployment> ?: return null
        val result: MutableList<ResourceLocator> = ArrayList()
        for (deployment in deployments.values) {
            val registry = deployment.registry as ResourceMethodRegistry
            for (resourceInvokers in registry.bounded.values) {
                for (resourceInvoker in resourceInvokers) {
                    if (resourceInvoker is ResourceLocatorInvoker || resourceInvoker is ResourceMethodInvoker) {
                        val field = resourceInvoker.javaClass.getDeclaredField("method")
                        field.isAccessible = true
                        val resourceMethod = field[resourceInvoker] as ResourceLocator
                        val annotation = resourceMethod.resourceClass.clazz.getDeclaredAnnotation(Metadata::class.java)
                        if (annotation != null) {
                            result.add(resourceMethod)
                        }
                    }
                }
            }
        }
        return result
    }

    private fun getMetadata(resourceLocators: List<ResourceLocator>): JsonArray {
        val result = Json.createArrayBuilder()
        for (resourceLocator in resourceLocators) {
            val metadata = Json.createObjectBuilder()
            val resourceClass = resourceLocator.resourceClass.clazz.kotlin
            val method = resourceLocator.method.kotlinFunction!!
            metadata.add("resourceClass", resourceClass.qualifiedName)
            metadata.add("returnType", getMetadata(method.returnType))
            metadata.add("method", method.name)
            val params = Json.createArrayBuilder()
            for ((index, methodParameter) in resourceLocator.params.withIndex()) {
                val param = Json.createObjectBuilder()
                val kParameters = method.parameters
                val kParameter = kParameters[index + 1] // "this" is at "0"
                param.add("type", getMetadata(kParameter.type))
                param.add("paramType", methodParameter.paramType.name)
                param.add("paramName", methodParameter.paramName)
                params.add(param.build())
            }
            metadata.add("params", params.build())
            val fullPath = resourceLocator.fullpath
            if (fullPath == null) {
                metadata.addNull("fullPath")
            } else {
                metadata.add("fullPath", fullPath)
            }
            val path = resourceLocator.path
            if (path == null) {
                metadata.addNull("path")
            } else {
                metadata.add("path", path)
            }
            val httpMethods = Json.createArrayBuilder()
            val produces = Json.createArrayBuilder()
            val consumes = Json.createArrayBuilder()
            if (resourceLocator is ResourceMethod) {
                metadata.add("resourceMethod", true)
                for (httpMethod in resourceLocator.httpMethods) {
                    httpMethods.add(httpMethod)
                }
                for (produce in resourceLocator.produces) {
                    produces.add(produce.toString())
                }
                for (consume in resourceLocator.consumes) {
                    consumes.add(consume.toString())
                }
            } else {
                metadata.add("resourceMethod", false)
            }
            metadata.add("httpMethods", httpMethods.build())
            metadata.add("produces", produces.build())
            metadata.add("consumes", consumes.build())
            result.add(metadata.build())
        }
        return result.build()
    }

    private fun getMetadata(type: KType?): JsonObject {
        val result = Json.createObjectBuilder()
        val kClass = type?.classifier as? KClass<*>
        result.add("qualifiedName", kClass?.qualifiedName ?: "???")
        result.add("isMarkedNullable", type?.isMarkedNullable ?: true)
        val typeArguments = Json.createArrayBuilder()
        type?.arguments?.forEach { argument ->
            typeArguments.add(getMetadata(argument.type))
        }
        result.add("typeArguments", typeArguments.build())
        return result.build()
    }
}