package secure.canal.campaigns.controller;


import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import secure.canal.campaigns.entity.SmsMessages;
import secure.canal.campaigns.enumeration.MessageStatus;
import secure.canal.campaigns.payload.HttpResponse;
import secure.canal.campaigns.payload.SmsMessageResponse;
import secure.canal.campaigns.payload.SmsMessagesDto;
import secure.canal.campaigns.payload.UserDto;
import secure.canal.campaigns.reports.SmsMessageReport;
import secure.canal.campaigns.service.AuthService;
import secure.canal.campaigns.service.CampaignsService;
import secure.canal.campaigns.service.SmsMessagesService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.time.LocalTime.now;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.parseMediaType;

@RestController
@RequestMapping(path = "/secureapi/message")
@RequiredArgsConstructor
@Slf4j
public class SmsMessagesController {

    private final SmsMessagesService smsMessagesService;
    private final AuthService authService;
    private final CampaignsService campaignsService;


    @GetMapping("/home")
    public ResponseEntity<HttpResponse> getBalanceSms(@AuthenticationPrincipal UserDto user) {
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of("user", authService.getUserByEmail(user.getEmail()),
                                "campaigns", campaignsService.getAllCampaigns(),
                                "stats", smsMessagesService.getStats()
                        ))
                        .message("Home retreived")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }


    @PostMapping("/{campaign_id}/{id_user}/create")
    public ResponseEntity<HttpResponse> importMessage(
            @AuthenticationPrincipal UserDto user,
            @RequestParam("file") MultipartFile importFile,
            @PathVariable(name = "campaign_id") Long campaign_id,
            @PathVariable(name = "id_user") Long id_user,
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size
    ) throws InterruptedException {

        TimeUnit.SECONDS.sleep(3);
        smsMessagesService.importCampaignFiles(importFile, campaign_id, id_user);
        Page<SmsMessagesDto> messages = smsMessagesService.getMessages(page.orElse(0), size.orElse(10),campaign_id);
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

    @PostMapping("/createUnicastMessage")
    public ResponseEntity<HttpResponse> saveUnitCastMessage(
            @AuthenticationPrincipal UserDto user,
            @RequestBody SmsMessagesDto smsMessagesDto
    ) throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        smsMessagesService.saveUnicastMessage(smsMessagesDto);
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of(
                                "user", authService.getUserByEmail(user.getEmail()),
                                "campaign", campaignsService.getCampaign(smsMessagesDto.getCampaign_id()),
                                "message", smsMessagesService.getAllMessagesByCampaign(smsMessagesDto.getCampaign_id())
                        ))
                        .message("Message retreived Successfully")
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build()
        );
    }

    @GetMapping("/{campaign_id}/{id_user}/createUnicastMessage/new")
    public ResponseEntity<HttpResponse> newUnicastMessage(
            @AuthenticationPrincipal UserDto user,
            @PathVariable(name = "campaign_id") Long campaign_id,
            @PathVariable(name = "id_user") Long id_user
    ) {
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of(
                                "user", authService.getUserByEmail(user.getEmail()),
                                "campaign", campaignsService.getCampaign(campaign_id),
                                "messages", smsMessagesService.getAllMessagesByCampaign(campaign_id)
                        ))
                        .message("Message retreived Successfully")
                        .status(CREATED)
                        .statusCode(CREATED.value())
                        .build()
        );
    }


    // BROADCAST MESSAGE
    @GetMapping("/{campaign_id}/{id_user}/new")
    public ResponseEntity<HttpResponse> newMessage(@AuthenticationPrincipal UserDto user,
                                                   @PathVariable(name = "campaign_id") Long campaign_id,
                                                   @PathVariable(name = "id_user") Long id_user,
                                                   @RequestParam(value = "pageNo", defaultValue = "0", required = false) int pageNo,
                                                   @RequestParam(value = "pageSize", defaultValue = "10", required = false) int pageSize,
                                                   @RequestParam(value = "sortBy", defaultValue = "id", required = false) String sortBy,
                                                   @RequestParam(value = "sortDir", defaultValue = "asc", required = false) String sortDir
    ) {
        SmsMessageResponse smsMessageResponse = smsMessagesService.getAllMessagesByCampaign(campaign_id,pageNo, pageSize, sortBy, sortDir);
        return ResponseEntity.created(URI.create(""))
                .body(
                        HttpResponse.builder()
                                .timeStamp(now().toString())
                                .data(Map.of(
                                        "user", authService.getUserByEmail(user.getEmail()),
                                        "campaign", campaignsService.getCampaign(campaign_id),
                                        "page", smsMessageResponse
                                ))
                                .message("Message retreived Successfully")
                                .status(OK)
                                .statusCode(CREATED.value())
                                .build());
    }

    @GetMapping("/{campaign_id}/{id_user}/add")
    public ResponseEntity<HttpResponse> newMessage2(@AuthenticationPrincipal UserDto user,
                                                   @PathVariable(name = "campaign_id") Long campaign_id,
                                                   @PathVariable(name = "id_user") Long id_user
      ) {
         List<SmsMessagesDto> smsMessagesDtos = smsMessagesService.getAllMessagesByCampaign(campaign_id);
        return ResponseEntity.created(URI.create(""))
                .body(
                        HttpResponse.builder()
                                .timeStamp(now().toString())
                                .data(Map.of(
                                        "user", authService.getUserByEmail(user.getEmail()),
                                        "campaign", campaignsService.getCampaign(campaign_id),
                                        "messages", smsMessagesDtos
                                ))
                                .message("Message retreived Successfully")
                                .status(OK)
                                .statusCode(CREATED.value())
                                .build());
    }

    @GetMapping("/{campaign_id}/{id_user}/search")
    public ResponseEntity<HttpResponse> searchMessages(
            @AuthenticationPrincipal UserDto user,
            @PathVariable(name = "campaign_id") Long campaign_id,
            @PathVariable(name = "id_user") Long id_user,
            @RequestParam(name = "status") String status,
            @RequestParam(value = "pageNo", defaultValue = "0", required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10", required = false) int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc", required = false) String sortDir
    ) {
        SmsMessageResponse smsMessageResponse = smsMessagesService.searchCampaignMessages(status,campaign_id,pageNo, pageSize, sortBy, sortDir);
        return ResponseEntity.ok(
                HttpResponse.builder()
                        .timeStamp(now().toString())
                        .data(Map.of(
                                "user", authService.getUserByEmail(user.getEmail()),
                                "campaign", campaignsService.getCampaign(campaign_id),
                                "page", smsMessageResponse
                        ))
                        .message("Messages retrieved")
                        .status(OK)
                        .statusCode(OK.value())
                        .build());
    }



    @GetMapping("/download/report/{campaignId}")
    public ResponseEntity<InputStreamResource> downloadMessagesByCampaign(@PathVariable(name = "campaignId") Long campaignId) throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        List<SmsMessages> smsMessagesList = new ArrayList<>();
        smsMessagesService.getSmsMessages(campaignId).iterator().forEachRemaining(smsMessagesList::add);
        SmsMessageReport report = new SmsMessageReport(smsMessagesList);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=messages-report.xlsx");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .headers(headers)
                .body(report.export()); // Ensure this returns an InputStreamResource
    }

}
