package secure.canal.campaigns.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import secure.canal.campaigns.payload.CampaignsDto;
import secure.canal.campaigns.payload.HttpResponse;
import secure.canal.campaigns.payload.UserDto;
import secure.canal.campaigns.service.AuthService;
import secure.canal.campaigns.service.CampaignsService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.time.LocalTime.now;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping(path = "/secureapi/campaign")
@RequiredArgsConstructor
public class CampaignsController {

    private final CampaignsService campaignsService;
    private final AuthService authService;




    @PostMapping("/create")
    public ResponseEntity<HttpResponse> createCampaign(@AuthenticationPrincipal UserDto user, @RequestBody CampaignsDto campaignsDto) {

        return ResponseEntity.created(URI.create(""))
                .body(
                        HttpResponse.builder()
                                .timeStamp(now().toString())
                                .data(Map.of("user", authService.getUserByEmail(user.getEmail()),
                                        "Campaign", campaignsService.create(campaignsDto)))
                                .message("Customer created")
                                .status(CREATED)
                                .statusCode(CREATED.value())
                                .build());
    }

    @GetMapping("/campaigns")
    public ResponseEntity<HttpResponse> getAllCompaigns(@AuthenticationPrincipal UserDto user){
        List<CampaignsDto> campaignsDtos = campaignsService.getAllCampaigns();
        return ResponseEntity.created(URI.create(""))
                .body(
                        HttpResponse.builder()
                                .timeStamp(now().toString())
                                .data(Map.of("user", authService.getUserByEmail(user.getEmail()),
                                        "Campaigns", campaignsDtos))
                                .message("Customer created")
                                .status(CREATED)
                                .statusCode(CREATED.value())
                                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HttpResponse> getCampaign(@AuthenticationPrincipal UserDto user,@PathVariable(name = "id") Long id){
           CampaignsDto campaignsDto = campaignsService.getCampaign(id);
        return ResponseEntity.created(URI.create(""))
                .body(
                        HttpResponse.builder()
                                .timeStamp(now().toString())
                                .data(Map.of("user", authService.getUserByEmail(user.getEmail()),
                                        "Campaigns", campaignsDto))
                                .message("Customer created")
                                .status(CREATED)
                                .statusCode(CREATED.value())
                                .build());
    }


}
