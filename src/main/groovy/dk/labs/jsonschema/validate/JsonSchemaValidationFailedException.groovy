package dk.labs.jsonschema.validate

class JsonSchemaValidationFailedException extends RuntimeException {

    def errors

    JsonSchemaValidationFailedException(List<String> errors) {
        this.errors = errors
    }
}
