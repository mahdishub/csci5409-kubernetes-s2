package com.example.service2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

@RestController
@Slf4j
public class MainController {

    @Value("${data.directory}")
    private String directory;

    private static final String INPUT_FILE_NOT_IN_CSV_FORMAT = "Input file not in CSV format.";

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorOutput> handleBadRequestException(BadRequestException e) {
        log.info("Error occurred --> {}" , e.getErrorOutput().getError());
        return ResponseEntity.badRequest()
                .body(e.getErrorOutput());
    }

    @PostMapping("/processFile")
    private ResponseEntity<Output> processFile(@RequestBody Input input) {
        validateCSVFormat(input.getFile());

        return ResponseEntity.ok(
                new Output(input.getFile(), calculateSum(input)));
    }

    private Integer calculateSum(Input input) {
        final var fileName = input.getFile();
        final var filePath = Paths.get(directory, fileName);
        try {
            final var content = Files.readString(filePath);
            final var lines = content.split("\n");

            var sum = 0;

            for (int i = 1; i < lines.length; i++) {
                final var entries = lines[i].split(",");

                if (Objects.equals(entries[0], input.getProduct())) {
                    final var amountValue = Integer.parseInt(entries[1].trim());
                    sum += amountValue;
                }
            }

            return sum;

        } catch (IOException e) {
            throw new RuntimeException();
        }
    }


    private void validateCSVFormat(String fileName) {
        final var filePath = Paths.get(directory, fileName);
        try {
            final var content = Files.readString(filePath);
            final var lines = content.split("\n");

            if (lines.length == 0) {
                throw new IOException();
            }

            final var headers = lines[0].split(",");

            if (headers.length != 2
                    || !Objects.equals(headers[0].trim(), "product")
                    || !Objects.equals(headers[1].trim(), "amount")) {

                throw new IOException();
            }

            for (int i = 1; i < lines.length; i++) {
                final var entries = lines[i].split(",");
                if (entries.length != 2) {
                    throw new IOException();
                }

                try {
                    Integer.parseInt(entries[1].trim());
                } catch (NumberFormatException e) {
                    throw new IOException();
                }
            }


        } catch (IOException e) {
            throw new BadRequestException(new ErrorOutput(fileName, INPUT_FILE_NOT_IN_CSV_FORMAT));
        }
    }

}
