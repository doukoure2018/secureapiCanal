package secure.canal.campaigns.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import secure.canal.campaigns.form.UpdateCampaingForm;
import secure.canal.campaigns.form.UpdateMessageForm;
import secure.canal.campaigns.payload.CampaignsDto;
import secure.canal.campaigns.payload.HttpResponse;
import secure.canal.campaigns.payload.UserDto;
import secure.canal.campaigns.service.AuthService;
import secure.canal.campaigns.service.CampaignsService;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.time.LocalTime.now;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping(path = "/secureapi/campaign")
@RequiredArgsConstructor
public class CampaignsController {

    private final CampaignsService campaignsService;
    private final AuthService authService;


    @GetMapping("/new")
    public ResponseEntity<HttpResponse> newCampaign(@AuthenticationPrincipal UserDto user) {

        return ResponseEntity.created(URI.create(""))
                .body(
                        HttpResponse.builder()
                                .timeStamp(now().toString())
                                .data(Map.of("user", authService.getUserByEmail(user.getEmail()),
                                            "balanceSms", campaignsService.getBalanceSms()))
                                .message("Nouvelle Campaigne")
                                .status(CREATED)
                                .statusCode(CREATED.value())
                                .build());
    }

    @PostMapping("/create")
    public ResponseEntity<HttpResponse> createCampaign(@AuthenticationPrincipal UserDto user, @RequestBody CampaignsDto campaignsDto) throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        return ResponseEntity.created(URI.create(""))
                .body(
                        HttpResponse.builder()
                                .timeStamp(now().toString())
                                .data(Map.of("user", authService.getUserByEmail(user.getEmail()),
                                        "Campaign", campaignsService.create(campaignsDto)))
                                .message("Campaign created")
                                .status(CREATED)
                                .statusCode(CREATED.value())
                                .build());
    }

    @PatchMapping("/{idCampaign}/updateCampaignName")
    public ResponseEntity<HttpResponse> createCampaign(@AuthenticationPrincipal UserDto user,
                                                       @PathVariable(name ="idCampaign") Long idCampaign,
                                                       @RequestBody UpdateMessageForm updateMessageForm) throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        return ResponseEntity.created(URI.create(""))
                .body(
                        HttpResponse.builder()
                                .timeStamp(now().toString())
                                .data(Map.of("user", authService.getUserByEmail(user.getEmail()),
                                        "campaign", campaignsService.updateCampaignMessageById(idCampaign,updateMessageForm)))
                                .message("Campaign Updated Successfully")
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
                                .message("Campaigns retreived")
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
                                .message("Campaign retreived")
                                .status(CREATED)
                                .statusCode(CREATED.value())
                                .build());
    }


    @PutMapping("/{idCampaign}/update")
    public ResponseEntity<HttpResponse> updateCampaign(@AuthenticationPrincipal UserDto user,
                                                       @RequestBody UpdateCampaingForm updateCampaingForm,
                                                       @PathVariable(name = "idCampaign") Long idCampaign
    ) throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        return ResponseEntity.created(URI.create(""))
                .body(
                        HttpResponse.builder()
                                .timeStamp(now().toString())
                                .data(Map.of("user", authService.getUserByEmail(user.getEmail()),
                                        "Campaign", campaignsService.updateCampaignById(idCampaign,updateCampaingForm)))
                                .message("Campaign created")
                                .status(CREATED)
                                .statusCode(CREATED.value())
                                .build());
    }
}
