package dk.labs.jsonschema.validate

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.fge.jsonschema.core.report.LogLevel
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory
import mjson.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.MethodParameter
import org.springframework.core.io.Resource
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter

import java.lang.reflect.Type

/**
 * @author Dmitriy Kopylenko
 */
@RestController
@ControllerAdvice
class SchemaValidatingController extends RequestBodyAdviceAdapter {

    @Value('classpath:basic-schema.json')
    Resource jsonSchemaResource

    @Autowired
    ObjectMapper jacksonMapper

    static String VALIDATE_WITH_MJSON = 'validateWithMJson'

    @Override
    boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        methodParameter.containingClass.simpleName == this.class.simpleName
    }

    @Override
    Object afterBodyRead(Object body, HttpInputMessage inputMessage,
                         MethodParameter parameter,
                         Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

        parameter.getMethod().name == VALIDATE_WITH_MJSON ?
                validateWithMjsonLibrary(body) :
                validateWithJsonSchemaValidatorLibrary(body)

        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType)
    }

    @PostMapping("/jsonvalidate1")
    ResponseEntity<?> validateWithMJson(@RequestBody Map<String, Object> incomingJsonMap) {
        return ResponseEntity.ok().build()
    }

    @PostMapping("/jsonvalidate2")
    ResponseEntity<?> validateWithJsonSchemaValidator(@RequestBody Map<String, Object> incomingJsonMap) {
        return ResponseEntity.ok().build()
    }

    @ExceptionHandler(JsonSchemaValidationFailedException)
    final ResponseEntity<?> handleUserNotFoundException(JsonSchemaValidationFailedException ex, WebRequest request) {
        new ResponseEntity<>([errors: ex.errors], HttpStatus.BAD_REQUEST)
    }

    private validateWithMjsonLibrary(requestBody) {
        Json incomingJson = Json.make(requestBody)
        Json.Schema schema = Json.schema(this.jsonSchemaResource.getURI())
        def validationResult = schema.validate(incomingJson)
        if (!validationResult.at('ok')) {
            throw new JsonSchemaValidationFailedException(validationResult.at('errors').asList())
        }
    }

    private validateWithJsonSchemaValidatorLibrary(requestBody) {
        JsonNode incomingJson = jacksonMapper.valueToTree(requestBody)
        JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema(this.jsonSchemaResource.getURI().toString())
        ProcessingReport validationReport = schema.validate(incomingJson)
        if(!validationReport.success) {
            def errorMsgs = validationReport.findAll {it.logLevel == LogLevel.ERROR}.collect {it.message}
            throw new JsonSchemaValidationFailedException(errorMsgs)
        }
    }

}
