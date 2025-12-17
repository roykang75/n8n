package xyz.oiio.n8n.push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PushController {

    private final PushService pushService;

    // @GetMapping("/rest/push")
    // public SseEmitter push(@RequestParam String pushRef) {
    // log.info("New push connection requested: {}", pushRef);
    // return pushService.createEmitter(pushRef);
    // }
}
