package dk.labs.jsonschema.validate


import mjson.Json
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter

import java.lang.reflect.Type

/**
 * @author Dmitriy Kopylenko
 */
@RestController
@ControllerAdvice
@RequestMapping('/validatedjson')
class SchemaValidatingController extends RequestBodyAdviceAdapter {

    @Value('classpath:basic-schema.json')
    Resource jsonSchemaResource

    @Override
    boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        methodParameter.containingClass.simpleName == this.class.simpleName
    }

    @Override
    Object afterBodyRead(Object body, HttpInputMessage inputMessage,
                         MethodParameter parameter,
                         Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {

        Json.Schema schema = Json.schema(jsonSchemaResource.getURI())

        Json incomingJson = Json.make(body)

        def validationResult = schema.validate(incomingJson)
        if(!validationResult.at('ok')) {
            throw new JsonSchemaValidationFailedException(validationResult.at('errors').asList())
        }

        return super.afterBodyRead(body, inputMessage, parameter, targetType, converterType)
    }

    @PostMapping
    ResponseEntity<?> validateJson(@RequestBody Map<String, Object> incomingJsonMap) {
        return ResponseEntity.ok().build()
    }

    @ExceptionHandler(JsonSchemaValidationFailedException)
    final ResponseEntity<?> handleUserNotFoundException(JsonSchemaValidationFailedException ex, WebRequest request) {
        new ResponseEntity<>([errors: ex.errors], HttpStatus.BAD_REQUEST)
    }

}
