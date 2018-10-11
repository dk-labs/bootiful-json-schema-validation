package dk.labs.jsonschema.validate

class JsonSchemaValidationFailedException extends RuntimeException {

    JsonSchemaValidationFailedException(String msg) {
        super(msg)
    }
}
