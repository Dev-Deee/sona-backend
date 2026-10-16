package io.trysona.sona.config;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Short, memorable link to the API docs: /api/docs → Swagger UI.
 */
@Hidden
@Controller
public class DocsRedirectController {

    @GetMapping("/api/docs")
    public String docs() {
        return "redirect:/swagger-ui/index.html";
    }
}
