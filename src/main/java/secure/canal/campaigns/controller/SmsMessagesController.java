package secure.canal.campaigns.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import secure.canal.campaigns.payload.HttpResponse;
import secure.canal.campaigns.payload.SmsMessagesDto;
import secure.canal.campaigns.payload.UserDto;
import secure.canal.campaigns.service.AuthService;
import secure.canal.campaigns.service.CampaignsService;
import secure.canal.campaigns.service.SmsMessagesService;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static java.time.LocalTime.now;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping(path = "/secureapi/message")
@RequiredArgsConstructor
public class SmsMessagesController {

    private final SmsMessagesService smsMessagesService;
    private final AuthService authService;
    private final CampaignsService campaignsService;

    @PostMapping("/{campaign_id}/{id_user}/create")
    public ResponseEntity<HttpResponse> importMessage(
            @AuthenticationPrincipal UserDto user,
            @RequestParam("file") MultipartFile importFile,
            @PathVariable(name = "campaign_id") Long campaign_id,
            @PathVariable(name = "id_user") Long id_user,
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size
    ) {

        smsMessagesService.importCampaignFiles(importFile, campaign_id, id_user);
        Page<SmsMessagesDto> messages = smsMessagesService.getMessages(page.orElse(0), size.orElse(10));
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of(
                                "user", authService.getUserByEmail(user.getEmail()),
                                "campaign", campaignsService.getCampaign(campaign_id),
                                "page", messages
                        ))
                        .message("Data Imported Successfully")
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build()
        );
    }


    @GetMapping("/{campaign_id}/{id_user}/new")
    public ResponseEntity<HttpResponse> newMessage(@AuthenticationPrincipal UserDto user,
                                                   @PathVariable(name = "campaign_id") Long campaign_id,
                                                   @PathVariable(name = "id_user") Long id_user,
                                                   @RequestParam Optional<Integer> page,
                                                   @RequestParam Optional<Integer> size
    ) {
        Page<SmsMessagesDto> messages = smsMessagesService.getMessages(page.orElse(0), size.orElse(10)); // Default to first 10 messages
        return ResponseEntity.created(URI.create(""))
                .body(
                        HttpResponse.builder()
                                .timeStamp(now().toString())
                                .data(Map.of(
                                        "user", authService.getUserByEmail(user.getEmail()),
                                        "campaign", campaignsService.getCampaign(campaign_id),
                                        "page", messages
                                ))
                                .message("Message retreived Successfully")
                                .status(OK)
                                .statusCode(CREATED.value())
                                .build());
    }

    @GetMapping("/home")
    public ResponseEntity<HttpResponse> getBalanceSms(@AuthenticationPrincipal UserDto user) {

        return ResponseEntity.created(URI.create(""))
                .body(
                        HttpResponse.builder()
                                .timeStamp(now().toString())
                                .data(Map.of("user", authService.getUserByEmail(user.getEmail()),
                                        "campaigns", campaignsService.getAllCampaigns(),
                                        "stats", smsMessagesService.getStats()
                                        ))
                                .message("Home retreived")
                                .status(OK)
                                .statusCode(CREATED.value())
                                .build());
    }
}
