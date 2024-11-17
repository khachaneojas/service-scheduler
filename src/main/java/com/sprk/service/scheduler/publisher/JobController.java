package com.sprk.service.scheduler.publisher;

import com.sprk.commons.dto.APIResponse;
import com.sprk.service.scheduler.dto.payload.JobRequest;
import com.sprk.service.scheduler.service.SchedulerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping(path = "/api/scheduler")
@RequiredArgsConstructor
public class JobController {

    private final SchedulerService schedulerService;

    /**
     * Add Job in Database.
     * This method adds a job to the database based on the provided request data.
     * It validates the request parameters, extracts necessary information, and saves the job in the database.
     * @param jobRequest The request object containing the details of the job to be added.
     * @return ResponseEntity containing the APIResponse indicating the success or failure of the job addition.
     */
    @PostMapping("/template/dkK9uJtB4rb4C8Z0KCTEqfDPhcHt2zdWJ52X9iE399g0q5kyX2")
    @Operation(
            summary = "Add Job in Database",
            description = "Adds a job to the database based on the provided request data.\n\n" +
                    "Access Control:\n" +
                    "This endpoint is accessible to all users with appropriate permissions.\n\n" +
                    "Endpoint Workflow:\n" +
                    "1. Validates the provided job request parameters.\n" +
                    "2. Calls the service method to add the job to the database.\n" +
                    "3. Returns a ResponseEntity with the APIResponse containing the status of the job addition.\n",
            tags = {"POST"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Job added successfully.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))
            )
    })
    public ResponseEntity<?> addJobInDB(
            @Parameter(
                    name = "jobRequest",
                    description = "The request object containing the details of the job to be added.",
                    required = true
            )
            @RequestBody JobRequest jobRequest
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(schedulerService.addJobInDB(jobRequest));

    }

}
